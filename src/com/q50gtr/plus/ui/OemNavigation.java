package com.q50gtr.plus.ui;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

/**
 * Нижняя панель вкладок во всю ширину окна. Активная вкладка — подсвеченная
 * трапеция с синим градиентом и светящейся верхней кромкой, как на эталоне.
 *
 * Рисуется Canvas целиком: панель обязана тянуться на любую ширину, а растр
 * такой ширины пришлось бы масштабировать.
 */
public final class OemNavigation {

    private OemNavigation() {
    }

    public static void draw(Canvas c, Theme t, Layout l, String[] titles, int active) {
        float y0 = l.navTop();
        float s = l.s;

        c.drawRect(0f, y0, l.w, l.h,
                t.vertical(Theme.NAV_TOP, Theme.NAV_BOTTOM, y0, l.h));

        float hw = l.tabPitch() * 0.5f;
        for (int i = 0; i < titles.length; i++) {
            float cx = l.tabCx(i);
            boolean on = i == active;
            if (on) {
                // Подсветка занимает всю высоту полосы, низ шире верха.
                Path p = t.path;
                p.reset();
                p.moveTo(cx - hw + 6f * s, y0);
                p.lineTo(cx + hw - 6f * s, y0);
                p.lineTo(cx + hw, l.h);
                p.lineTo(cx - hw, l.h);
                p.close();
                c.drawPath(p, t.vertical(Theme.TAB_TOP, Theme.TAB_BOTTOM, y0, l.h));
                c.drawRect(cx - hw + 6f * s, y0, cx + hw - 6f * s, y0 + 1.6f * s,
                        t.fill(Theme.TAB_EDGE));
            }
            c.drawText(titles[i], cx, y0 + 36f * s,
                    t.text(on ? Theme.WHITE : Theme.LABEL, l.text(17f),
                            Paint.Align.CENTER, on));
        }

        float mid = y0 + l.navH * 0.5f;
        OemStatusBar.chevron(c, t, l.navArrowLeftCx(), mid, 9f * s, 11f * s, false);
        OemStatusBar.chevron(c, t, l.navArrowRightCx(), mid, 9f * s, 11f * s, true);
    }
}
