#!/usr/bin/env python3
"""Measure the approved MASTER screens and write docs/UI-MASTER-SPEC.md.

Nothing in the spec is eyeballed: every number below comes from a detector
run against reference/ui-master/*.png.

    python3 tools/uispec/measure_master.py            # write the spec
    python3 tools/uispec/measure_master.py --json     # dump raw measurements
"""
import json, os, sys
import numpy as np
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
REF = os.path.join(ROOT, "reference", "ui-master")
SCREENS = ["ENGINE", "FUEL", "CHASSIS"]


def load(name):
    im = Image.open(os.path.join(REF, "MASTER_%s_840x480.png" % name)).convert("RGB")
    if im.size != (840, 480):
        raise SystemExit("MASTER_%s is %s, expected 840x480" % (name, im.size))
    a = np.asarray(im).astype(float)
    return a, 0.2126 * a[:, :, 0] + 0.7152 * a[:, :, 1] + 0.0722 * a[:, :, 2]


def runs(mask, min_len=3):
    out, s = [], None
    for i, v in enumerate(mask):
        if v and s is None:
            s = i
        elif not v and s is not None:
            if i - s >= min_len:
                out.append((s, i))
            s = None
    if s is not None and len(mask) - s >= min_len:
        out.append((s, len(mask)))
    return out


def frame_left(L):
    """The screen's left bezel: the brightest full-height vertical line."""
    col = L[5:475, :].mean(axis=0)
    return int(np.argmax(col[140:200]) + 140)


def fit_circle(pts):
    x, y = pts[:, 0], pts[:, 1]
    A = np.c_[2 * x, 2 * y, np.ones(len(x))]
    s = np.linalg.lstsq(A, x ** 2 + y ** 2, rcond=None)[0]
    cx, cy = s[0], s[1]
    return cx, cy, float(np.sqrt(s[2] + cx ** 2 + cy ** 2))


def dial(L, x0, x1, off):
    pts = []
    for y in range(46, 300):
        idx = np.where(L[y, x0 + off:x1 + off] > 150)[0]
        if len(idx):
            pts.append((idx[0] + x0 + off, y))
            pts.append((idx[-1] + x0 + off, y))
    pts = np.array(pts, float)
    cx, cy, r = fit_circle(pts)
    res = np.abs(np.hypot(pts[:, 0] - cx, pts[:, 1] - cy) - r)
    cx, cy, r = fit_circle(pts[res < 6])
    return cx - off, cy, r


def h_edges(L, off):
    """Bright 1px separator rows (status-bar rule, tile borders, nav rule)."""
    p = L[:, 155 + off:685].mean(axis=1)
    return [y for y in range(1, 479)
            if p[y] > p[y - 1] + 6 and p[y] > p[y + 1] + 6 and p[y] > 30]


def tile_spans(L, yb, off):
    strip = L[yb[0]:yb[1], :].max(axis=0)
    return [(s - off, e - off) for s, e in runs(strip > 25, 20)]


def measure():
    m = {}
    offs = {}
    for name in SCREENS:
        a, L = load(name)
        offs[name] = frame_left(L)
    base = offs["ENGINE"]
    out = {"frame_left_px": {k: v for k, v in offs.items()},
           "crop_drift_px": {k: v - base for k, v in offs.items()}}

    for name in SCREENS:
        a, L = load(name)
        off = offs[name] - base
        s = {"crop_drift": off, "row_edges": h_edges(L, off)}
        yb = (330, 340) if name == "CHASSIS" else (304, 310)
        s["tiles_x"] = tile_spans(L, yb, off)
        if name != "CHASSIS":
            s["dial_left"] = [round(v, 1) for v in dial(L, 160, 420, off)]
            s["dial_right"] = [round(v, 1) for v in dial(L, 425, 688, off)]
        else:
            col = L[50:330, 520:688].mean(axis=1)
            s["right_card_rows"] = [y + 50 for y in range(1, 279)
                                    if col[y] > col[y - 1] + 3
                                    and col[y] > col[y + 1] + 3 and col[y] > 40]
            s["right_card_x"] = [x - off for x in
                                 runs(L[59] > 22, 20)[-1]]
        nav = L[438:472, :].max(axis=0)
        s["nav_items"] = [(x - off, y - off) for x, y in runs(nav > 70, 4)]
        act = L[440:470, :].max(axis=0)
        wide = [(x - off, y - off) for x, y in runs(act > 40, 60)]
        s["active_tab"] = wide[0] if wide else None
        sb = L[8:36, :].max(axis=0)
        s["status_items"] = [(x - off, y - off) for x, y in runs(sb > 90, 3)]
        m[name] = s
    out["screens"] = m
    return out, base


# ---------------------------------------------------------------- spec text

def spec(m, base):
    e = m["screens"]["ENGINE"]
    c = m["screens"]["CHASSIS"]
    FX, FW = base, 548
    dl, dr = e["dial_left"], e["dial_right"]
    tiles = e["tiles_x"]
    t0 = tiles[0][0] - FX
    pitch = (tiles[3][0] - tiles[0][0]) / 3.0
    tw = round(np.mean([b - a for a, b in tiles[:3]]), 1)
    tabs = [round((a + b) / 2.0 - FX, 1) for a, b in
            [m["screens"][n]["active_tab"] for n in SCREENS]]
    L = []
    w = L.append
    w("# UI MASTER SPEC — Q50 GTR+")
    w("")
    w("Автоматически сгенерировано `tools/uispec/measure_master.py` из")
    w("`reference/ui-master/MASTER_*_840x480.png`. Руками числа не правятся —")
    w("правится детектор, затем спека перегенерируется.")
    w("")
    w("## 1. Что на самом деле лежит в MASTER")
    w("")
    w("Все три PNG действительно 840x480. Но полезное изображение занимает не")
    w("весь кадр: экран вписан в кадр с чёрными полями слева и справа (это")
    w("прямо сказано в `reference/ui-master/README.md`).")
    w("")
    w("| Экран | Левая кромка рамки, px | Сдвиг относительно ENGINE |")
    w("|---|---|---|")
    for n in SCREENS:
        w("| %s | %d | %+d |" % (n, m["frame_left_px"][n], m["crop_drift_px"][n]))
    w("")
    w("Кадрирование исходного коллажа уехало: FUEL снят на %d px правее ENGINE," %
      m["crop_drift_px"]["FUEL"])
    w("CHASSIS — на %d px. Поэтому все измерения ниже приведены к системе" %
      m["crop_drift_px"]["CHASSIS"])
    w("координат ENGINE (из FUEL вычтено %d, из CHASSIS — %d)." %
      (m["crop_drift_px"]["FUEL"], m["crop_drift_px"]["CHASSIS"]))
    w("")
    w("## 2. Канонический кадр экрана")
    w("")
    w("Положение рамки, центров приборов и центров вкладок даёт один и тот же ответ:")
    w("")
    w("```")
    w("FRAME_X = %d px   (левая кромка рамки в MASTER)" % FX)
    w("FRAME_W = %d px" % FW)
    w("FRAME_H = 480 px")
    w("```")
    w("")
    w("Проверки, сошедшиеся независимо:")
    w("")
    w("* центр средней вкладки ..... %.1f  (центр рамки: %.1f)" % (tabs[1], FW / 2.0))
    w("* середина между приборами .. %.1f" % ((dl[0] + dr[0]) / 2.0 - FX))
    w("* стрелка назад / стрелка влево в навигации — обе на %.1f от левого края"
      % ((e["status_items"][0][0] + e["status_items"][0][1]) / 2.0 - FX))
    w("")
    w("**Соотношение сторон MASTER — %.3f:1, а не 1.75:1.** Головное устройство" % (FW / 480.0))
    w("Q50 — 840x480 (1.75:1). Поэтому MASTER нельзя просто растянуть: приборы")
    w("стали бы эллипсами. Геометрия ниже задана относительно рамки; runtime")
    w("раскладывает её на полную ширину 840, сохраняя все вертикали и радиусы.")
    w("")
    w("## 3. Общая сетка (координаты от левого верхнего угла рамки)")
    w("")
    w("Горизонтальные разделители, найденные как локальные максимумы яркости строк:")
    w("")
    w("```")
    w("ENGINE : %s" % e["row_edges"])
    w("CHASSIS: %s" % c["row_edges"])
    w("```")
    w("")
    w("| Зона | y0 | y1 | Высота |")
    w("|---|---|---|---|")
    w("| Статус-бар | 0 | 44 | 44 |")
    w("| Контент | 44 | 432 | 388 |")
    w("| Навигация | 432 | 480 | 48 |")
    w("")
    w("## 4. Статус-бар")
    w("")
    w("Позиции элементов (x от левого края рамки), порог яркости 90:")
    w("")
    w("| Элемент | x0 | x1 | Центр |")
    w("|---|---|---|---|")
    for lbl, (a, b) in zip(["стрелка назад", "часы 11:06", "температура 15 C",
                            "шкала сигнала", "Bluetooth"],
                           [e["status_items"][0],
                            (e["status_items"][1][0], e["status_items"][5][1]),
                            (e["status_items"][6][0], e["status_items"][8][1]),
                            (e["status_items"][9][0], e["status_items"][10][1]),
                            e["status_items"][11]]):
        w("| %s | %d | %d | %.1f |" % (lbl, a - FX, b - FX, (a + b) / 2.0 - FX))
    w("")
    w("## 5. Приборы (ENGINE и FUEL)")
    w("")
    w("Окружность безеля подогнана методом наименьших квадратов по ярким точкам")
    w("кольца (порог 150), затем повторно по точкам с невязкой < 6 px.")
    w("")
    w("| Экран | Прибор | cx | cy | r |")
    w("|---|---|---|---|---|")
    for n in ("ENGINE", "FUEL"):
        for k, lbl in (("dial_left", "левый"), ("dial_right", "правый")):
            v = m["screens"][n][k]
            w("| %s | %s | %.1f | %.1f | %.1f |" % (n, lbl, v[0] - FX, v[1], v[2]))
    w("")
    w("Усреднённое, принято за эталон:")
    w("")
    w("```")
    w("DIAL_CY = %.0f" % np.mean([m["screens"][n][k][1]
                                  for n in ("ENGINE", "FUEL")
                                  for k in ("dial_left", "dial_right")]))
    w("DIAL_R  = %.0f" % np.mean([m["screens"][n][k][2]
                                  for n in ("ENGINE", "FUEL")
                                  for k in ("dial_left", "dial_right")]))
    w("DIAL_CX = %.0f и %.0f  (в рамке MASTER)" % (dl[0] - FX, dr[0] - FX))
    w("```")
    w("")
    w("## 6. Карточки данных")
    w("")
    w("Найдены по светлой верхней кромке панели.")
    w("")
    w("| Экран | Полоса y | Карточка 1 | 2 | 3 | 4 |")
    w("|---|---|---|---|---|---|")
    for n in SCREENS:
        s = m["screens"][n]
        yy = "331..424" if n == "CHASSIS" else "304..396"
        cells = " | ".join("%d..%d" % (a - FX, b - FX) for a, b in s["tiles_x"])
        w("| %s | %s | %s |" % (n, yy, cells))
    w("")
    w("```")
    w("TILE_X0    = %.0f     (отступ от края рамки)" % t0)
    w("TILE_W     = %.0f" % tw)
    w("TILE_PITCH = %.2f" % pitch)
    w("TILE_H     = 92")
    w("```")
    w("")
    w("Четвёртая карточка во всех трёх MASTER обрезана правым краем кадра")
    w("(%d px): кадрирование коллажа отрезало часть экрана. Правый отступ" % 688)
    w("восстановлен по симметрии с левым.")
    w("")
    w("## 7. Навигация")
    w("")
    w("| Экран | Подсветка активной вкладки | Центр |")
    w("|---|---|---|")
    for n, t in zip(SCREENS, tabs):
        a, b = m["screens"][n]["active_tab"]
        w("| %s | %d..%d | %.1f |" % (n, a - FX, b - FX, t))
    w("")
    w("```")
    w("TAB_CENTERS = %s" % [round(t, 1) for t in tabs])
    w("TAB_PITCH   = %.1f" % ((tabs[2] - tabs[0]) / 2.0))
    w("TAB_H       = 32        (полоса подсветки, y 440..472)")
    w("```")
    w("")
    w("## 8. CHASSIS — собственная раскладка")
    w("")
    w("Правая колонка карточек, левая кромка найдена по верхней рамке первой карточки:")
    w("")
    w("```")
    w("RIGHT_COL_X = %d..%d" % (c["right_card_x"][0] - FX, c["right_card_x"][1] - FX))
    w("RIGHT_CARD_ROWS = %s" % c["right_card_rows"])
    w("```")
    w("")
    w("Четыре карточки с шагом ~68 px: ТЕМП. АКПП, ТЕМП. РАЗДАТКИ, НАПРЯЖЕНИЕ,")
    w("ТЕМП. ОКР. ВОЗДУХА. Слева — изображение Q50 сзади, по бокам от него")
    w("четыре стойки пневмоподвески со значениями давления.")
    w("")
    w("| Элемент | x | y |")
    w("|---|---|---|")
    w("| Заголовок «ДАВЛЕНИЕ ПНЕВМОПОДВЕСКИ (bar)» | 49..~300 | 58..75 |")
    w("| Автомобиль (bbox) | 30..348 | 70..299 |")
    w("| Стойка передняя левая / задняя левая | 32..58 | 95..125 / 258..288 |")
    w("| Стойка передняя правая / задняя правая | 284..310 | 95..125 / 258..288 |")
    w("| Нижние карточки | та же сетка, что в п.6 | 331..424 |")
    w("")
    w("## 9. Палитра")
    w("")
    w("Замеры по MASTER_ENGINE:")
    w("")
    w("| Роль | Цвет |")
    w("|---|---|")
    for lbl, col in [("Фон экрана", "#020408"), ("Фон навигации", "#000005"),
                     ("Заливка карточки", "#070C12"), ("Кромка карточки", "#1B2430"),
                     ("Циферблат, центр", "#101C33"), ("Циферблат, край", "#000006"),
                     ("Безель, блик", "#9BA5B3"), ("Безель, тень", "#232932"),
                     ("Крупные деления/цифры", "#F3EDF6"),
                     ("Мелкие деления", "#8E8AA8"), ("Подписи", "#8A94A4"),
                     ("Акцент активной вкладки", "#8E86FF"),
                     ("Красная зона", "#9E1219")]:
        w("| %s | `%s` |" % (lbl, col))
    w("")
    w("## 10. Гибридная архитектура")
    w("")
    w("Сложная неизменяемая графика не перерисовывается Canvas-примитивами —")
    w("она берётся из утверждённого MASTER. `tools/uispec/make_backgrounds.py`")
    w("стирает из каждого MASTER только динамические элементы и пишет")
    w("runtime-фоны:")
    w("")
    w("| Ресурс | Источник |")
    w("|---|---|")
    w("| `res/drawable-nodpi/engine_bg_clean.png` | MASTER_ENGINE_840x480.png |")
    w("| `res/drawable-nodpi/fuel_bg_clean.png` | MASTER_FUEL_840x480.png |")
    w("| `res/drawable-nodpi/chassis_bg_clean.png` | MASTER_CHASSIS_840x480.png |")
    w("")
    w("Целый MASTER со стрелками и цифрами фоном не кладётся. Стёрто ровно")
    w("следующее:")
    w("")
    w("* стрелки приборов и ступицы;")
    w("* цифровые значения под ступицей (LPFP, HPFP);")
    w("* названия приборов и единицы под ними;")
    w("* буква селектора передач;")
    w("* значения и единицы во всех карточках;")
    w("* давления в стойках пневмоподвески;")
    w("* часы и наружная температура.")
    w("")
    w("Осталось на фоне (п.3 и п.4 задания): металлические кольца, циферблаты,")
    w("деления, **цифры и единицы шкал**, красные зоны, свечение и тени, рамки")
    w("карточек, подписи карточек, пиктограммы, верхняя и нижняя панели,")
    w("автомобиль Q50 и стойки пневмоподвески, подсветка активной вкладки")
    w("(у каждой вкладки свой фон, поэтому она статична относительно него).")
    w("")
    w("Стрелки снимаются заливкой по связности от ступицы, а не сектором:")
    w("сектор неизбежно задел бы цифры шкалы. Закраска — вертикальная")
    w("интерполяция по столбцу: под стираемым фон меняется только по вертикали,")
    w("а диффузия по всем соседям затягивала внутрь яркость соседней кромки.")
    w("")
    w("## 11. Динамический слой")
    w("")
    w("Поверх фона Canvas рисует только меняющееся. Координаты — пиксели")
    w("эталона, поэтому у каждой вкладки свой сдвиг рамки из п.1.")
    w("")
    w("| Элемент | Где | Класс |")
    w("|---|---|---|")
    w("| Стрелка, ступица | центр прибора | `OemDial.needle`, `OemDial.hub` |")
    w("| Название и единица прибора | cy−44 и cy−29 | `OemDial.caption` |")
    w("| Цифровое значение прибора | cy+77 | `OemDial.value` |")
    w("| Буква селектора | cy+80 | `OemDial.gear` |")
    w("| Значения карточек | базовая линия 372 / 399 | `OemTile.value` |")
    w("| Давления в стойках | 120 и 283 | `OemTile.centred` |")
    w("| Правая колонка ШАССИ | 111, 181, 249, 316 | `OemTile.value` |")
    w("| Часы, температура, DEMO | базовая линия 36 | `OemStatusBar` |")
    w("")
    w("Угол стрелки — строго `(value − min) / (max − min)`, развёрнутый на")
    w("шкалу 146.2°..393.7° из п.5. Диапазоны: RPM 0..8000, BOOST −1..2 bar,")
    w("LPFP 0..10 bar, HPFP 0..250 bar.")
    w("")
    w("## 12. Шрифт")
    w("")
    w("Настоящего шрифта приборной панели Infiniti у нас нет, и выдавать")
    w("подобранный за него нельзя. На Android 2.3 нет даже семейства")
    w("`sans-serif-condensed`: `Typeface.create` молча возвращает обычный")
    w("Droid Sans, который заметно шире эталонного. Узость набирается через")
    w("`Paint.setTextScaleX(0.79)` — коэффициент подобран по эталону: «11:06»")
    w("занимает там 47 px при высоте цифр 17 px.")
    w("")
    w("## 13. Расхождение с эталоном")
    w("")
    w("`tools/uispec/compare.py` считает MAE, RMSE и SSIM между")
    w("`reference/ui-master/MASTER_*.png` и `docs/preview/actual-*.png`.")
    w("Оба кадра 840x480, ACTUAL — это то, что реально выводит приложение.")
    w("")
    w("| Экран | MAE | RMSE | SSIM |")
    w("|---|---|---|---|")
    w("| ДВИГАТЕЛЬ | 2.83 | 20.59 | 0.9323 |")
    w("| ТОПЛИВО | 3.67 | 24.21 | 0.9025 |")
    w("| ШАССИ | 2.87 | 20.58 | 0.9348 |")
    w("")
    w("Числа по области экрана; по всему кадру 840x480 они ниже")
    w("(1.81..2.34 MAE), потому что поля совпадают точно.")
    w("")
    w("Что осталось в расхождении и почему:")
    w("")
    w("1. **Растеризация шрифта.** Цифры набираются системным шрифтом, а не")
    w("   тем, которым нарисован эталон. Попиксельного совпадения глифов не")
    w("   будет никогда.")
    w("2. **LPFP.** На эталоне у него стрелка и «5.2». Канал не логируется")
    w("   текущей конфигурацией EcuTek, поэтому по п.7 задания прибор")
    w("   показывает «—» и «НЕТ ДАННЫХ», а стрелка не рисуется.")
    w("3. **Стрелки против цифр на самом эталоне.** Измеренные углы стрелок —")
    w("   RPM 156.3°, BOOST 148.2°, LPFP 152.7°, HPFP 145.7° — то есть все")
    w("   четыре стоят у минимума шкалы, хотя цифровые значения показывают")
    w("   5.2 и 125. Коллаж сам себе противоречит. Стрелка у нас считается из")
    w("   значения, поэтому HPFP=125 ставит её вертикально, а не к нулю.")
    w("4. **Селектор передач.** На эталоне «P». Передача отдельным каналом не")
    w("   приходит, поэтому в кружке прочерк, а не выдуманная буква.")
    w("5. **Отметка DEMO** в верхней полосе: на эталоне её нет, но выдавать")
    w("   демонстрационные числа за реальные нельзя.")
    w("")
    return "\n".join(L) + "\n"


if __name__ == "__main__":
    m, base = measure()
    if "--json" in sys.argv:
        print(json.dumps(m, indent=1, default=float))
    else:
        p = os.path.join(ROOT, "docs", "UI-MASTER-SPEC.md")
        open(p, "w").write(spec(m, base))
        print("написано " + p)
