#!/usr/bin/env python3
"""Cut the static decorative sprites out of the approved MASTER screens.

Only fixed decoration is taken — the metal dial bezel, the Q50 rear view and
the air-strut assembly.  Needles, digits, scale labels, captions and the
active tab stay in code, and no full MASTER screen is ever used as a runtime
background.

    python3 tools/uispec/extract_sprites.py

Writes res/drawable-nodpi/*.png.  Re-run it if the MASTER files change.
"""
import os
import numpy as np
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
REF = os.path.join(ROOT, "reference", "ui-master")
OUT = os.path.join(ROOT, "res", "drawable-nodpi")

FRAME_X = 152
DRIFT_CHASSIS = 22

# Прибор на MASTER_ENGINE (спека, п.5), в координатах кадра.
DIAL_CX, DIAL_CY = 289.2, 180.8
# Кольцо: внутрь от тёмного кольца, наружу за безель. Ticks начинаются
# на r = 112, поэтому внутренняя граница — 113.
R_IN, R_FADE_IN, R_FADE_OUT, R_OUT = 112.0, 117.0, 130.0, 135.0


def master(name):
    return Image.open(os.path.join(REF, "MASTER_%s_840x480.png" % name)).convert("RGB")


def bezel():
    m = np.asarray(master("ENGINE")).astype(np.uint8)
    n = int(np.ceil(R_OUT)) * 2 + 2
    cx0, cy0 = DIAL_CX - n / 2.0, DIAL_CY - n / 2.0
    x0, y0 = int(round(cx0)), int(round(cy0))
    crop = m[y0:y0 + n, x0:x0 + n].astype(np.float64)

    yy, xx = np.mgrid[0:n, 0:n]
    r = np.hypot(xx - (DIAL_CX - x0), yy - (DIAL_CY - y0))

    a = np.zeros((n, n))
    a += np.clip((r - R_IN) / (R_FADE_IN - R_IN), 0, 1)
    a *= np.clip((R_OUT - r) / (R_OUT - R_FADE_OUT), 0, 1)
    a = np.clip(a, 0, 1)

    rgba = np.dstack([crop, a * 255.0]).astype(np.uint8)
    img = Image.fromarray(rgba, "RGBA")
    img.save(os.path.join(OUT, "dial_bezel.png"))
    return "dial_bezel.png", img.size


def car():
    m = master("CHASSIS")
    x = FRAME_X + DRIFT_CHASSIS
    img = m.crop((69 + x, 125, 267 + x, 263))
    img.save(os.path.join(OUT, "q50_rear.png"))
    return "q50_rear.png", img.size


def strut():
    m = master("CHASSIS")
    x = FRAME_X + DRIFT_CHASSIS
    img = m.crop((31 + x, 129, 58 + x, 248))
    img.save(os.path.join(OUT, "air_strut.png"))
    return "air_strut.png", img.size


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    for fn in (bezel, car, strut):
        name, size = fn()
        print("%-18s %s" % (name, size))
