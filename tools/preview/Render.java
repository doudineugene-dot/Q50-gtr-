import android.content.Context;
import android.graphics.Canvas;

import com.q50gtr.plus.data.DataHub;
import com.q50gtr.plus.data.VehicleData;
import com.q50gtr.plus.ui.DashboardView;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;
import javax.imageio.ImageIO;

/**
 * Рендерит все три вкладки тем же кодом, который идёт в APK. Android-классы
 * подменены тонким слоем поверх Java2D, но Theme, OemDial, OemTile,
 * OemNavigation, OemStatusBar, Icons, Q50Rear и сами страницы — настоящие.
 *
 * Пишет actual-<экран>.png — ровно то, что выводит приложение: фоновый
 * растр из res/drawable-nodpi плюс динамический слой поверх него. Кадр
 * 840x480, тот же, что у MASTER, поэтому сравнение попиксельное.
 */
public final class Render {

    private static final String[] NAMES = {"engine", "fuel", "chassis"};

    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : ".");
        out.mkdirs();
        // Второй аргумент — размер вью, чтобы проверить экран ГУ 800x480.
        int viewW = args.length > 1 ? Integer.parseInt(args[1]) : 840;
        int viewH = args.length > 2 ? Integer.parseInt(args[2]) : 480;

        DataHub hub = new DataHub();
        hub.start();
        long t0 = System.currentTimeMillis();
        for (int i = 0; i < 40; i++) {
            hub.poll(t0 + i * 100L);
        }
        // Кадр должен быть повторяемым и сравнимым с эталоном, поэтому во
        // все каналы кладутся ровно те значения, которые показывает MASTER.
        // Это поза для сравнения, а не подмена источника данных: сам
        // DataHub остаётся тем же, что в APK.
        pose(hub.getData(), t0);
        VehicleData d = hub.getData();

        DashboardView view = new DashboardView(new Context(), hub);
        view.setClockOverride("11:06");
        Method onDraw = DashboardView.class.getDeclaredMethod("onDraw", Canvas.class);
        onDraw.setAccessible(true);

        view.setSize(viewW, viewH);
        for (int page = 0; page < 3; page++) {
            view.setPage(page);
            BufferedImage img = new BufferedImage(viewW, viewH, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            hint(g);
            onDraw.invoke(view, new Canvas(g));
            g.dispose();
            write(img, new File(out, "actual-" + NAMES[page] + ".png"));
        }
    }

    /** Значения, снятые с MASTER: см. docs/UI-MASTER-SPEC.md. */
    private static void pose(VehicleData d, long now) {
        // Углы стрелок, измеренные по MASTER: RPM 156.3°, BOOST 148.2°.
        d.rpm.setDemo(325f, now);
        d.boostActual.setDemo(-0.976f, now);
        d.coolantTemp.setDemo(93f, now);
        d.oilTemp.setDemo(97f, now);
        d.oilPressure.setDemo(4.1f, now);
        d.intakeTemp.setDemo(41f, now);

        // LPFP не логируется — значение не подставляем, прибор покажет «—».
        d.lpfp.setUnavailable();
        d.hpfpActual.setDemo(125f, now);
        d.stftB1.setDemo(2f, now);
        d.afrB1.setDemo(11.6f, now);
        d.boostTarget.setDemo(1.6f, now);

        d.airFrontLeft.setDemo(2.4f, now);
        d.airFrontRight.setDemo(2.4f, now);
        d.airRearLeft.setDemo(2.3f, now);
        d.airRearRight.setDemo(2.3f, now);
        d.transmissionTemp.setDemo(72f, now);
        d.transferCaseTemp.setDemo(48f, now);
        d.batteryVoltage.setDemo(13.8f, now);
        d.ambientTemp.setDemo(15f, now);
        d.ignitionTiming.setDemo(12f, now);
        d.knockRetard.setDemo(1.6f, now);
        for (int i = 0; i < d.knockIndex.length; i++) {
            d.knockIndex[i].setDemo(i == 2 ? 1.6f : 0.9f, now);
        }
        d.throttle.setDemo(18f, now);
        d.speed.setDemo(0f, now);
    }

    private static BufferedImage blank() {
        return new BufferedImage(840, 480, BufferedImage.TYPE_INT_RGB);
    }

    private static void hint(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE);
    }

    private static void write(BufferedImage img, File f) throws Exception {
        ImageIO.write(img, "png", f);
        System.out.println("написано " + f.getPath());
    }
}
