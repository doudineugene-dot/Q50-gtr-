package com.q50gtr.plus.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.q50gtr.plus.data.DataHub;
import com.q50gtr.plus.data.VehicleData;

import java.util.Calendar;

/**
 * Вся панель в одной View: фоновый растр вкладки плюс динамический слой
 * поверх него. Дочерних вью нет, onDraw один.
 *
 * Система координат — 840x480 пикселей утверждённого эталона; canvas
 * масштабируется под фактический размер вью один раз в начале onDraw.
 */
public final class DashboardView extends View implements Runnable {

    private static final long FRAME_MS = 100L;
    private static final float SWIPE_COMMIT = 70f;

    private final Theme theme = new Theme();
    private final DataHub hub;
    private final AssetRenderer assets;
    private final Page[] pages = new Page[]{new EnginePage(), new FuelPage(), new ChassisPage()};
    private final Layout[] layouts = new Layout[]{new Layout(0), new Layout(1), new Layout(2)};
    private final Calendar calendar = Calendar.getInstance();

    /** Фиксированное время: нужно офлайн-рендеру, чтобы кадр был повторяемым. */
    private String clockOverride;

    private int page;
    private float dragX;
    private boolean dragging;
    private float downX;
    private float downY;
    private boolean running;
    private final float touchSlop;

    public DashboardView(Context context, DataHub hub) {
        super(context);
        this.hub = hub;
        this.assets = new AssetRenderer(context);
        setBackgroundColor(Theme.BG);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public void setClockOverride(String hhmm) {
        clockOverride = hhmm;
    }

    public void start() {
        if (!running) {
            running = true;
            post(this);
        }
    }

    public void stop() {
        running = false;
        removeCallbacks(this);
    }

    public void run() {
        if (!running) {
            return;
        }
        hub.poll(System.currentTimeMillis());
        invalidate();
        postDelayed(this, FRAME_MS);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stop();
    }

    /* ------------------------------------------------------------------ */

    @Override
    protected void onDraw(Canvas canvas) {
        VehicleData d = hub.getData();
        Theme t = theme;

        float sx = getWidth() / Layout.SCREEN_W;
        float sy = getHeight() / Layout.SCREEN_H;

        int base = canvas.save();
        canvas.scale(sx, sy);

        t.rect.set(0f, 0f, Layout.SCREEN_W, Layout.SCREEN_H);
        canvas.drawRect(t.rect, t.fill(Theme.BG));

        if (dragging && dragX != 0f) {
            int neighbour = dragX < 0f ? page + 1 : page - 1;
            drawScreen(canvas, d, page, dragX);
            if (neighbour >= 0 && neighbour < pages.length) {
                drawScreen(canvas, d, neighbour,
                        dragX + (dragX < 0f ? Layout.SCREEN_W : -Layout.SCREEN_W));
            }
        } else {
            drawScreen(canvas, d, page, 0f);
        }

        canvas.restoreToCount(base);
    }

    private void drawScreen(Canvas canvas, VehicleData d, int index, float offsetX) {
        int save = canvas.save();
        canvas.translate(offsetX, 0f);
        assets.draw(canvas, index, 0f);
        Layout l = layouts[index];
        OemStatusBar.draw(canvas, theme, l, clock(),
                d.ambientTemp.hasValue() ? d.ambientTemp.text(0) : "--",
                hub.isDemoActive());
        pages[index].draw(canvas, theme, l, d);
        canvas.restoreToCount(save);
    }

    private String clock() {
        if (clockOverride != null) {
            return clockOverride;
        }
        calendar.setTimeInMillis(System.currentTimeMillis());
        int hh = calendar.get(Calendar.HOUR_OF_DAY);
        int mm = calendar.get(Calendar.MINUTE);
        StringBuilder sb = new StringBuilder();
        if (hh < 10) {
            sb.append('0');
        }
        sb.append(hh).append(':');
        if (mm < 10) {
            sb.append('0');
        }
        sb.append(mm);
        return sb.toString();
    }

    /* ------------------------------------------------------------------ */

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float sx = getWidth() / Layout.SCREEN_W;
        float sy = getHeight() / Layout.SCREEN_H;
        float x = event.getX() / (sx <= 0f ? 1f : sx);
        float y = event.getY() / (sy <= 0f ? 1f : sy);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = x;
                downY = y;
                dragX = 0f;
                dragging = false;
                return true;

            case MotionEvent.ACTION_MOVE: {
                float dx = x - downX;
                float dy = y - downY;
                float slop = touchSlop / (sx <= 0f ? 1f : sx);
                if (!dragging && Math.abs(dx) > slop && Math.abs(dx) > Math.abs(dy)
                        && downY < Layout.NAV_TOP) {
                    dragging = true;
                }
                if (dragging) {
                    dragX = clampDrag(dx);
                    invalidate();
                }
                return true;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (dragging) {
                    if (dragX <= -SWIPE_COMMIT && page < pages.length - 1) {
                        page++;
                    } else if (dragX >= SWIPE_COMMIT && page > 0) {
                        page--;
                    }
                    dragging = false;
                    dragX = 0f;
                    invalidate();
                    return true;
                }
                if (event.getAction() == MotionEvent.ACTION_UP && y >= Layout.NAV_TOP) {
                    int tapped = tabAt(x);
                    if (tapped >= 0) {
                        setPage(tapped);
                    } else if (x < layouts[page].tabCx(0) - layouts[page].tabHalfWidth()) {
                        setPage(page - 1);
                    } else {
                        setPage(page + 1);
                    }
                }
                return true;
            }

            default:
                return super.onTouchEvent(event);
        }
    }

    private float clampDrag(float dx) {
        if (dx < 0f && page >= pages.length - 1) {
            return dx * 0.25f;
        }
        if (dx > 0f && page <= 0) {
            return dx * 0.25f;
        }
        return dx;
    }

    private int tabAt(float x) {
        Layout l = layouts[page];
        for (int i = 0; i < pages.length; i++) {
            if (Math.abs(x - l.tabCx(i)) <= l.tabHalfWidth()) {
                return i;
            }
        }
        return -1;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int index) {
        if (index >= 0 && index < pages.length && index != page) {
            page = index;
            invalidate();
        }
    }
}
