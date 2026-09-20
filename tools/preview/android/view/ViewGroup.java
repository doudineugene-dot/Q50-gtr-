package android.view;

public class ViewGroup {
    public static class LayoutParams {
        public static final int MATCH_PARENT = -1;
        public static final int WRAP_CONTENT = -2;
        public int width, height;
        public LayoutParams(int w, int h) { width = w; height = h; }
    }
}
