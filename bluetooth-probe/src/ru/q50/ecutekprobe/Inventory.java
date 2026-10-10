package ru.q50.ecutekprobe;

import android.content.Context;
import android.content.pm.*;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Build;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.zip.*;

final class Inventory {
    private static final Set<String> identifiedPackages=new HashSet<String>();
    private static boolean relevant(String name) {
        String s=name.toLowerCase(Locale.US);
        for(String known:identifiedPackages)if(s.contains(known))return true;
        return s.contains("bluetooth") || s.contains("bluez") || s.contains("ecutek") || s.contains("vr30") || s.contains("dash") ||
            s.contains("ygomi") || s.contains("connexis") || s.contains("dbus") || s.contains("carwings") ||
            s.contains("audio_ps") || s.contains("tel_proc") || s.contains("cmm") || s.contains("bta");
    }
    static void collect(Context context,File dir,int index) throws IOException {
        StringBuilder out=new StringBuilder("snapshot="+index+" wallMs="+System.currentTimeMillis()+"\n");
        out.append("Android ").append(Build.VERSION.RELEASE).append(" sdk=").append(Build.VERSION.SDK_INT)
            .append(" ABI=").append(Build.CPU_ABI).append(" ABI2=").append(Build.CPU_ABI2).append("\n")
            .append("JNI loadError=").append(NativeProbe.loadError).append("\n");
        String[] paths={"/proc/version","/proc/self/status","/proc/self/mountinfo","/proc/mounts","/proc/modules",
            "/proc/net/unix","/proc/net/bluetooth/l2cap","/proc/net/bluetooth/rfcomm","/proc/net/bluetooth/hci",
            "/proc/net/bluetooth/hci_conn","/sys/kernel/debug/bluetooth/l2cap","/sys/kernel/debug/bluetooth/hci0/conn_info"};
        for(String path:paths)out.append("\n== ").append(path).append(" ==\n").append(EvidenceIO.file(path));
        String[] lists={"/sys/class/bluetooth","/dev/socket","/dev","/system/lib","/system/framework",
            "/system/bin","/system/xbin","/usr/lib","/usr/sbin","/run","/var/run","/home/naviwork/system/bin",
            "/home/naviwork/system/lib","/proc/1/root","/proc/1/root/home/naviwork/system/bin"};
        for(String path:lists)out.append("\n== list ").append(path).append(" ==\n").append(EvidenceIO.listing(path));
        File[] controllers=new File("/sys/class/bluetooth").listFiles();
        if(controllers!=null)for(File controller:controllers) {
            out.append("\ncontroller=").append(controller.getName()).append("\n");
            for(String path:new String[]{"uevent","address","name","device/uevent","device/modalias"})
                out.append(path).append("=").append(EvidenceIO.file(new File(controller,path).getPath()));
            if(NativeProbe.loadError==null)for(String link:new String[]{"device","device/driver"})
                out.append(link).append(" -> ").append(NativeProbe.readLink(new File(controller,link).getPath())).append("\n");
        }
        try {
            BluetoothAdapter adapter=BluetoothAdapter.getDefaultAdapter();out.append("\nBluetoothAdapter=").append(adapter==null?"null":adapter.getState()).append("\n");
            if(adapter!=null)for(BluetoothDevice device:adapter.getBondedDevices())out.append("BONDED_ONLY ").append(device.getAddress()).append(" name=").append(device.getName()).append("\n");
        }catch(Exception e){out.append("BluetoothAdapter error ").append(e).append("\n");}
        // Introspect signatures only: never invoke a hidden/vendor connection method.
        for(String name:new String[]{"android.bluetooth.BluetoothAdapter","android.bluetooth.BluetoothDevice",
            "android.bluetooth.BluetoothSocket","android.bluetooth.IBluetooth","android.os.ServiceManager"}) {
            try {Class<?> c=Class.forName(name,false,context.getClassLoader());out.append("\nCLASS ").append(name).append("\n");
                for(Method m:c.getDeclaredMethods())out.append(m.toString()).append("\n");
            }catch(Throwable e){out.append("CLASS ").append(name).append(" ").append(e).append("\n");}
        }
        for(String[] cmd:new String[][]{{"/system/bin/service","list"},{"/system/bin/getprop"},{"/system/bin/ps"}})
            out.append("\nCOMMAND ").append(Arrays.toString(cmd)).append("\n").append(EvidenceIO.command(cmd));
        EvidenceIO.write(new File(dir,"inventory-"+index+".txt"),out.toString());
        if(index==0)packages(context,dir);processes(dir,index);
    }
    private static void processes(File dir,int index) throws IOException {
        StringBuilder out=new StringBuilder();File[] pids=new File("/proc").listFiles();int inspected=0;
        if(pids==null){EvidenceIO.write(new File(dir,"processes-"+index+".txt"),"[PROC UNREADABLE]");return;}
        Arrays.sort(pids);
        for(File pid:pids) {
            if(!pid.getName().matches("[0-9]+"))continue;
            String cmd=EvidenceIO.file(new File(pid,"cmdline").getPath());
            if(!relevant(cmd) && !pid.getName().equals("1"))continue;
            if(++inspected>64 || out.length()>1048576){out.append("[PROCESS/AGGREGATE LIMIT]\n");break;}
            out.append("\nPID ").append(pid.getName()).append(" CMD ").append(cmd).append("\n");
            for(String leaf:new String[]{"status","maps"}) {
                out.append(leaf).append("\n");
                try {out.append(EvidenceIO.read(new File(pid,leaf),65536));}
                catch(IOException e){out.append("[UNREADABLE] ").append(e).append("\n");}
            }
            File[] fds=new File(pid,"fd").listFiles();
            if(fds==null)out.append("[FD UNREADABLE]\n");
            else if(NativeProbe.loadError==null)for(int i=0;i<fds.length && i<256 && out.length()<1048576;i++)out.append("fd ").append(fds[i].getName()).append(" -> ").append(NativeProbe.readLink(fds[i].getPath())).append("\n");
            else out.append("[JNI readlink unavailable]\n");
            if(NativeProbe.loadError==null)out.append("root -> ").append(NativeProbe.readLink(new File(pid,"root").getPath())).append("\n");
        }
        out.append("inspected relevant PIDs=").append(inspected).append("\n");
        EvidenceIO.write(new File(dir,"processes-"+index+".txt"),out.toString());
    }
    private static void packages(Context context,File dir) throws IOException {
        StringBuilder out=new StringBuilder();PackageManager pm=context.getPackageManager();
        for(PackageInfo p:pm.getInstalledPackages(PackageManager.GET_PERMISSIONS|PackageManager.GET_SERVICES)) {
            String label=String.valueOf(p.applicationInfo.loadLabel(pm));
            out.append(p.packageName).append(" label=").append(label).append(" version=").append(p.versionName)
                .append(" uid=").append(p.applicationInfo.uid).append(" apk=").append(p.applicationInfo.sourceDir).append("\n");
            if(!relevant(p.packageName+" "+label))continue;
            identifiedPackages.add(p.packageName.toLowerCase(Locale.US));
            out.append(" permissions=").append(Arrays.toString(p.requestedPermissions)).append("\n");
            if(p.services!=null)for(ServiceInfo s:p.services)out.append(" service=").append(s.name).append(" permission=").append(s.permission).append("\n");
            try {ZipFile apk=new ZipFile(p.applicationInfo.sourceDir);
                try {Enumeration<? extends ZipEntry> entries=apk.entries();while(entries.hasMoreElements()){
                    ZipEntry e=entries.nextElement();String n=e.getName();
                    if(n.startsWith("lib/") || n.endsWith(".dex"))out.append(" APK entry ").append(n).append(" bytes=").append(e.getSize()).append("\n");
                }}finally{apk.close();}
            }catch(Exception e){out.append("[APK INVENTORY ERROR] ").append(e).append("\n");}
        }
        // No copying proprietary APKs, pairing stores, link keys, or phone data.
        EvidenceIO.write(new File(dir,"packages.txt"),out.toString());
    }
}
