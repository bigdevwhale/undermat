"""Copies the rendered screens (run the `-Prender` tests first) into docs/screenshots as JPEGs."""
import os
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PICK = ["01-board", "02-door", "03-naples", "04-naples-kitchen", "07-evening", "10-kyoto", "12-bergen", "18-lisbon", "08-suitcase"]


def main():
    for lang in ("en", "ru"):
        src = os.path.join(ROOT, "app", "build", "art", f"screens-{lang}")
        dst = os.path.join(ROOT, "docs", "screenshots", lang)
        os.makedirs(dst, exist_ok=True)
        for i, name in enumerate(PICK):
            im = Image.open(os.path.join(src, name + ".png")).convert("RGB")
            im = im.resize((720, round(im.height * 720 / im.width)), Image.LANCZOS)
            im.save(os.path.join(dst, f"{i}-{name[3:]}.jpg"), quality=86, optimize=True)


if __name__ == "__main__":
    main()
