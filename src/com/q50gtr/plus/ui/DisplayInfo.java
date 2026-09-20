package com.q50gtr.plus.ui;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

/**
 * Фактическая геометрия окна на этом ГУ.
 *
 * 840x480 было нашим пространством проектирования, а не доказанным размером
 * окна. Пока устройство само не скажет свои числа, любая раскладка — догадка,
 * поэтому они снимаются и пишутся в лог при старте и при первой отрисовке.
 */
public final class DisplayInfo {

    private static final String TAG = "Q50GTR/DISPLAY";

    public int displayW;
    public int displayH;
    public int metricsW;
    public int metricsH;
    public float density;
    public int densityDpi;
    public float scaledDensity;
    public float xdpi;
    public float ydpi;
    public int viewW;
    public int viewH;
    public int canvasW;
    public int canvasH;
    public int decorW;
    public int decorH;
    public int visibleW;
    public int visibleH;
    public int orientation;

    private boolean loggedDraw;

    public void readWindow(Activity a) {
        try {
            WindowManager wm = a.getWindowManager();
            Display d = wm.getDefaultDisplay();
            displayW = d.getWidth();
            displayH = d.getHeight();
            orientation = d.getOrientation();

            DisplayMetrics m = new DisplayMetrics();
            d.getMetrics(m);
            metricsW = m.widthPixels;
            metricsH = m.heightPixels;
            density = m.density;
            densityDpi = m.densityDpi;
            scaledDensity = m.scaledDensity;
            xdpi = m.xdpi;
            ydpi = m.ydpi;

            View decor = a.getWindow().getDecorView();
            if (decor != null) {
                decorW = decor.getWidth();
                decorH = decor.getHeight();
                Rect r = new Rect();
                decor.getWindowVisibleDisplayFrame(r);
                visibleW = r.width();
                visibleH = r.height();
            }
        } catch (Throwable t) {
            Log.w(TAG, "не удалось снять геометрию окна: " + t);
        }
        log("WINDOW");
    }

    /** Вызывается из onDraw: только здесь известны настоящие View и Canvas. */
    public void readDraw(View v, Canvas c) {
        viewW = v.getWidth();
        viewH = v.getHeight();
        canvasW = c.getWidth();
        canvasH = c.getHeight();
        if (!loggedDraw && viewW > 0) {
            loggedDraw = true;
            log("DRAW");
        }
    }

    private void log(String stage) {
        Log.i(TAG, stage + " PHYSICAL = " + displayW + "x" + displayH
                + "  METRICS = " + metricsW + "x" + metricsH
                + "  orientation=" + orientation);
        Log.i(TAG, stage + " DECOR = " + decorW + "x" + decorH
                + "  VISIBLE FRAME = " + visibleW + "x" + visibleH);
        Log.i(TAG, stage + " VIEW = " + viewW + "x" + viewH
                + "  CANVAS = " + canvasW + "x" + canvasH);
        Log.i(TAG, stage + " DENSITY = " + density + " dpi=" + densityDpi
                + " scaled=" + scaledDensity + " xdpi=" + xdpi + " ydpi=" + ydpi);
    }

    public void appendTo(StringBuilder b) {
        b.append("DISPLAY PHYSICAL = ").append(displayW).append('x').append(displayH).append('\n');
        b.append("METRICS          = ").append(metricsW).append('x').append(metricsH).append('\n');
        b.append("DECOR            = ").append(decorW).append('x').append(decorH).append('\n');
        b.append("VISIBLE FRAME    = ").append(visibleW).append('x').append(visibleH).append('\n');
        b.append("VIEW             = ").append(viewW).append('x').append(viewH).append('\n');
        b.append("CANVAS           = ").append(canvasW).append('x').append(canvasH).append('\n');
        b.append("DENSITY          = ").append(density).append(" dpi=").append(densityDpi)
                .append(" scaled=").append(scaledDensity)
                .append(" xdpi=").append(xdpi).append(" ydpi=").append(ydpi).append('\n');
        b.append("ORIENTATION      = ").append(orientation).append('\n');
    }

    public String summary() {
        return "DISP " + displayW + "x" + displayH
                + "  VIEW " + viewW + "x" + viewH
                + "  CANVAS " + canvasW + "x" + canvasH
                + "  d=" + density;
    }
}
