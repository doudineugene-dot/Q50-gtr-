package android.graphics;

/** Минимальная подмена android.graphics.Rect для офлайн-рендера. */
public final class Rect {
    public int left, top, right, bottom;

    public Rect() { }

    public Rect(int l, int t, int r, int b) {
        left = l; top = t; right = r; bottom = b;
    }

    public int width() { return right - left; }
    public int height() { return bottom - top; }
}
