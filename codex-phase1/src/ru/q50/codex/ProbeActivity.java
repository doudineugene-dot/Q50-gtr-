package ru.q50.codex;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import java.io.File;

public final class ProbeActivity extends Activity {
    public void onCreate(Bundle state){super.onCreate(state);
        if(!new File(getFilesDir(),"dcu-bluetooth-report.txt").exists())startService(new Intent(this,ProbeService.class));
        LinearLayout layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);
        Button run=new Button(this);run.setText("Запустить сбор (30 секунд + чтение системы)");layout.addView(run);
        final TextView text=new TextView(this);text.setTextSize(12);
        Button refresh=new Button(this);refresh.setText("Обновить отчёт / путь экспорта");layout.addView(refresh);
        Button export=new Button(this);export.setText("Экспорт завершённого сбора на USB");layout.addView(export);
        ScrollView scroll=new ScrollView(this);scroll.addView(text);layout.addView(scroll);setContentView(layout);
        final Runnable show=new Runnable(){public void run(){File f=new File(getFilesDir(),"dcu-bluetooth-report.txt");
            String s=ProbeIO.read(f);text.setText("EVI DISCOVERED / CONNECTED: UNKNOWN\nTransport / remote / HCI handle / PSM / CID: UNKNOWN\nEVI RX bytes/s / packets/s / age / hex: NO VERIFIED DATA\nECUTEK RPM / BOOST ACTUAL: НЕ ДОКАЗАНЫ\nПассивный сбор; соединение и декодирование ещё не реализованы.\n"+f+"\nЭкспорт: корень shared storage / Q50-DCU-bluetooth-report.txt\n\n"+s);}};
        run.setOnClickListener(new View.OnClickListener(){public void onClick(View v){startService(new Intent(ProbeActivity.this,ProbeService.class));show.run();}});
        refresh.setOnClickListener(new View.OnClickListener(){public void onClick(View v){show.run();}});show.run();
        export.setOnClickListener(new View.OnClickListener(){public void onClick(View v){
            if(!new File(getFilesDir(),"COMPLETE").isFile()){text.setText("Сбор ещё не завершён. Подождите и обновите отчёт.");return;}
            new Thread(new Runnable(){public void run(){final StringBuilder result=new StringBuilder();
                java.util.List<File> mounts=ProbeIO.usbMounts(ProbeIO.read(new File("/proc/mounts")));
                if(mounts.isEmpty())result.append("USB mount не виден Android; shared storage или existing host shell.\n");
                for(File mount:mounts){try{File target=new File(mount,"Q50-DCU-evidence-"+System.currentTimeMillis());
                    if(!target.mkdir())throw new java.io.IOException("mkdir failed "+target);
                    for(File f:getFilesDir().listFiles())if(f.isFile())ProbeService.copy(f,new File(target,f.getName()));
                    result.append("EXPORT ").append(target).append('\n');
                }catch(Exception e){result.append("EXPORT_FAILED ").append(e).append('\n');}}
                runOnUiThread(new Runnable(){public void run(){text.setText(result.toString());}});
            }},"USB-report-export").start();
        }});
    }
}
