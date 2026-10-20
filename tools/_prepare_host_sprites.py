"""Copy whole pose sprites and crop mouth visemes. Do not slice the character."""
from __future__ import annotations

import os
import shutil

from PIL import Image

POSES = r"C:\Users\akhin\Downloads\doctor_character_sprites"
SHEETS = r"C:\Users\akhin\Downloads\doctor_puppet_assets\doctor_puppet_assets"
DEST = r"C:\Users\akhin\Documents\Pet\Dr.Quizman\tv\src\main\res\drawable-nodpi"
ALPHA = 40


def column_runs(im: Image.Image) -> list[tuple[int, int]]:
    w, h = im.size
    px = im.load()
    on = [any(px[x, y][3] > ALPHA for y in range(0, h, 2)) for x in range(w)]
    runs, start = [], None
    for i, v in enumerate(on):
        if v and start is None:
            start = i
        elif not v and start is not None:
            runs.append((start, i - 1))
            start = None
    if start is not None:
        runs.append((start, w - 1))
    return runs


def crop_opaque(im: Image.Image, x0: int, x1: int) -> Image.Image:
    w, h = im.size
    px = im.load()
    ys = [y for y in range(h) for x in range(x0, x1 + 1) if px[x, y][3] > ALPHA]
    return im.crop((x0, min(ys), x1 + 1, max(ys) + 1))


def main() -> None:
    os.makedirs(DEST, exist_ok=True)
    for src_name, dest_name in [
        ("IDLE.png", "host_idle.png"),
        ("TALK.png", "host_talk.png"),
        ("HAPPY.png", "host_happy.png"),
        ("SAD.png", "host_sad.png"),
        ("HYPE.png", "host_hype.png"),
    ]:
        shutil.copyfile(os.path.join(POSES, src_name), os.path.join(DEST, dest_name))
        print(" copied", dest_name)

    sheet = Image.open(os.path.join(SHEETS, "mouth", "mouth_sprite_sheet.png")).convert("RGBA")
    for i, (a, b) in enumerate(column_runs(sheet)):
        m = crop_opaque(sheet, a, b)
        path = os.path.join(DEST, f"host_mouth_{i}.png")
        m.save(path)
        print(" ", os.path.basename(path), m.size)


if __name__ == "__main__":
    main()
