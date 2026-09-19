package com.q50gtr.plus.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.q50gtr.plus.data.Channel;
import com.q50gtr.plus.data.DataHub;
import com.q50gtr.plus.data.VehicleData;

/**
 * The whole instrument panel: one View, one onDraw, no layout inflation and no
 * child views. Everything is drawn in a fixed 840x480 design space that is
 * scaled to whatever the head unit actually gives us.
 *
 * Navigation is a tap on a tab or a horizontal swipe across the instruments.
 */
public final class DashboardView extends View implements Runnable {

    /** The screen this dashboard is drawn for. */
    public static final float DESIGN_W = 840f;
    public static final float DESIGN_H = 480f;

    private static final float HEADER_H = 52f;
    private static final float FOOTER_H = 46f;
    private static final float CONTENT_H = DESIGN_H - HEADER_H - FOOTER_H;

    private static final float TAB_W = 140f;
    private static final float TAB_H = 32f;
    private static final float TAB_Y = 10f;
    private static final float TAB_X0 = 300f;
    private static final float TAB_GAP = 10f;

    /** 10 Hz is plenty for a gauge and kind to an old SoC. */
    private static final long FRAME_MS = 100L;

    /** How far a drag must travel before it flips the page. */
    private static final float SWIPE_COMMIT = 70f;

    private final Theme theme = new Theme();
    private final DataHub hub;
    private final Page[] pages = new Page[]{new EnginePage(), new FuelPage(), new ChassisPage()};

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
    }

    /* ------------------------------------------------------------------ */
    /* lifecycle                                                           */
    /* ------------------------------------------------------------------ */

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
    /* drawing                                                             */
    /* ------------------------------------------------------------------ */

    @Override
    protected void onDraw(Canvas canvas) {
        VehicleData d = hub.getData();

        float sx = getWidth() / DESIGN_W;
        float sy = getHeight() / DESIGN_H;

        int base = canvas.save();
        canvas.scale(sx, sy);

        canvas.drawColor(Theme.BG);
        drawHeader(canvas);

        int clip = canvas.save();
        canvas.clipRect(0f, HEADER_H, DESIGN_W, HEADER_H + CONTENT_H);
        canvas.translate(0f, HEADER_H);

        if (dragging && dragX != 0f) {
            int neighbour = dragX < 0f ? page + 1 : page - 1;
            drawPage(canvas, d, page, dragX);
            if (neighbour >= 0 && neighbour < pages.length) {
                drawPage(canvas, d, neighbour, dragX + (dragX < 0f ? DESIGN_W : -DESIGN_W));
            }
        } else {
            drawPage(canvas, d, page, 0f);
        }
        canvas.restoreToCount(clip);

        drawFooter(canvas, d);
        canvas.restoreToCount(base);
    }

    private void drawPage(Canvas canvas, VehicleData d, int index, float offsetX) {
        int save = canvas.save();
        canvas.translate(offsetX, 0f);
        pages[index].draw(canvas, theme, d, DESIGN_W, CONTENT_H);
        canvas.restoreToCount(save);
    }

    private void drawHeader(Canvas c) {
        Theme t = theme;
        t.rect.set(0f, 0f, DESIGN_W, HEADER_H);
        c.drawRect(t.rect, t.fill(Theme.PANEL));
        c.drawLine(0f, HEADER_H - 0.5f, DESIGN_W, HEADER_H - 0.5f, t.stroke(Theme.EDGE, 1f));

        Paint p = t.text(Theme.WHITE, 26f, Paint.Align.LEFT, true);
        float titleW = p.measureText("Q50 GTR");
        c.drawText("Q50 GTR", 18f, 35f, p);
        c.drawText("+", 18f + titleW + 2f, 35f, t.text(Theme.ACCENT, 26f, Paint.Align.LEFT, true));
        c.drawText("v0.6", 18f + titleW + 22f, 35f,
                t.text(Theme.DIM, 12f, Paint.Align.LEFT, false));

        for (int i = 0; i < pages.length; i++) {
            drawTab(c, i, pages[i].title(), i == page);
        }

        String source = hub.getSourceLabel();
        c.drawText(source, DESIGN_W - 18f, 30f,
                t.text(hub.isDemoActive() ? Theme.ACCENT : Theme.WHITE, 13f, Paint.Align.RIGHT, true));
        c.drawText(hub.isDemoActive() ? "нет связи" : "link", DESIGN_W - 18f, 43f,
                t.text(Theme.DIM, 10f, Paint.Align.RIGHT, false));
    }

    private void drawTab(Canvas c, int index, String label, boolean active) {
        Theme t = theme;
        float x = tabX(index);
        t.rect.set(x, TAB_Y, x + TAB_W, TAB_Y + TAB_H);
        if (active) {
            c.drawRect(t.rect, t.fill(Theme.PANEL_TOP));
            c.drawLine(x + 6f, TAB_Y + TAB_H - 1f, x + TAB_W - 6f, TAB_Y + TAB_H - 1f,
                    t.stroke(Theme.ACCENT, 2f));
        }
        c.drawText(label, x + TAB_W / 2f, TAB_Y + 21f,
                t.text(active ? Theme.WHITE : Theme.DIM, 15f, Paint.Align.CENTER, active));
    }

    private static float tabX(int index) {
        return TAB_X0 + index * (TAB_W + TAB_GAP);
    }

    private void drawFooter(Canvas c, VehicleData d) {
        Theme t = theme;
        float top = DESIGN_H - FOOTER_H;
        t.rect.set(0f, top, DESIGN_W, DESIGN_H);
        c.drawRect(t.rect, t.fill(Theme.PANEL));
        c.drawLine(0f, top + 0.5f, DESIGN_W, top + 0.5f, t.stroke(Theme.EDGE, 1f));

        float cell = DESIGN_W / 4f;
        for (int i = 1; i < 4; i++) {
            c.drawLine(cell * i, top + 8f, cell * i, DESIGN_H - 8f, t.stroke(Theme.EDGE, 1f));
        }

        Channel knock = d.maxKnockIndexChannel();
        footerCell(c, 0, cell, top, "IGN TIMING", d.ignitionTiming.text(1), "°",
                d.ignitionTiming.isDemo(), Theme.WHITE);
        footerCell(c, 1, cell, top, "KNOCK IDX MAX",
                knock == null ? "—" : knock.text(0),
                knock == null ? "" : knock.label,
                knock != null && knock.isDemo(),
                knock != null && knock.getValue() >= 40f ? Theme.ALERT : Theme.WHITE);
        footerCell(c, 2, cell, top, "THROTTLE", d.throttle.text(0), "%",
                d.throttle.isDemo(), Theme.WHITE);
        footerCell(c, 3, cell, top, "SPEED", d.speed.text(0), "km/h",
                d.speed.isDemo(), Theme.WHITE);
    }

    private void footerCell(Canvas c, int index, float cell, float top,
                            String label, String value, String unit, boolean demo, int color) {
        Theme t = theme;
        float x = cell * index + 16f;
        c.drawText(label, x, top + 19f, t.text(Theme.DIM, 11f, Paint.Align.LEFT, false));

        Paint p = t.text(color, 24f, Paint.Align.LEFT, true);
        float vw = p.measureText(value);
        c.drawText(value, x, top + 39f, p);
        if (unit.length() > 0) {
            c.drawText(unit, x + vw + 5f, top + 39f,
                    t.text(Theme.DIM, 12f, Paint.Align.LEFT, false));
        }
        if (demo) {
            Gauges.demoBadge(c, t, cell * (index + 1) - 12f, top + 19f);
        }
    }

    /* ------------------------------------------------------------------ */
    /* touch                                                               */
    /* ------------------------------------------------------------------ */

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float sx = getWidth() / DESIGN_W;
        float sy = getHeight() / DESIGN_H;
        float x = event.getX() / (sx == 0f ? 1f : sx);
        float y = event.getY() / (sy == 0f ? 1f : sy);

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
                if (!dragging && Math.abs(dx) > touchSlop / sxSafe(sx)
                        && Math.abs(dx) > Math.abs(dy) && downY > HEADER_H) {
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
                if (event.getAction() == MotionEvent.ACTION_UP && y <= HEADER_H) {
                    int tapped = tabAt(x, y);
                    if (tapped >= 0 && tapped != page) {
                        page = tapped;
                        invalidate();
                    }
                }
                return true;
            }

            default:
                return super.onTouchEvent(event);
        }
    }

    private static float sxSafe(float sx) {
        return sx <= 0f ? 1f : sx;
    }

    /** Stops the drag running past the first or last page. */
    private float clampDrag(float dx) {
        if (dx < 0f && page >= pages.length - 1) {
            return dx * 0.25f;
        }
        if (dx > 0f && page <= 0) {
            return dx * 0.25f;
        }
        return dx;
    }

    private int tabAt(float x, float y) {
        if (y < TAB_Y - 6f || y > TAB_Y + TAB_H + 6f) {
            return -1;
        }
        for (int i = 0; i < pages.length; i++) {
            float tx = tabX(i);
            if (x >= tx && x <= tx + TAB_W) {
                return i;
            }
        }
        return -1;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int index) {
        if (index >= 0 && index < pages.length) {
            page = index;
            invalidate();
        }
    }
}
