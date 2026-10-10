package ru.q50.ecutekprobe;

import android.app.*;
import android.content.*;
import android.os.*;
import android.util.Log;
import java.io.*;

public final class ProbeService extends Service {
    static volatile boolean running=false;
    static volatile String progress="Готов к сбору. Сначала отключите VR30 DASH от EVI.";
    static volatile File lastDirectory;
    private static final String TAG="Q50BtProbe";
    public IBinder onBind(Intent intent){return null;}
    public int onStartCommand(Intent intent,int flags,int id) {
        if(running)return START_NOT_STICKY;
        final int seconds=Math.max(5,Math.min(90,intent==null?90:intent.getIntExtra("seconds",90)));
        running=true;progress="Сбор начат: "+seconds+" с. Можно открыть VR30 DASH.";
        try {startForeground(1,notification());}
        catch(Exception e){running=false;progress="Ошибка notification: "+e;Log.e(TAG,progress);stopSelf();return START_NOT_STICKY;}
        new Thread(new Runnable(){public void run(){collect(seconds);}},"q50-evidence").start();
        return START_NOT_STICKY;
    }
    private Notification notification() throws Exception {
        PendingIntent content=PendingIntent.getActivity(this,0,new Intent(this,ProbeActivity.class),0);
        if(Build.VERSION.SDK_INT<11){
            Notification n=new Notification(android.R.drawable.stat_notify_sync,"Q50: пассивная диагностика",System.currentTimeMillis());
            Notification.class.getMethod("setLatestEventInfo",Context.class,CharSequence.class,CharSequence.class,PendingIntent.class)
                .invoke(n,this,"Q50 EcuTek Probe","Сбор в фоне; команды ECU не отправляются",content);return n;
        }
        // Keep API10 compile/runtime while supporting the newer test emulator.
        Class<?> type=Class.forName("android.app.Notification$Builder");
        Object builder=type.getConstructor(Context.class).newInstance(this);
        type.getMethod("setSmallIcon",Integer.TYPE).invoke(builder,android.R.drawable.stat_notify_sync);
        type.getMethod("setContentTitle",CharSequence.class).invoke(builder,"Q50 EcuTek Probe");
        type.getMethod("setContentText",CharSequence.class).invoke(builder,"Сбор в фоне; команды ECU не отправляются");
        type.getMethod("setContentIntent",PendingIntent.class).invoke(builder,content);
        return (Notification)type.getMethod("getNotification").invoke(builder);
    }
    private void collect(final int seconds) {
        Thread nativeThread=null;
        try {
            final File dir=new File(getFilesDir(),"run-"+System.currentTimeMillis());
            if(!dir.mkdirs())throw new IOException("cannot create report directory");
            lastDirectory=dir;getSharedPreferences("probe",0).edit().putString("last",dir.getPath()).commit();
            EvidenceIO.write(new File(dir,"SUMMARY.txt"),
                "EVI DISCOVERED=UNKNOWN\nEVI CONNECTED=UNKNOWN\ntransport=UNKNOWN\nremote=UNKNOWN\nHCI handle=UNKNOWN\nPSM/CID=UNKNOWN\n"+
                "EVI RX bytes/sec=UNAVAILABLE\nEVI RX packets/sec=UNAVAILABLE\nEVI last RX age=UNAVAILABLE\nEVI raw RX=UNAVAILABLE\n"+
                "ECUTEK RPM=UNAVAILABLE\nECUTEK BOOST ACTUAL=UNAVAILABLE\n"+
                "This is passive transport reconnaissance, not an EcuTek connection or decoder.\n"+
                "Generic HCI counters are NOT EVI telemetry. No DEMO, fallback, remote connect, or protocol writes.\n"+
                "durationSec="+seconds+"\nJNI="+NativeProbe.loadError+"\n");
            if(NativeProbe.loadError==null) {
                nativeThread=new Thread(new Runnable(){public void run(){
                    try {int code=NativeProbe.capture(dir.getPath(),seconds);Log.i(TAG,"native result="+code);}
                    catch(Throwable e){try{EvidenceIO.write(new File(dir,"native-error.txt"),e.toString());}catch(IOException ignored){}}
                }},"q50-hci-passive");nativeThread.start();
            }
            long start=SystemClock.elapsedRealtime();int count=seconds<30?2:4;
            for(int i=0;i<count;i++) {
                long due=start+(long)seconds*1000*i/(count-1);
                while(SystemClock.elapsedRealtime()<due)Thread.sleep(Math.min(250,due-SystemClock.elapsedRealtime()));
                Inventory.collect(this,dir,i);progress="Снимок "+(i+1)+"/"+count+". "+dir.getName();
                Log.i(TAG,"snapshot="+i);
            }
            if(nativeThread!=null)nativeThread.join();
            EvidenceIO.write(new File(dir,"COMPLETE.txt"),"collection complete; EcuTek live RPM/boost not proven\n");
            File zip=new File(getFilesDir(),dir.getName()+".zip");EvidenceIO.zip(dir,zip);
            String exported=export(Environment.getExternalStorageDirectory(),dir);
            progress="Готово. "+exported;Log.i(TAG,"COMPLETE "+exported);
        }catch(Exception e){progress="Ошибка сбора: "+e;Log.e(TAG,progress);}
        finally{running=false;stopForeground(true);stopSelf();}
    }
    static String export(File root,File dir) {
        try {
            File target=new File(root,"Q50-EcuTek-Probe");if(!target.isDirectory() && !target.mkdirs())throw new IOException("no writable export directory");
            File zip=new File(target,dir.getName()+".zip");EvidenceIO.zip(dir,zip);return zip.getPath();
        }catch(Exception e){return "Экспорт недоступен: "+root+" "+e;}
    }
}
