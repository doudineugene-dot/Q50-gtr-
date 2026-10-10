package ru.q50.ecutekprobe;

import java.io.*;
import java.util.*;
import java.util.zip.*;

/** Bounded evidence collection; never recursively read device nodes or credentials. */
final class EvidenceIO {
    static final int LIMIT=262144;
    static String read(File file,int limit) throws IOException {
        InputStream in=new FileInputStream(file);
        try {
            ByteArrayOutputStream out=new ByteArrayOutputStream(); byte[] b=new byte[4096];
            int n,remaining=limit;
            while(remaining>0 && (n=in.read(b,0,Math.min(b.length,remaining)))!=-1){out.write(b,0,n);remaining-=n;}
            return new String(out.toByteArray(),"UTF-8").replace('\0',' ')+(remaining==0?"\n[LIMIT]\n":"");
        } finally { in.close(); }
    }
    static void write(File file,String value) throws IOException {
        Writer w=new OutputStreamWriter(new FileOutputStream(file),"UTF-8");
        try {w.write(value);} finally {w.close();}
    }
    static String file(String path) {
        try{return read(new File(path),LIMIT);}catch(Exception e){return "[UNREADABLE] "+path+" "+e+"\n";}
    }
    static String listing(String path) {
        File dir=new File(path); File[] files=dir.listFiles();
        if(files==null)return "[UNREADABLE OR ABSENT] "+path+"\n";
        Arrays.sort(files); StringBuilder out=new StringBuilder();
        for(int i=0;i<files.length && i<512;i++) {
            File f=files[i];out.append(f.getName()).append(" r=").append(f.canRead()).append(" w=").append(f.canWrite()).append("\n");
        }
        if(files.length>512)out.append("[LIST LIMIT]\n");return out.toString();
    }
    static String command(String[] args) {
        final ByteArrayOutputStream out=new ByteArrayOutputStream();
        Process p=null; Thread reader=null;
        try {
            p=new ProcessBuilder(args).redirectErrorStream(true).start(); final InputStream in=p.getInputStream();
            reader=new Thread(new Runnable(){public void run(){
                try {byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){synchronized(out){if(out.size()<LIMIT)out.write(b,0,Math.min(n,LIMIT-out.size()));}}}
                catch(IOException ignored){} finally{try{in.close();}catch(IOException ignored){}}
            }},"probe-command-reader"); reader.setDaemon(true);reader.start();
            long deadline=System.nanoTime()+3000000000L; boolean exited=false;
            while(System.nanoTime()<deadline) {
                try {int code=p.exitValue();synchronized(out){out.write(("\n[exit="+code+"]\n").getBytes("UTF-8"));}exited=true;break;}
                catch(IllegalThreadStateException running){Thread.sleep(50);}
            }
            if(!exited){p.destroy();synchronized(out){out.write("\n[TIMEOUT]\n".getBytes("UTF-8"));}}
            reader.join(500);synchronized(out){return out.toString("UTF-8");}
        }catch(Exception e){return "[COMMAND ERROR] "+e+"\n";}
        finally{if(p!=null)p.destroy();}
    }
    static List<File> usbMounts(String mounts) {
        List<File> result=new ArrayList<File>();
        for(String line:mounts.split("\n")) {
            String[] fields=line.split(" +");
            if(fields.length<4 || !(fields[2].equals("vfat") || fields[2].equals("exfat") || fields[2].equals("fuse")))continue;
            // Only actual USB-labelled mounts, never internal /data or /system.
            String path=fields[1].replace("\\040"," ").replace("\\134","\\");
            String lower=path.toLowerCase(Locale.US);
            if(path.startsWith("/") && (lower.contains("usb") || lower.startsWith("/media/")))result.add(new File(path));
        }
        return result;
    }
    static void zip(File dir,File target) throws IOException {
        ZipOutputStream zip=new ZipOutputStream(new FileOutputStream(target));
        try {
            File[] files=dir.listFiles();if(files==null)throw new IOException("missing evidence directory");Arrays.sort(files);
            byte[] b=new byte[8192];
            for(File f:files) {
                if(!f.isFile() || f.equals(target))continue;
                zip.putNextEntry(new ZipEntry(f.getName()));InputStream in=new FileInputStream(f);
                try {int n;while((n=in.read(b))!=-1)zip.write(b,0,n);}finally{in.close();}zip.closeEntry();
            }
        }finally{zip.close();}
    }
}
