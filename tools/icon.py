"""Draws the logo (docs/logo.png) the way the launcher icon is drawn: a doormat on a green door, and the key on it."""
import os
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
S = 4  # supersampling
N = 512 * S
GREEN, MAT, EDGE, STRAW, GOLD = (47, 90, 67), (168, 104, 58), (107, 58, 26), (138, 80, 40), (216, 178, 90)


def main():
    im = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    u = N / 108  # the vector icon's viewport is 108 units

    def P(x, y):
        return (x * u, y * u)
    d.rounded_rectangle([0, 0, N - 1, N - 1], radius=int(N * 0.22), fill=GREEN)
    d.polygon([P(30, 58), P(78, 58), P(84, 78), P(24, 78)], fill=MAT)
    d.line([P(32.5, 61), P(75.5, 61), P(80, 75), P(28, 75), P(32.5, 61)], fill=EDGE, width=int(1.2 * u))
    for a, b in [(36, 34), (42, 41), (48, 47.5), (54, 54), (60, 60.5), (66, 67), (72, 74)]:
        d.line([P(a, 61), P(b, 75)], fill=STRAW, width=int(1 * u))
    w = int(4.2 * u)
    c, r = P(37, 44), 7 * u
    d.ellipse([c[0] - r, c[1] - r, c[0] + r, c[1] + r], outline=GOLD, width=w)
    d.line([P(44, 44), P(72, 44)], fill=GOLD, width=w)
    d.line([P(64, 44), P(64, 50)], fill=GOLD, width=w)
    d.line([P(70, 44), P(70, 49)], fill=GOLD, width=w)
    out = im.resize((512, 512), Image.LANCZOS)
    os.makedirs(os.path.join(ROOT, "docs"), exist_ok=True)
    out.save(os.path.join(ROOT, "docs", "logo.png"), optimize=True)


if __name__ == "__main__":
    main()
