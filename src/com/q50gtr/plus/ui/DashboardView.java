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
 * Вся панель: статусная строка сверху, приборы посередине, вкладки снизу —
 * как в штатном InTouch. Одна View, один onDraw, никаких дочерних вью.
 *
 * Ширина раскладки задаётся через {@link Layout}. На устройстве это 840,
 * офлайн-рендер для сравнения с эталоном использует 548 — см.
 * docs/UI-MASTER-SPEC.md, п.10.
 */
public final class DashboardView extends View implements Runnable {

    public static final float DESIGN_H = Layout.SCREEN_H;

    private static final long FRAME_MS = 100L;
    private static final float SWIPE_COMMIT = 70f;

    private final Theme theme = new Theme();
    private final DataHub hub;
    private final Page[] pages = new Page[]{new EnginePage(), new FuelPage(), new ChassisPage()};
    private final Calendar calendar = Calendar.getInstance();
    private final String[] titles = new String[pages.length];

    private Layout layout = Layout.device();
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
        setBackgroundColor(Theme.BG);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        // Статичные растры эталона грузятся один раз, не в onDraw.
        Sprites.load(context);
        for (int i = 0; i < pages.length; i++) {
            titles[i] = pages[i].title();
        }
    }

    /** Офлайн-рендер ставит сюда рамку эталона; на устройстве всегда 840. */
    public void setLayout(Layout l) {
        layout = l;
    }

    public Layout getLayout() {
        return layout;
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
        Layout l = layout;

        float sx = getWidth() / l.w;
        float sy = getHeight() / DESIGN_H;

        int base = canvas.save();
        canvas.scale(sx, sy);

        t.rect.set(0f, 0f, l.w, DESIGN_H);
        canvas.drawRect(t.rect, t.fill(Theme.BG));

        OemStatusBar.draw(canvas, t, l, clock(),
                d.ambientTemp.hasValue() ? d.ambientTemp.text(0) : "--",
                hub.isDemoActive() ? "DEMO" : null);

        int clip = canvas.save();
        canvas.clipRect(0f, Layout.CONTENT_Y, l.w, Layout.CONTENT_Y + Layout.CONTENT_H);
        canvas.translate(0f, Layout.CONTENT_Y);
        if (dragging && dragX != 0f) {
            int neighbour = dragX < 0f ? page + 1 : page - 1;
            drawPage(canvas, d, page, dragX);
            if (neighbour >= 0 && neighbour < pages.length) {
                drawPage(canvas, d, neighbour, dragX + (dragX < 0f ? l.w : -l.w));
            }
        } else {
            drawPage(canvas, d, page, 0f);
        }
        canvas.restoreToCount(clip);

        OemNavigation.draw(canvas, t, l, titles, page);
        canvas.restoreToCount(base);
    }

    private void drawPage(Canvas canvas, VehicleData d, int index, float offsetX) {
        int save = canvas.save();
        canvas.translate(offsetX, 0f);
        pages[index].draw(canvas, theme, layout, d);
        canvas.restoreToCount(save);
    }

    public void setClockOverride(String hhmm) {
        clockOverride = hhmm;
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
        Layout l = layout;
        float sx = getWidth() / l.w;
        float sy = getHeight() / DESIGN_H;
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
                        && downY > Layout.TOP_H && downY < DESIGN_H - Layout.NAV_H) {
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
                if (event.getAction() == MotionEvent.ACTION_UP && y >= DESIGN_H - Layout.NAV_H) {
                    if (x < Layout.TAB_SIDE) {
                        setPage(page - 1);
                    } else if (x > l.w - Layout.TAB_SIDE) {
                        setPage(page + 1);
                    } else {
                        int tapped = tabAt(x);
                        if (tapped >= 0) {
                            setPage(tapped);
                        }
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
        for (int i = 0; i < pages.length; i++) {
            if (Math.abs(x - layout.tabCx(i)) <= layout.tabPitch * 0.5f) {
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
