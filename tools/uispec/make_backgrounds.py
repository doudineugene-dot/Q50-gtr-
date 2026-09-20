#!/usr/bin/env python3
"""Собрать runtime-фоны из утверждённого MASTER.

Из каждого MASTER_*.png стираются ТОЛЬКО динамические элементы — стрелки,
ступица, цифровые значения приборов, показания карточек, часы, наружная
температура, буква селектора передач и давления в стойках. Всё остальное
(металлические кольца, циферблаты, деления, ЦИФРЫ ШКАЛ, названия приборов,
единицы у шкал, красные зоны, рамки и подписи карточек, иконки, верхняя и
нижняя панели, автомобиль, стойки) остаётся частью фона.

    python3 tools/uispec/make_backgrounds.py [--debug]

Пишет res/drawable-nodpi/{engine,fuel,chassis}_bg_clean.png и, с --debug,
docs/preview/mask-*.png с подсветкой стираемых зон.
"""
import os, sys
import numpy as np
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
REF = os.path.join(ROOT, "reference", "ui-master")
OUT = os.path.join(ROOT, "res", "drawable-nodpi")
DBG = os.path.join(ROOT, "docs", "preview")

# Левая кромка рамки экрана внутри кадра 840x480 (docs/UI-MASTER-SPEC.md, п.1).
FRAME_X = {"ENGINE": 152, "FUEL": 163, "CHASSIS": 174}
# Приборы: центр и внешний радиус безеля, в координатах рамки.
DIAL_CX = (137.0, 409.0)
DIAL_CY = 181.0
DIAL_R = 128.0

TILE_X0, TILE_W, TILE_PITCH = 9.0, 128.0, 134.0


def load(name):
    p = os.path.join(REF, "MASTER_%s_840x480.png" % name)
    im = Image.open(p).convert("RGB")
    if im.size != (840, 480):
        raise SystemExit("%s: %s, ожидалось 840x480" % (p, im.size))
    return np.asarray(im).astype(np.float64)


def luma(a):
    return 0.2126 * a[:, :, 0] + 0.7152 * a[:, :, 1] + 0.0722 * a[:, :, 2]


def needle_mask(L, cx, cy, r, seed=None):
    """Стрелка и ступица — связная яркая область, выходящая из центра.

    Заливка по связности, а не сектор: сектор неизбежно задел бы цифры шкалы,
    а они по условию п.4 должны остаться на фоне. Цифры шкалы со стрелкой не
    соприкасаются, поэтому заливка до них не доходит. Название прибора она,
    наоборот, захватывает — на LPFP через него проходит вторая линия, — и
    поэтому названия и единицы рисует код.
    """
    h, w = L.shape
    yy, xx = np.mgrid[0:h, 0:w]
    rr = np.hypot(xx - cx, yy - cy)
    field = (rr <= r * 0.86) & (L > 50)
    start = field & (rr <= r * 0.26)
    if seed is not None:
        start |= seed & (rr <= r * 0.86)
    grown = start.copy()
    for _ in range(400):
        nxt = grown.copy()
        for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1),
                       (1, 1), (1, -1), (-1, 1), (-1, -1)):
            nxt |= np.roll(grown, (dy, dx), (0, 1))
        nxt &= field
        if nxt.sum() == grown.sum():
            break
        grown = nxt
    # Небольшое расширение: у стрелки мягкие края со сглаживанием.
    for _ in range(2):
        g = grown.copy()
        for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            g |= np.roll(grown, (dy, dx), (0, 1))
        grown = g & (rr <= r * 0.88)
    return grown


def box(mask, x0, y0, x1, y1):
    mask[int(y0):int(y1), int(x0):int(x1)] = True


def disc(mask, cx, cy, r0, r1, a0=None, a1=None):
    h, w = mask.shape
    yy, xx = np.mgrid[0:h, 0:w]
    rr = np.hypot(xx - cx, yy - cy)
    sel = (rr >= r0) & (rr <= r1)
    if a0 is not None:
        ang = (np.degrees(np.arctan2(yy - cy, xx - cx)) + 360) % 360
        lo, hi = a0 % 360, a1 % 360
        sel &= (ang >= lo) & (ang <= hi) if lo <= hi else ((ang >= lo) | (ang <= hi))
    mask |= sel


def stray_rays(L, cx, cy, r, done):
    """Оставшиеся после заливки стрелки.

    Стрелка — это длинная линия: она яркая и у ступицы, и в середине, и у
    шкалы. Цифры шкалы, кружок селектора и цифровое значение яркие только в
    своём кольце, поэтому требование «ярко во всех трёх кольцах сразу»
    оставляет именно стрелки.
    """
    bands = ((0.30, 0.40), (0.50, 0.58), (0.66, 0.74))
    prof = np.zeros((len(bands), 720))
    for bi, (f0, f1) in enumerate(bands):
        for i in range(720):
            a = np.radians(i * 0.5)
            v = []
            for f in np.arange(f0, f1 + 1e-6, 0.01):
                y = int(round(cy + np.sin(a) * r * f))
                x = int(round(cx + np.cos(a) * r * f))
                v.append(0.0 if done[y, x] else L[y, x])
            prof[bi, i] = max(v)
    hit = (prof > 80).all(axis=0)
    out, group = [], []
    for i in range(720):
        if hit[i]:
            group.append(i)
        elif group:
            out.append(np.mean(group) * 0.5)
            group = []
    if group:
        out.append(np.mean(group) * 0.5)
    return out


def ray_strip(mask, cx, cy, r, ang, half=2.2):
    h, w = mask.shape
    yy, xx = np.mgrid[0:h, 0:w]
    dx, dy = xx - cx, yy - cy
    rr = np.hypot(dx, dy)
    ca, sa = np.cos(np.radians(ang)), np.sin(np.radians(ang))
    along = dx * ca + dy * sa
    perp = np.abs(-dx * sa + dy * ca)
    mask |= (perp <= half) & (along > 0) & (rr <= r * 0.86)


def dial_mask(mask, L, fx, index, value_box):
    cx, cy = fx + DIAL_CX[index], DIAL_CY
    r = DIAL_R
    # Сначала известные прямоугольники, потом заливка от них и от ступицы:
    # линия, разорванная стёртой подписью, так дотягивается до конца.
    seed = np.zeros_like(mask)
    disc(seed, cx, cy, 0, r * 0.145)
    box(seed, cx - r * 0.27, cy - r * 0.47, cx + r * 0.27, cy - r * 0.185)
    if value_box:
        box(seed, cx - r * 0.46, cy + r * 0.28, cx + r * 0.46, cy + r * 0.68)
    mask |= seed | needle_mask(L, cx, cy, r, seed)


def tiles_mask(mask, fx, y0, y1):
    for i in range(4):
        x = fx + TILE_X0 + i * TILE_PITCH
        box(mask, x + 50, y0, x + TILE_W - 4, y1)


def status_mask(mask, fx):
    box(mask, fx + 198, 11, fx + 264, 42)    # часы
    box(mask, fx + 318, 11, fx + 378, 42)    # наружная температура


def build_mask(name, a, L):
    fx = FRAME_X[name]
    m = np.zeros(a.shape[:2], dtype=bool)
    status_mask(m, fx)
    if name in ("ENGINE", "FUEL"):
        # Цифровое значение под ступицей есть только на ТОПЛИВЕ.
        dial_mask(m, L, fx, 0, name == "FUEL")
        dial_mask(m, L, fx, 1, name == "FUEL")
        tiles_mask(m, fx, 344, 380)
        if name == "ENGINE":
            # Буква селектора передач внутри декоративного кружка.
            box(m, fx + DIAL_CX[0] - 15, DIAL_CY + 48, fx + DIAL_CX[0] + 15, DIAL_CY + 80)
    else:
        tiles_mask(m, fx, 372, 408)
        for y0 in (88, 158, 226, 293):       # значения правой колонки
            box(m, fx + 412, y0, fx + 524, y0 + 28)
        for sx in (fx + 31, fx + 283):       # давления в стойках
            box(m, sx - 6, 100, sx + 34, 126)
            box(m, sx - 6, 262, sx + 34, 289)
    return m


def inpaint(a, mask):
    """Закраска вертикальной интерполяцией.

    Под всем, что стирается, фон меняется только по вертикали: заливка
    карточки — вертикальный градиент, циферблат — почти плоский. Диффузия по
    всем соседям тянула бы внутрь яркость соседней кромки и оставляла клинья,
    поэтому каждый закрашиваемый пиксель берётся из ближайших известных
    пикселей своего столбца — сверху и снизу.
    """
    h, w, _ = a.shape
    out = a.copy()

    idx = np.arange(h)[:, None].repeat(w, axis=1)
    known = ~mask

    # Ближайший известный пиксель сверху.
    up_i = np.where(known, idx, -1)
    up_i = np.maximum.accumulate(up_i, axis=0)
    # Ближайший известный пиксель снизу.
    dn_i = np.where(known, idx, h)
    dn_i = np.minimum.accumulate(dn_i[::-1], axis=0)[::-1]

    cols = np.arange(w)[None, :].repeat(h, axis=0)
    up_ok, dn_ok = up_i >= 0, dn_i < h
    up_v = out[np.clip(up_i, 0, h - 1), cols]
    dn_v = out[np.clip(dn_i, 0, h - 1), cols]

    span = (dn_i - up_i).astype(np.float64)
    span[span == 0] = 1.0
    wgt = ((idx - up_i) / span)[:, :, None]
    blend = up_v * (1.0 - wgt) + dn_v * wgt
    blend = np.where(up_ok[:, :, None] & dn_ok[:, :, None], blend,
                     np.where(up_ok[:, :, None], up_v, dn_v))
    out[mask] = blend[mask]

    # Лёгкое сглаживание шва.
    for _ in range(2):
        acc = np.zeros_like(out)
        for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1), (0, 0)):
            acc += np.roll(out, (dy, dx), (0, 1))
        out[mask] = (acc / 5.0)[mask]
    return out


def main():
    os.makedirs(OUT, exist_ok=True)
    debug = "--debug" in sys.argv
    if debug:
        os.makedirs(DBG, exist_ok=True)
    for name, fn in (("ENGINE", "engine"), ("FUEL", "fuel"), ("CHASSIS", "chassis")):
        a = load(name)
        m = build_mask(name, a, luma(a))
        clean = np.clip(inpaint(a, m), 0, 255).astype(np.uint8)
        p = os.path.join(OUT, "%s_bg_clean.png" % fn)
        Image.fromarray(clean).save(p)
        print("%-24s стёрто %d px" % (os.path.relpath(p, ROOT), int(m.sum())))
        if debug:
            ov = a.copy()
            ov[m] = ov[m] * 0.25 + np.array([255, 40, 40]) * 0.75
            Image.fromarray(np.clip(ov, 0, 255).astype(np.uint8)).save(
                os.path.join(DBG, "mask-%s.png" % fn))


if __name__ == "__main__":
    main()
