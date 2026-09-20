package com.q50gtr.plus.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.q50gtr.plus.data.DataHub;
import com.q50gtr.plus.data.VehicleData;
import com.q50gtr.plus.data.VehicleProbe;

import java.util.Calendar;

/**
 * Вся панель в одной View.
 *
 * Рисуется целиком Canvas в фактическом размере окна: никакого промежуточного
 * растра на весь экран и никакого canvas.scale() поверх него. Двойного
 * масштабирования нет, поэтому кромки и текст остаются резкими.
 *
 * Раскладка берётся из {@link Layout}, который строится по реальным
 * getWidth()/getHeight(), а не по выдуманным 840x480.
 */
public final class DashboardView extends View implements Runnable {

    /** 20 кадров в секунду: шина отдаёт быстрые каналы на 20 Гц. */
    private static final long FRAME_MS = 50L;
    private static final float SWIPE_COMMIT = 70f;

    private final Theme theme = new Theme();
    private final DataHub hub;
    private final Page[] pages = new Page[]{new EnginePage(), new FuelPage(), new ChassisPage()};
    private final String[] titles = new String[3];
    private final Calendar calendar = Calendar.getInstance();

    private final DisplayInfo display = new DisplayInfo();
    private Layout layout = new Layout(840f, 480f);

    /** Фиксированное время: нужно офлайн-рендеру, чтобы кадр был повторяемым. */
    private String clockOverride;

    /* Скрытый диагностический оверлей: по умолчанию выключен. */
    private VehicleProbe probe;
    private boolean diag;
    private long clockPressAtMs;

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
        Sprites.load(context);
        for (int i = 0; i < pages.length; i++) {
            titles[i] = pages[i].title();
        }
        if (context instanceof Activity) {
            display.readWindow((Activity) context);
        }
    }

    public void setClockOverride(String hhmm) {
        clockOverride = hhmm;
    }

    public void setProbe(VehicleProbe p) {
        probe = p;
    }

    public DisplayInfo getDisplayInfo() {
        return display;
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
        display.readDraw(this, canvas);

        Layout l = layout;
        if (l.w != getWidth() || l.h != getHeight()) {
            l = new Layout(getWidth(), getHeight());
            layout = l;
        }

        VehicleData d = hub.getData();
        Theme t = theme;

        canvas.drawColor(Theme.BG);

        OemStatusBar.draw(canvas, t, l, clock(),
                d.ambientTemp.hasValue() ? d.ambientTemp.text(0) : "--",
                hub.isDemoActive() ? "DEMO" : null);

        // Страницы рисуют в координатах окна, как и размечено в Layout;
        // сдвигать их ещё раз на высоту полосы было бы двойным смещением.
        int clip = canvas.save();
        canvas.clipRect(0f, l.contentY(), l.w, l.contentY() + l.contentH());
        if (dragging && dragX != 0f) {
            int neighbour = dragX < 0f ? page + 1 : page - 1;
            drawPage(canvas, d, l, page, dragX);
            if (neighbour >= 0 && neighbour < pages.length) {
                drawPage(canvas, d, l, neighbour, dragX + (dragX < 0f ? l.w : -l.w));
            }
        } else {
            drawPage(canvas, d, l, page, 0f);
        }
        canvas.restoreToCount(clip);

        OemNavigation.draw(canvas, t, l, titles, page);

        if (diag) {
            DiagOverlay.draw(canvas, t, l, hub, probe, display, System.currentTimeMillis());
        }
    }

    private void drawPage(Canvas canvas, VehicleData d, Layout l, int index, float offsetX) {
        int save = canvas.save();
        canvas.translate(offsetX, 0f);
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
        Layout l = layout;
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = x;
                downY = y;
                dragX = 0f;
                dragging = false;
                clockPressAtMs = isOnClock(l, x, y) ? System.currentTimeMillis() : 0L;
                return true;

            case MotionEvent.ACTION_MOVE: {
                float dx = x - downX;
                float dy = y - downY;
                if (!dragging && Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy)
                        && downY < l.navTop()) {
                    dragging = true;
                    clockPressAtMs = 0L;
                }
                if (dragging) {
                    dragX = clampDrag(l, dx);
                    invalidate();
                }
                return true;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                // Долгое нажатие на часы переключает диагностический оверлей.
                if (clockPressAtMs != 0L && isOnClock(l, x, y)
                        && System.currentTimeMillis() - clockPressAtMs >= 900L) {
                    clockPressAtMs = 0L;
                    diag = !diag;
                    invalidate();
                    return true;
                }
                clockPressAtMs = 0L;
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
                if (event.getAction() == MotionEvent.ACTION_UP && y >= l.navTop()) {
                    int tapped = tabAt(l, x);
                    if (tapped >= 0) {
                        setPage(tapped);
                    } else if (x < l.tabSide()) {
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

    private boolean isOnClock(Layout l, float x, float y) {
        return y < l.topH && Math.abs(x - l.clockCx()) < 60f * l.s;
    }

    private float clampDrag(Layout l, float dx) {
        if (dx < 0f && page >= pages.length - 1) {
            return dx * 0.25f;
        }
        if (dx > 0f && page <= 0) {
            return dx * 0.25f;
        }
        return dx;
    }

    private int tabAt(Layout l, float x) {
        for (int i = 0; i < pages.length; i++) {
            if (Math.abs(x - l.tabCx(i)) <= l.tabPitch() * 0.5f) {
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
