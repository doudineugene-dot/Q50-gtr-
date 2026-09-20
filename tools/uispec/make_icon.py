#!/usr/bin/env python3
"""Собрать ярлык приложения для рабочего стола головного устройства.

Дизайн утверждён вместе с остальным интерфейсом: чёрная скруглённая плитка,
лавандовая подсветка по краю, фирменный знак по центру. Оригинал — плитка
Q50 GTR+ в меню APPS на reference/ui-master/ORIGINAL_APPROVED_MASTER.png.

Плитка и свечение рисуются под каждую плотность, а не пересэмплируются:
исходник в коллаже 134x130, и тянуть из него края было бы мылом. Знак берётся
из эталона и только уменьшается.

    python3 tools/uispec/make_icon.py

Пишет res/drawable-{ldpi,mdpi,hdpi,xhdpi}/ic_launcher.png.
"""
import os
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SRC = os.path.join(ROOT, "reference", "ui-master", "ORIGINAL_APPROVED_MASTER.png")

# Знак внутри плитки, координаты в исходном коллаже.
LOGO_BOX = (863, 781, 933, 815)

# Плотности Android. ГУ — hdpi (240 dpi), остальные на случай другой прошивки.
SIZES = {"ldpi": 36, "mdpi": 48, "hdpi": 72, "xhdpi": 96}

TILE = (5, 7, 12)          # заливка плитки
GLOW = (150, 140, 255)     # лавандовая подсветка
EDGE = (205, 200, 255)     # кромка


def logo(size):
    im = Image.open(SRC).convert("RGB").crop(LOGO_BOX)
    a = np.asarray(im).astype(np.float32)
    # Знак светлый на почти чёрном: яркость и есть его маска, так края
    # остаются мягкими и не появляется ореола от прямоугольного кропа.
    lum = 0.2126 * a[:, :, 0] + 0.7152 * a[:, :, 1] + 0.0722 * a[:, :, 2]
    alpha = np.clip((lum - 18.0) / 90.0, 0.0, 1.0)
    rgba = np.dstack([a, alpha * 255.0]).astype(np.uint8)
    out = Image.fromarray(rgba, "RGBA")
    w = int(round(size * 0.62))
    h = max(1, int(round(w * (LOGO_BOX[3] - LOGO_BOX[1]) / float(LOGO_BOX[2] - LOGO_BOX[0]))))
    return out.resize((w, h), Image.LANCZOS)


def tile(size):
    # Рисуем в четыре раза крупнее и уменьшаем: скругления и кромка выходят
    # гладкими без отдельного сглаживания.
    k = 4
    n = size * k
    pad = int(n * 0.045)
    r = int(n * 0.20)

    glow = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    ImageDraw.Draw(glow).rounded_rectangle(
        [pad, pad, n - pad, n - pad], radius=r, outline=GLOW + (255,), width=int(n * 0.035))
    glow = glow.filter(ImageFilter.GaussianBlur(n * 0.030))

    im = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.rounded_rectangle([pad, pad, n - pad, n - pad], radius=r, fill=TILE + (255,))
    im = Image.alpha_composite(im, glow)
    d = ImageDraw.Draw(im)
    d.rounded_rectangle([pad, pad, n - pad, n - pad], radius=r,
                        outline=EDGE + (255,), width=max(1, int(n * 0.012)))
    return im.resize((size, size), Image.LANCZOS)


def build(size):
    im = tile(size)
    mark = logo(size)
    im.alpha_composite(mark, ((size - mark.width) // 2,
                              int(size * 0.50) - mark.height // 2))
    return im


if __name__ == "__main__":
    for density, size in SIZES.items():
        d = os.path.join(ROOT, "res", "drawable-" + density)
        os.makedirs(d, exist_ok=True)
        p = os.path.join(d, "ic_launcher.png")
        build(size).save(p)
        print("%-24s %dx%d" % (os.path.relpath(p, ROOT), size, size))
