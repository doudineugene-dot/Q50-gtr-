package android.view;

public class WindowManager {
    public static class LayoutParams {
        public static final int FLAG_KEEP_SCREEN_ON = 128;
        public static final int FLAG_FULLSCREEN = 1024;
    }

    public Display getDefaultDisplay() { return new Display(); }
}
