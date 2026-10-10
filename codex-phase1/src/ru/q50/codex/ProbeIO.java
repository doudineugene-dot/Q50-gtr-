package ru.q50.codex;

import java.io.*;
import java.util.Locale;

/** Bounded evidence reads. No shell interpolation, vehicle commands or protocol. */
public final class ProbeIO {
    public static final int LIMIT=65536;
    public static boolean relevant(String name) {
        String n=name.toLowerCase(Locale.US);
        return n.indexOf("bluetooth")>=0 || n.indexOf("bluez")>=0 || n.indexOf("bt_")>=0
            || n.indexOf("ygomi")>=0 || n.indexOf("connexis")>=0 || n.indexOf("ecutek")>=0
            || n.indexOf("vr30")>=0 || n.indexOf("dash")>=0 || n.indexOf("carwings")>=0
            || n.indexOf("dbus")>=0 || n.indexOf("tel_proc")>=0 || n.indexOf("hci")>=0;
    }
    public static String read(File f) {
        try {
            InputStream in=new FileInputStream(f);
            try { return bounded(in,LIMIT); } finally { in.close(); }
        } catch(Exception e) { return "READ_FAILED "+e.toString(); }
    }
    public static String bounded(InputStream in,int limit) throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;
        while(out.size()<limit && (n=in.read(b,0,Math.min(b.length,limit-out.size())))!=-1)out.write(b,0,n);
        String s=new String(out.toByteArray(),"UTF-8").replace('\0',' ');
        return s+(out.size()==limit?"\nTRUNCATED_AT "+limit:"");
    }
    public static String methods(Class<?> c) {
        StringBuilder s=new StringBuilder(c.getName()+"\n");
        try { for(java.lang.reflect.Method m:c.getDeclaredMethods())s.append(m.toString()).append('\n');
            for(java.lang.reflect.Constructor<?> k:c.getDeclaredConstructors())s.append(k.toString()).append('\n');
        }catch(Throwable e){s.append(e.toString());}return s.toString();
    }
}
