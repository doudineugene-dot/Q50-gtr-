#!/usr/bin/env python3
"""Compare the rendered UI against the approved MASTER, pixel by pixel.

    python3 tools/uispec/compare.py <render-dir> [out-dir]

Reads <render-dir>/actual-{engine,fuel,chassis}.png (840x480, drawn by the
real UI code in the MASTER frame) and writes, per screen:

    diff-<screen>.png         amplified absolute difference
    comparison-<screen>.png   MASTER | ACTUAL | DIFF

and prints MAE, RMSE and SSIM.  Metrics are reported twice: over the whole
840x480 frame, and over the MASTER's content box only (the letterbox bars
are identical black in both images and would flatter the numbers).
"""
import os, sys
import numpy as np
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
REF = os.path.join(ROOT, "reference", "ui-master")
SCREENS = [("engine", "ENGINE", 152), ("fuel", "FUEL", 163), ("chassis", "CHASSIS", 174)]
FRAME_W = 536


def rgb(path):
    return np.asarray(Image.open(path).convert("RGB")).astype(np.float64)


def gray(a):
    return 0.2126 * a[:, :, 0] + 0.7152 * a[:, :, 1] + 0.0722 * a[:, :, 2]


def box_blur(a, k=8):
    """Uniform filter via a summed-area table — no scipy needed."""
    p = np.pad(a, k, mode="edge")
    s = p.cumsum(0).cumsum(1)
    s = np.pad(s, ((1, 0), (1, 0)))
    n = 2 * k + 1
    h, w = a.shape
    out = (s[n:n + h, n:n + w] - s[0:h, n:n + w]
           - s[n:n + h, 0:w] + s[0:h, 0:w])
    return out / (n * n)


def ssim(x, y):
    """Global SSIM with an 17x17 uniform window, on luma in 0..255."""
    C1, C2 = (0.01 * 255) ** 2, (0.03 * 255) ** 2
    mx, my = box_blur(x), box_blur(y)
    sxx = box_blur(x * x) - mx * mx
    syy = box_blur(y * y) - my * my
    sxy = box_blur(x * y) - mx * my
    m = ((2 * mx * my + C1) * (2 * sxy + C2)) / \
        ((mx ** 2 + my ** 2 + C1) * (sxx + syy + C2))
    return float(m.mean())


def metrics(m, a):
    d = m - a
    return float(np.abs(d).mean()), float(np.sqrt((d ** 2).mean()))


def main():
    src = sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, "docs", "preview")
    out = sys.argv[2] if len(sys.argv) > 2 else src
    os.makedirs(out, exist_ok=True)

    rows = []
    for name, up, drift in SCREENS:
        mp = os.path.join(REF, "MASTER_%s_840x480.png" % up)
        ap = os.path.join(src, "actual-%s.png" % name)
        M, A = rgb(mp), rgb(ap)
        if M.shape != A.shape:
            raise SystemExit("size mismatch: %s vs %s" % (M.shape, A.shape))

        x0 = drift
        x1 = min(840, x0 + FRAME_W)
        full = metrics(M, A)
        content = metrics(M[:, x0:x1], A[:, x0:x1])
        s = ssim(gray(M)[:, x0:x1], gray(A)[:, x0:x1])
        rows.append((up, full, content, s))

        # diff: absolute difference, amplified 3x so small errors stay visible
        d = np.clip(np.abs(M - A) * 3.0, 0, 255).astype(np.uint8)
        Image.fromarray(d).save(os.path.join(out, "diff-%s.png" % name))

        # comparison: three panels stacked side by side with captions
        pad, cap = 8, 26
        cw, ch = 840, 480
        comp = Image.new("RGB", (cw * 3 + pad * 4, ch + cap + pad * 2), (12, 12, 14))
        g = ImageDraw.Draw(comp)
        for i, (img, label) in enumerate([(M, "MASTER"), (A, "ACTUAL"), (d, "DIFF x3")]):
            x = pad + i * (cw + pad)
            comp.paste(Image.fromarray(np.clip(img, 0, 255).astype(np.uint8)),
                       (x, cap))
            g.text((x + 6, 7), "%s — %s" % (label, up), fill=(235, 235, 240))
        g.text((pad, ch + cap + 6),
               "MAE %.2f   RMSE %.2f   SSIM %.4f   (content box x%d..%d)"
               % (content[0], content[1], s, x0, x1), fill=(160, 170, 185))
        comp.save(os.path.join(out, "comparison-%s.png" % name))

    print("%-9s %18s %18s %8s" % ("screen", "full frame", "content box", "SSIM"))
    print("%-9s %8s %8s %8s %8s %8s" % ("", "MAE", "RMSE", "MAE", "RMSE", ""))
    for up, f, c, s in rows:
        print("%-9s %8.2f %8.2f %8.2f %8.2f %8.4f" % (up, f[0], f[1], c[0], c[1], s))


if __name__ == "__main__":
    main()
