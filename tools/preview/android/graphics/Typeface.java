package android.graphics;

public class Typeface {
    public static final int NORMAL = 0;
    public static final int BOLD = 1;

    public static final Typeface SANS_SERIF = new Typeface(false);
    public static final Typeface DEFAULT_BOLD = new Typeface(true);

    public final boolean bold;
    /** Реальный шрифт из assets, если он загружен. */
    public final java.awt.Font awt;

    private Typeface(boolean bold) { this(bold, null); }

    private Typeface(boolean bold, java.awt.Font awt) {
        this.bold = bold;
        this.awt = awt;
    }

    public static Typeface create(String family, int style) {
        return new Typeface(style == BOLD);
    }

    public static Typeface createFromAsset(android.content.res.AssetManager am, String path) {
        try {
            java.io.InputStream in = am.open(path);
            try {
                java.awt.Font f = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, in);
                return new Typeface(false, f);
            } finally {
                in.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("native typeface cannot be made: " + path, e);
        }
    }
}
