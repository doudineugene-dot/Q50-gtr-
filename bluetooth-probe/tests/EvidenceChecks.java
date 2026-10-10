package ru.q50.ecutekprobe;
import java.io.*;
import java.util.*;
import java.util.zip.*;

public final class EvidenceChecks {
    public static void main(String[] args) throws Exception {
        File dir=new File(args[0]);dir.mkdirs();
        File data=new File(dir,"report.txt");EvidenceIO.write(data,"abc\n");
        if(!EvidenceIO.read(data,2).startsWith("ab\n[LIMIT]"))throw new AssertionError("bounded read");
        if(!EvidenceIO.file(new File(dir,"missing").getPath()).contains("UNREADABLE"))throw new AssertionError("absence must be explicit");
        List<File> mounts=EvidenceIO.usbMounts("/dev/a /mnt/usb\\040drive vfat rw 0 0\n/dev/b /system ext4 rw 0 0\n/dev/c /data vfat rw 0 0\n");
        if(mounts.size()!=1 || !mounts.get(0).getPath().contains("usb drive"))throw new AssertionError("USB selection");
        File archive=new File(dir,"archive.zip");EvidenceIO.zip(dir,archive);
        ZipFile zip=new ZipFile(archive);try {
            if(zip.getEntry("report.txt")==null || zip.getEntry("archive.zip")!=null)throw new AssertionError("ZIP contents");
        }finally{zip.close();}
        String command=EvidenceIO.command(new String[]{"java","-version"});
        if(!command.contains("[exit=0]"))throw new AssertionError(command);
        String denied=EvidenceIO.command(new String[]{"nonexistent-q50-probe-command"});
        if(!denied.contains("COMMAND ERROR"))throw new AssertionError("command failure");
        System.out.println("PASS: bounded files, explicit denial, USB mount escaping/filter, ZIP export, process execution/failure");
    }
}
