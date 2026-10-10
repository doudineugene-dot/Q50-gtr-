package ru.q50.ecutekprobe;

final class NativeProbe {
    static final String loadError;
    static {
        String error=null;
        try { System.loadLibrary("q50btprobe"); }
        catch (LinkageError e) { error=e.toString(); }
        catch (SecurityException e) { error=e.toString(); }
        loadError=error;
    }
    static native int capture(String directory,int seconds);
    static native String readLink(String path);
}
