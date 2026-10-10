package ru.q50.codex;

public final class NativeProbe {
    private static native String snapshot();
    private static native String link(String path);
    private static native String capture(String path);
    public static String collect() {
        try { System.loadLibrary("q50probe");return snapshot(); }
        catch(Throwable e){return "NATIVE_UNAVAILABLE "+e.toString();}
    }
    public static String readlink(String path) {
        try { return link(path); }catch(Throwable e){return "READLINK_FAILED "+e.toString();}
    }
    public static String passiveCapture(String path){
        try{return capture(path);}catch(Throwable e){return "CAPTURE_UNAVAILABLE "+e.toString();}
    }
}
