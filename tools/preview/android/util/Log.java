package android.util;

/** Подмена android.util.Log для офлайн-рендера: пишет в stderr. */
public final class Log {
    private Log() { }

    public static int i(String tag, String msg) { System.err.println("I/" + tag + ": " + msg); return 0; }
    public static int w(String tag, String msg) { System.err.println("W/" + tag + ": " + msg); return 0; }
    public static int e(String tag, String msg) { System.err.println("E/" + tag + ": " + msg); return 0; }
    public static int d(String tag, String msg) { System.err.println("D/" + tag + ": " + msg); return 0; }
}
