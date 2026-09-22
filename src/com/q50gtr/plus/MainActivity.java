package com.q50gtr.plus;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import android.util.Log;

import com.q50gtr.plus.data.AirLiftLiveSource;
import com.q50gtr.plus.data.DataHub;
import com.q50gtr.plus.data.DemoDataProvider;
import com.q50gtr.plus.data.EcuTekLiveSource;
import com.q50gtr.plus.data.InTouchVehicleSource;
import com.q50gtr.plus.data.Channel;
import com.q50gtr.plus.data.VehicleProbe;
import com.q50gtr.plus.diag.DiagnosticBundle;
import com.q50gtr.plus.diag.TransportProbe;
import com.q50gtr.plus.diag.DiagnosticReport;
import com.q50gtr.plus.diag.UsbStorage;
import com.q50gtr.plus.ecutek.EcuTekParameters;
import com.q50gtr.plus.ecutek.EcuTekRawLog;
import com.q50gtr.plus.ui.DashboardView;

/**
 * Q50 GTR+ — a single full-screen instrument panel.
 *
 * No fragments, no support library, no layout XML: the activity owns a data hub
 * and one custom View, which is all a factory head unit should ever be asked
 * to run.
 */
public final class MainActivity extends Activity {

    private static final String STATE_PAGE = "page";
    private static final String TAG = "Q50GTR";

    private DataHub hub;
    private DashboardView dashboard;
    private VehicleProbe probe;
    private EcuTekLiveSource ecuTek;
    private com.q50gtr.plus.net.BridgeSource bridge;
    private TransportProbe transport;
    private final EcuTekRawLog rawLog = new EcuTekRawLog();
    private final Handler handler = new Handler();
    /** Раз в столько миллисекунд отчёты уходят на флешку сами. */
    private static final long SAVE_EVERY_MS = 20000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // Всю область, которую ГУ отдаёт приложению, берём целиком: без
        // заголовка и строки состояния. Тема уже полноэкранная, флаг ставится
        // явно, чтобы это не зависело от того, какую тему подставит прошивка.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Сначала зонд: на незнакомой прошивке важно записать факты до того,
        // как что-то подключать. Он только читает.
        probe = new VehicleProbe();
        try {
            probe.run(this);
        } catch (Throwable t) {
            Log.w(TAG, "зонд упал: " + t);
        }

        // Сырой лог обмена с EVI выключен по умолчанию и включается только
        // файлом-маркером в корне флешки: случайно его не задеть.
        try {
            java.io.File usb = UsbStorage.pickWritableDir();
            rawLog.setEnabled(usb != null
                    && new java.io.File(usb, EcuTekRawLog.MARKER).exists());
        } catch (Throwable t) {
            Log.w(TAG, "маркер сырого лога не проверить: " + t);
        }

        ecuTek = new EcuTekLiveSource(this, rawLog);
        bridge = new com.q50gtr.plus.net.BridgeSource();
        hub = new DataHub(new InTouchVehicleSource(this), ecuTek,
                new AirLiftLiveSource(), new DemoDataProvider(), bridge);
        com.q50gtr.plus.ui.DiagOverlay.setVersion(versionName());
        // Разведка транспортов идёт в фоне: она читает десятки файлов и
        // зовёт перечисляющие утилиты, а отрисовку приборов задерживать
        // из-за этого нельзя.
        transport = new TransportProbe(this);
        com.q50gtr.plus.ui.DiagOverlay.setTransport(transport);
        com.q50gtr.plus.ui.DiagOverlay.setBridge(bridge);
        new Thread(new Runnable() {
            public void run() {
                try {
                    transport.run();
                } catch (Throwable t) {
                    Log.w(TAG, "разведка транспортов упала: " + t);
                }
            }
        }, "q50-transport").start();
        dashboard = new DashboardView(this, hub);
        dashboard.setProbe(probe);
        // MATCH_PARENT x MATCH_PARENT: никаких фиксированных размеров, вью
        // получает ровно то окно, которое даёт InTouch.
        dashboard.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        // Экранная стрелка «назад» до сих пор ничего не делала: на ГУ
        // аппаратной кнопки «назад» может не оказаться, и тогда из приложения
        // нечем выйти — а вместе с выходом терялась вся диагностика.
        dashboard.setOnExit(new Runnable() {
            public void run() {
                Log.i(TAG, "выход по экранной стрелке");
                finish();
            }
        });
        // Загрузка драйвера USB-модема. Только по явному нажатию кнопки на
        // странице ТРАНСПОРТ — пользователь разрешил именно такой порядок.
        dashboard.setOnLoadRndis(new Runnable() {
            public void run() {
                Log.i(TAG, "запрошен следующий шаг настройки транспорта");
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            // Кнопка ведёт по шагам: пока драйвера нет —
                            // грузим его, дальше поднимаем интерфейс.
                            if (!transport.isRndisLoaded()) {
                                transport.loadRndis();
                            } else if (!TransportProbe.isIfaceUp("usb0")
                                    || TransportProbe.ifaceAddress("usb0") == null) {
                                transport.configureUsb0();
                            } else if (!bridge.isTxTried()) {
                                // Интерфейс поднят и адресован: заставляем ГУ
                                // отправить пакет самому и смотрим на TX.
                                bridge.txTest();
                            } else if (!TransportProbe.isPhoneAnswering()) {
                                // Передавать умеем, а телефон на ARP молчит.
                                // Дальше спрашиваем адрес у него самого.
                                transport.dhcpUsb0();
                                bridge.txTest();
                            } else {
                                bridge.txTest();
                            }
                        } catch (Throwable t) {
                            Log.w(TAG, "настройка транспорта упала: " + t);
                        }
                        saveDiagnostics();
                    }
                }, "q50-insmod").start();
            }
        });
        setContentView(dashboard);

        if (savedInstanceState != null) {
            dashboard.setPage(savedInstanceState.getInt(STATE_PAGE, 0));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hub.start();
        dashboard.start();
        handler.removeCallbacks(saver);
        handler.postDelayed(saver, SAVE_EVERY_MS);
    }

    /**
     * Отчёты пишутся по таймеру, а не только на выходе.
     *
     * Так уже было в предыдущем проекте на этом же ГУ, и не зря: в машине
     * кнопка «назад» может не сработать вовсе, и тогда onPause() не наступит
     * никогда — а вместе с ним пропадёт всё, ради чего ездили. Запись идёт в
     * отдельном потоке: файл маленький, но тормозить отрисовку приборов
     * из-за флешки нельзя.
     */
    private final Runnable saver = new Runnable() {
        public void run() {
            new Thread(new Runnable() {
                public void run() {
                    saveDiagnostics();
                }
            }, "q50-save").start();
            handler.postDelayed(saver, SAVE_EVERY_MS);
        }
    };

    /** Неизвестные коды клавиш пишем в лог: какие даёт это ГУ — неизвестно. */
    /** Отчёт и архив на флешку. Зовётся и по таймеру, и на выходе. */
    private void saveDiagnostics() {
        try {
            String path = DiagnosticReport.write(buildReport());
            if (path != null) {
                Log.i(TAG, "диагностика сохранена: " + path);
            }
        } catch (Throwable t) {
            Log.w(TAG, "отчёт не записан: " + t);
        }
        try {
            String bt = ecuTek.getProbe() == null
                    ? "Зонд Bluetooth не отработал.\n" : ecuTek.getProbe().getReport();
            String pars = EcuTekParameters.build(hub.getData(), ecuTek.getProbe(),
                    ecuTek.getStateName(), ecuTek.getLastError());
            String tr = transport == null ? "(не готово)\n" : transport.getReport();
            String zip = DiagnosticBundle.write(bt + "\n\n" + tr, pars,
                    rawLog.dump(), buildReport());
            if (zip != null) {
                Log.i(TAG, "архив диагностики: " + zip);
            }
        } catch (Throwable t) {
            Log.w(TAG, "архив не записан: " + t);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.i(TAG, "клавиша: код " + keyCode);
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(saver);
        dashboard.stop();
        hub.stop();
        // На выходе — ещё раз, самым свежим состоянием. Основная запись идёт
        // по таймеру: onPause() может не наступить вовсе.
        saveDiagnostics();
        super.onPause();
    }

    /**
     * Версия берётся из установленного пакета, а не из строки в коде: строка
     * в коде уже успела разойтись с манифестом, и отчёт врал о версии.
     */
    private String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Throwable t) {
            return "?";
        }
    }

    /** Полный диагностический отчёт: геометрия, зонд, состояние каналов. */
    private String buildReport() {
        StringBuilder b = new StringBuilder();
        b.append("Q50 GTR+ DIAGNOSTIC REPORT\n");
        b.append("version ").append(versionName()).append("\n\n");
        dashboard.getDisplayInfo().appendTo(b);
        b.append('\n');
        b.append("DATA SOURCE      = ").append(hub.getSourceLabel()).append('\n');
        b.append("MODE             = ").append(hub.isLive() ? "LIVE" : "DEMO").append('\n');
        b.append('\n').append("-- КАНАЛЫ --\n");
        Channel[] all = hub.getData().allChannels();
        long now = System.currentTimeMillis();
        for (int i = 0; i < all.length; i++) {
            Channel c = all[i];
            b.append(pad(c.id, 18)).append(pad(c.getStatusText(), 12))
                    .append(pad(c.hasValue() ? c.text(2) : "-", 10))
                    .append(pad(c.getSource() == null ? "-" : c.getSource(), 9))
                    .append("n=").append(c.getUpdates())
                    .append("  [").append(c.getObservedRange()).append(']');
            if (c.getUpdatedAtMs() != 0) {
                b.append("  age=").append(now - c.getUpdatedAtMs()).append("ms");
            }
            b.append('\n');
        }
        b.append('\n').append("-- ЗОНД --\n");
        if (probe != null) {
            b.append(probe.getReport());
        }
        return b.toString();
    }

    private static String pad(String s, int n) {
        StringBuilder b = new StringBuilder(s == null ? "-" : s);
        while (b.length() < n) {
            b.append(' ');
        }
        return b.toString();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_PAGE, dashboard.getPage());
    }
}
