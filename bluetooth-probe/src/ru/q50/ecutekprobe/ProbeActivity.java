package ru.q50.ecutekprobe;

import android.app.Activity;
import android.os.*;
import android.content.Intent;
import android.view.View;
import android.widget.*;
import java.io.File;
import java.util.List;

public final class ProbeActivity extends Activity {
    private TextView status;
    private final Handler handler=new Handler();
    private final Runnable refresh=new Runnable(){public void run(){status.setText(ProbeService.progress);handler.postDelayed(this,1000);}};
    public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);layout.setPadding(12,8,12,8);
        TextView title=new TextView(this);title.setText("Q50 / EcuTek — штатный Bluetooth: пассивная диагностика");title.setTextSize(19);layout.addView(title);
        TextView gate=new TextView(this);gate.setText(
            "EVI DISCOVERED / CONNECTED: UNKNOWN · transport / remote / HCI / PSM / CID: UNKNOWN\n"+
            "EVI RX байт/с · пакетов/с · возраст · hex: НЕТ ДАННЫХ\n"+
            "ECUTEK RPM: НЕТ ДАННЫХ     BOOST ACTUAL: НЕТ ДАННЫХ\n"+
            "Пробник собирает сведения 90 с в фоне. Соединение и декодер EcuTek ещё не реализованы.");
        gate.setTextSize(16);layout.addView(gate);
        LinearLayout buttons=new LinearLayout(this);
        Button start=new Button(this);start.setText("НАЧАТЬ 90 С");buttons.addView(start);
        start.setOnClickListener(new View.OnClickListener(){public void onClick(View v){startService(new Intent(ProbeActivity.this,ProbeService.class));}});
        Button export=new Button(this);export.setText("ЭКСПОРТ НА USB");buttons.addView(export);
        export.setOnClickListener(new View.OnClickListener(){public void onClick(View v){exportUsb();}});
        layout.addView(buttons);status=new TextView(this);status.setTextSize(15);layout.addView(status);
        ScrollView scroll=new ScrollView(this);scroll.addView(layout);setContentView(scroll);
        String last=getSharedPreferences("probe",0).getString("last",null);
        if(ProbeService.lastDirectory==null && last!=null){ProbeService.lastDirectory=new File(last);ProbeService.progress="Предыдущий отчёт: "+last;}
        android.util.Log.i("Q50BtProbe","READY ABI="+Build.CPU_ABI+" JNI="+NativeProbe.loadError);
        int testSeconds=getIntent().getIntExtra("seconds",0);
        if(testSeconds>0)startService(new Intent(this,ProbeService.class).putExtra("seconds",testSeconds));
    }
    protected void onResume(){super.onResume();handler.removeCallbacks(refresh);handler.post(refresh);}
    protected void onPause(){handler.removeCallbacks(refresh);super.onPause();}
    private void exportUsb() {
        if(ProbeService.running){ProbeService.progress="Дождитесь завершения 90 с перед экспортом.";return;}
        final File dir=ProbeService.lastDirectory;
        if(dir==null || !new File(dir,"COMPLETE.txt").isFile()){ProbeService.progress="Нет завершённого отчёта.";return;}
        new Thread(new Runnable(){public void run(){
            List<File> mounts=EvidenceIO.usbMounts(EvidenceIO.file("/proc/mounts"));StringBuilder result=new StringBuilder();
            for(File mount:mounts)result.append(ProbeService.export(mount,dir)).append("\n");
            ProbeService.progress=mounts.isEmpty()?"USB не виден в Android. Отчёт: "+ProbeService.export(Environment.getExternalStorageDirectory(),dir):result.toString();
        }},"q50-export").start();
    }
}
