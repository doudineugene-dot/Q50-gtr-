package android.graphics;

public class Paint {
    public static final int ANTI_ALIAS_FLAG = 1;
    public static final int FILTER_BITMAP_FLAG = 2;
    public static final int DITHER_FLAG = 4;

    public enum Style { FILL, STROKE, FILL_AND_STROKE }
    public enum Align { LEFT, CENTER, RIGHT }
    public enum Cap { BUTT, ROUND, SQUARE }

    private Style style = Style.FILL;
    private Align align = Align.LEFT;
    private Cap cap = Cap.BUTT;
    private int color = 0xFF000000;
    private float strokeWidth = 1f;
    private float textSize = 12f;
    private Typeface typeface = Typeface.SANS_SERIF;
    private Shader shader;
    private float textScaleX = 1f;

    public Paint() { }
    public Paint(int flags) { }

    public void setStyle(Style s) { style = s; }
    public Style getStyle() { return style; }
    public void setTextAlign(Align a) { align = a; }
    public Align getTextAlign() { return align; }
    public void setStrokeCap(Cap c) { cap = c; }
    public Cap getStrokeCap() { return cap; }
    public void setColor(int c) { color = c; }
    public int getColor() { return color; }
    public void setStrokeWidth(float w) { strokeWidth = w; }
    public float getStrokeWidth() { return strokeWidth; }
    public void setTextSize(float s) { textSize = s; }
    public float getTextSize() { return textSize; }
    public void setTypeface(Typeface t) { typeface = t; }
    public Typeface getTypeface() { return typeface; }
    public void setTextScaleX(float x) { textScaleX = x; }
    public float getTextScaleX() { return textScaleX; }
    public void setShader(Shader s) { shader = s; }
    public Shader getShader() { return shader; }

    public java.awt.Font awtFont() {
        String[] families = {"DejaVu Sans Condensed", "DejaVu Sans", "Liberation Sans", "SansSerif"};
        java.awt.GraphicsEnvironment ge =
                java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
        java.util.List<String> avail = java.util.Arrays.asList(ge.getAvailableFontFamilyNames());
        String pick = "SansSerif";
        for (String f : families) {
            if (avail.contains(f)) { pick = f; break; }
        }
        int st = (typeface != null && typeface.bold) ? java.awt.Font.BOLD : java.awt.Font.PLAIN;
        java.awt.Font f = new java.awt.Font(pick, st, 10).deriveFont(textSize);
        if (textScaleX != 1f) {
            // Android сжимает глифы по горизонтали; в Java2D это аффинный
            // трансформ шрифта с тем же коэффициентом.
            f = f.deriveFont(java.awt.geom.AffineTransform.getScaleInstance(textScaleX, 1.0));
        }
        return f;
    }

    public float measureText(String s) {
        java.awt.image.BufferedImage img =
                new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setFont(awtFont());
        float w = (float) g.getFontMetrics().getStringBounds(s, g).getWidth();
        g.dispose();
        return w;
    }
}
