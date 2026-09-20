package com.q50gtr.plus.ui;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * Нижняя панель вкладок. Активная вкладка — подсвеченная трапеция с синим
 * градиентом и светящейся верхней кромкой, ровно как на эталоне.
 *
 * Геометрия — docs/UI-MASTER-SPEC.md, п.7. Активная вкладка динамическая.
 */
public final class OemNavigation {

    private OemNavigation() {
    }

    public static void draw(Canvas c, Theme t, Layout l, String[] titles, int active) {
        float y0 = Layout.SCREEN_H - Layout.NAV_H;
        // Фон полосы: сверху 0E141C, снизу 05080C — замер по эталону.
        c.drawRect(0f, y0, l.w, Layout.SCREEN_H, t.vertical(Theme.NAV_TOP,
                Theme.NAV_BOTTOM, y0, Layout.SCREEN_H));

        for (int i = 0; i < titles.length; i++) {
            float cx = l.tabCx(i);
            boolean on = i == active;
            if (on) {
                // На эталоне подсветка занимает всю высоту полосы, а не
                // отдельную плашку внутри неё.
                float hw = l.tabW * 0.5f;
                android.graphics.Path p = t.path;
                p.reset();
                p.moveTo(cx - hw + 6f, y0);
                p.lineTo(cx + hw - 6f, y0);
                p.lineTo(cx + hw, Layout.SCREEN_H);
                p.lineTo(cx - hw, Layout.SCREEN_H);
                p.close();
                c.drawPath(p, t.vertical(Theme.TAB_TOP, Theme.TAB_BOTTOM,
                        y0, Layout.SCREEN_H));
                c.drawRect(cx - hw + 6f, y0, cx + hw - 6f, y0 + 1.6f,
                        t.fill(Theme.TAB_EDGE));
            }
            // Базовая линия подписи на эталоне — 468 при полосе 432..480.
            c.drawText(titles[i], cx, y0 + 36f,
                    t.text(on ? Theme.WHITE : Theme.LABEL, 17f,
                            Paint.Align.CENTER, on));
        }

        float mid = y0 + Layout.NAV_H * 0.5f;
        OemStatusBar.chevron(c, t, l.navArrowLeftCx(), mid, 9f, 11f, false);
        OemStatusBar.chevron(c, t, l.navArrowRightCx(), mid, 9f, 11f, true);
    }
}
