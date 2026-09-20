package android.graphics;

public class Typeface {
    public static final int NORMAL = 0;
    public static final int BOLD = 1;

    public static final Typeface SANS_SERIF = new Typeface(false);
    public static final Typeface DEFAULT_BOLD = new Typeface(true);

    public final boolean bold;

    private Typeface(boolean bold) { this.bold = bold; }

    public static Typeface create(String family, int style) {
        return new Typeface(style == BOLD);
    }
}
