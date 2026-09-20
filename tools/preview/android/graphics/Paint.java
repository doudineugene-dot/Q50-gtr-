package android.graphics;

public class Paint {
    public static final int ANTI_ALIAS_FLAG = 1;

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
        return new java.awt.Font(pick, st, 10).deriveFont(textSize);
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
