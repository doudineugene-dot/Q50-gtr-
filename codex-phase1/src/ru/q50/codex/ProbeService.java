package ru.q50.codex;

import android.app.Service;
import android.content.*;
import android.content.pm.*;
import android.bluetooth.*;
import android.os.*;
import android.util.Log;
import java.io.*;
import java.util.*;

/** One run: baseline and timed snapshots while the owner opens VR30 DASH. */
public final class ProbeService extends Service {
    private boolean running;
    public IBinder onBind(Intent i){return null;}
    public synchronized int onStartCommand(Intent i,int flags,int id){
        if(!running){running=true;new Thread(new Runnable(){public void run(){collect();}},"DCU-evidence").start();}
        return START_NOT_STICKY;
    }
    private void section(PrintWriter out,String title,String value){out.println("\n## "+title);out.println(value);out.flush();}
    private void file(PrintWriter out,String path){section(out,path,ProbeIO.read(new File(path)));}
    private void directory(PrintWriter out,String path,int depth){
        File[] fs=new File(path).listFiles();
        if(fs==null){section(out,path,"LIST_FAILED (absent or denied)");return;}
        Arrays.sort(fs);int count=0;
        for(File f:fs){if(++count>512){out.println("DIRECTORY_TRUNCATED "+path);break;}
            out.println(f.getPath()+" length="+f.length()+" read="+f.canRead()+" write="+f.canWrite());
            if(depth>0 && f.isDirectory())directory(out,f.getPath(),depth-1);
        }out.flush();
    }
    private void command(PrintWriter out,final String[] argv){
        final StringBuilder result=new StringBuilder();final java.lang.Process[] process=new java.lang.Process[1];
        Thread t=new Thread(new Runnable(){public void run(){try{
            process[0]=new ProcessBuilder(argv).redirectErrorStream(true).start();
            result.append(ProbeIO.bounded(process[0].getInputStream(),ProbeIO.LIMIT));
        }catch(Exception e){result.append(e.toString());}}});
        t.start();try{t.join(2500);}catch(InterruptedException e){Thread.currentThread().interrupt();}
        if(t.isAlive()){if(process[0]!=null)process[0].destroy();section(out,Arrays.toString(argv),"TIMEOUT (2500ms)");}
        else section(out,Arrays.toString(argv),result.toString());
    }
    private void processes(PrintWriter out){
        File[] ps=new File("/proc").listFiles();if(ps==null){out.println("PROC_LIST_FAILED");return;}
        int count=0;for(File p:ps){if(!p.getName().matches("[0-9]+"))continue;
            if(++count>2048){out.println("PROCESS_TRUNCATED");break;}
            String cmd=ProbeIO.read(new File(p,"cmdline"));out.println("PID "+p.getName()+" "+cmd);
            if(ProbeIO.relevant(cmd)){
                file(out,p+"/status");file(out,p+"/maps");
                section(out,p+"/exe",NativeProbe.readlink(p+"/exe"));
                section(out,p+"/root",NativeProbe.readlink(p+"/root"));
                File[] fds=new File(p,"fd").listFiles();
                if(fds==null)out.println("FD_LIST_FAILED "+p);
                else {int n=0;for(File fd:fds){if(++n>512){out.println("FD_TRUNCATED");break;}
                    out.println(fd+" -> "+NativeProbe.readlink(fd.toString()));}}
            }
        }out.flush();
    }
    private void packages(PrintWriter out){
        int copied=0;
        for(PackageInfo p:getPackageManager().getInstalledPackages(PackageManager.GET_SERVICES|PackageManager.GET_PERMISSIONS)){
            String label=String.valueOf(p.applicationInfo.loadLabel(getPackageManager()));
            if(!ProbeIO.relevant(p.packageName+" "+label))continue;
            out.println("PACKAGE "+p.packageName+" label="+label+" version="+p.versionName+" apk="+p.applicationInfo.sourceDir);
            if(p.requestedPermissions!=null)out.println("permissions="+Arrays.toString(p.requestedPermissions));
            if(p.services!=null)for(ServiceInfo s:p.services)out.println("SERVICE "+s.name+" exported="+s.exported+" permission="+s.permission);
            if((p.packageName+" "+label).toLowerCase(Locale.US).contains("vr30") && copied++<4){
                File apk=new File(p.applicationInfo.sourceDir);
                if(apk.length()<64*1024*1024)try{copy(apk,new File(getFilesDir(),p.packageName+".apk"));out.println("VR30_APK_COPIED "+p.packageName);}
                catch(Exception e){out.println("APK_COPY_FAILED "+e);}
            }
        }out.flush();
    }
    private void collect(){
        File report=new File(getFilesDir(),"dcu-bluetooth-report.txt");
        File complete=new File(getFilesDir(),"COMPLETE");
        if(complete.exists() && !complete.delete()){Log.e("Q50Probe","Cannot clear previous completion marker");stopSelf();return;}
        try {PrintWriter out=new PrintWriter(new OutputStreamWriter(new FileOutputStream(report),"UTF-8"));
            try {
                section(out,"Q50 stock DCU probe v1",new Date()+"\nuid="+android.os.Process.myUid()+" pid="+android.os.Process.myPid()
                    +" SDK="+Build.VERSION.SDK_INT+" release="+Build.VERSION.RELEASE+" ABI="+Build.CPU_ABI
                    +"\nNo remote connect/send, no pairing/scan, no ECU commands. RPM/Boost UNPROVEN.");
                section(out,"native",NativeProbe.collect());
                final String[] captureResult=new String[]{"CAPTURE_PENDING"};
                Thread capture=new Thread(new Runnable(){public void run(){captureResult[0]=NativeProbe.passiveCapture(new File(getFilesDir(),"hci-passive.txt").toString());}},"HCI-passive");
                capture.start();
                section(out,"BluetoothAdapter surface",ProbeIO.methods(BluetoothAdapter.class));
                section(out,"BluetoothDevice surface",ProbeIO.methods(BluetoothDevice.class));
                section(out,"BluetoothSocket surface",ProbeIO.methods(BluetoothSocket.class));
                try {BluetoothAdapter a=BluetoothAdapter.getDefaultAdapter();
                    section(out,"adapter",a==null?"NULL (not proof of missing host Bluetooth)":"state="+a.getState()+" name="+a.getName()+" address="+a.getAddress());
                    if(a!=null)for(BluetoothDevice d:a.getBondedDevices())out.println("BONDED "+d.getName()+" "+d.getAddress());
                }catch(Throwable e){section(out,"adapter failure",e.toString());}
                packages(out);
                String[] files={"/proc/version","/proc/modules","/proc/mounts","/proc/self/status","/proc/self/maps","/proc/1/mountinfo",
                    "/system/build.prop","/etc/meego-release","/init.rc","/etc/bluetooth/main.conf"};
                for(String f:files)file(out,f);
                String[] dirs={"/sys/class/bluetooth","/sys/class/rfkill","/sys/bus/usb/devices","/dev","/system/framework","/system/lib",
                    "/system/app","/usr/lib","/usr/sbin","/etc/dbus-1/system.d","/home/naviwork/system/bin","/home/naviwork/system/lib",
                    "/proc/1/root/home/naviwork/system/bin","/proc/1/root/usr/lib","/mnt","/media","/storage"};
                for(String d:dirs){section(out,"directory",d);directory(out,d,d.indexOf("/sys/")==0?1:0);}
                File[] controllers=new File("/sys/class/bluetooth").listFiles();
                if(controllers!=null)for(File c:controllers){file(out,c+"/uevent");file(out,c+"/address");file(out,c+"/name");
                    section(out,"controller device "+c,NativeProbe.readlink(c+"/device"));}
                command(out,new String[]{"/system/bin/getprop"});
                command(out,new String[]{"/system/bin/service","list"});
                command(out,new String[]{"/system/bin/dumpsys","bluetooth"});
                for(int n=0;n<3;n++){
                    section(out,"snapshot "+n,"wall="+System.currentTimeMillis()+" elapsed="+SystemClock.elapsedRealtime());
                    for(String f:new String[]{"/proc/net/unix","/proc/net/bluetooth/l2cap","/proc/net/bluetooth/rfcomm","/proc/net/bluetooth/hci"})file(out,f);
                    processes(out);
                    if(n<2)Thread.sleep(15000);
                }
                capture.join(35000);section(out,"passive HCI",captureResult[0]);
                section(out,"completion","COMPLETE; transport/ECUTEK_RPM/BOOST_ACTUAL remain UNPROVEN");
                if(out.checkError())throw new IOException("Report write failed; no completion marker");
            }finally{out.close();}
            FileOutputStream marker=new FileOutputStream(complete);marker.close();
            File external=Environment.getExternalStorageDirectory();
            try {copy(report,new File(external,"Q50-DCU-bluetooth-report.txt"));
                File bundle=new File(external,"Q50-DCU-evidence");if(!bundle.exists() && !bundle.mkdirs())throw new IOException("Cannot create evidence directory");
                for(File f:getFilesDir().listFiles())if(f.getName().equals("hci-passive.txt") || f.getName().endsWith(".apk"))copy(f,new File(bundle,f.getName()));
                Log.i("Q50Probe","EXPORT "+external);}
            catch(Exception e){Log.w("Q50Probe","EXPORT_FAILED "+e);}
            Log.i("Q50Probe","COMPLETE "+report);
        }catch(Exception e){Log.e("Q50Probe","FAILED",e);}finally{stopSelf();}
    }
    static void copy(File from,File to)throws IOException {
        InputStream in=new FileInputStream(from);try{OutputStream out=new FileOutputStream(to);
            try{byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}finally{out.close();}
        }finally{in.close();}
    }
}
