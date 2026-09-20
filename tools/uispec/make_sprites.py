#!/usr/bin/env python3
"""Вырезать из утверждённого эталона неподвижную декоративную графику.

Берётся только то, что Canvas-примитивами не воспроизвести убедительно:
металлическое кольцо прибора, автомобиль и стойка пневмоподвески. Шкалы,
деления, цифры, стрелки, карточки и панели по-прежнему рисуются кодом.

    python3 tools/uispec/make_sprites.py

Пишет res/drawable-nodpi/{dial_bezel,q50_rear,air_strut}.png.
"""
import os
import numpy as np
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
REF = os.path.join(ROOT, "reference", "ui-master")
OUT = os.path.join(ROOT, "res", "drawable-nodpi")

# Сдвиг кадрирования у экрана ШАССИ и левая кромка рамки (спека, п.1).
FRAME_X, DRIFT_CHASSIS = 152, 22

# Прибор на MASTER_ENGINE (спека, п.5).
DIAL_CX, DIAL_CY = 289.2, 180.8
# Кольцо: внутрь до тёмного ободка, наружу за внешнюю кромку безеля.
# Деления кончаются на r = 112, красная зона на 114 — начинаем с 115,
# чтобы в кольцо не затекла красная полоса. Снаружи режем на 132: дальше
# начинается верхняя кромка карточек.
R_IN, R_FADE_IN, R_FADE_OUT, R_OUT = 115.0, 119.0, 128.0, 132.0


def master(name):
    return Image.open(os.path.join(REF, "MASTER_%s_840x480.png" % name)).convert("RGB")


def feather(rgb, fade=14.0):
    """Свести края кропа в прозрачность: иначе виден прямоугольник."""
    a = np.asarray(rgb).astype(float)
    h, w, _ = a.shape
    yy, xx = np.mgrid[0:h, 0:w]
    d = np.minimum(np.minimum(xx, w - 1 - xx), np.minimum(yy, h - 1 - yy)).astype(float)
    alpha = np.clip(d / fade, 0.0, 1.0)
    return Image.fromarray(np.dstack([a, alpha * 255.0]).astype(np.uint8), "RGBA")


def bezel():
    """Кольцо прибора: самая дорогая на вид деталь и самая безнадёжная для
    примитивов — на эталоне это полированный металл с бликами и тенью."""
    m = np.asarray(master("ENGINE")).astype(float)
    n = int(np.ceil(R_OUT)) * 2 + 2
    x0 = int(round(DIAL_CX - n / 2.0))
    y0 = int(round(DIAL_CY - n / 2.0))
    crop = m[y0:y0 + n, x0:x0 + n]

    yy, xx = np.mgrid[0:n, 0:n]
    r = np.hypot(xx - (DIAL_CX - x0), yy - (DIAL_CY - y0))
    a = np.clip((r - R_IN) / (R_FADE_IN - R_IN), 0, 1)
    a *= np.clip((R_OUT - r) / (R_OUT - R_FADE_OUT), 0, 1)
    # Снизу кольцо на эталоне заходит под карточки, и в кроп попадает их
    # верхняя кромка. Режем по строке, с которой карточки начинаются.
    a *= np.clip((302.0 - (yy + y0)) / 4.0, 0, 1)

    img = Image.fromarray(np.dstack([crop, np.clip(a, 0, 1) * 255.0]).astype(np.uint8), "RGBA")
    img.save(os.path.join(OUT, "dial_bezel.png"))
    return "dial_bezel.png", img.size


def car():
    x = FRAME_X + DRIFT_CHASSIS
    img = feather(master("CHASSIS").crop((69 + x, 125, 267 + x, 263)), fade=26.0)
    img.save(os.path.join(OUT, "q50_rear.png"))
    return "q50_rear.png", img.size


def strut():
    x = FRAME_X + DRIFT_CHASSIS
    img = feather(master("CHASSIS").crop((31 + x, 129, 58 + x, 248)), fade=5.0)
    img.save(os.path.join(OUT, "air_strut.png"))
    return "air_strut.png", img.size


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    for fn in (bezel, car, strut):
        name, size = fn()
        print("%-18s %s" % (name, size))
