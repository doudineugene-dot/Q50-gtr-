package android.view;

import android.content.Context;
import android.graphics.Canvas;

public class View {
    private int w = 840, h = 480;

    public View(Context c) { }

    public void setBackgroundColor(int color) { }
    public void setLayoutParams(ViewGroup.LayoutParams p) { }
    public void invalidate() { }
    public boolean post(Runnable r) { return true; }
    public boolean postDelayed(Runnable r, long d) { return true; }
    public boolean removeCallbacks(Runnable r) { return true; }
    public void getWindowVisibleDisplayFrame(android.graphics.Rect r) {
        r.left = 0; r.top = 0; r.right = w; r.bottom = h;
    }
    public int getWidth() { return w; }
    public int getHeight() { return h; }
    public void setSize(int width, int height) { w = width; h = height; }
    protected void onDraw(Canvas c) { }
    protected void onDetachedFromWindow() { }
    public boolean onTouchEvent(MotionEvent e) { return false; }
}
