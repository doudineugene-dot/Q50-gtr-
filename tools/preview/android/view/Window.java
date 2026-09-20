package android.view;

public class Window {
    public static final int FEATURE_NO_TITLE = 1;

    public static class LayoutParams {
        public static final int FLAG_KEEP_SCREEN_ON = 128;
        public static final int FLAG_FULLSCREEN = 1024;
        public static final int MATCH_PARENT = -1;
    }

    public void addFlags(int f) { }
    public void clearFlags(int f) { }
    public View getDecorView() { return null; }
}
