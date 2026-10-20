"""Bake puppet layers on one canvas.

Head/hair share a 1254 canvas (1:1). Eyes are placed so each
iris/sclera sits in the matching white socket. Head is scaled
down so it no longer dwarfs the body.
"""
from __future__ import annotations

import os

from PIL import Image

SRC = r"C:\Users\akhin\Downloads\doctor_puppet_assets\doctor_puppet_assets"
DEST = r"C:\Users\akhin\Documents\Pet\Dr.Quizman\tv\src\main\res\drawable-nodpi"
PREVIEW = r"C:\Users\akhin\Documents\Pet\Dr.Quizman\preview"

HEAD_SCALE = 0.38
HEAD_CUT_Y = 1038
NECK_CX, NECK_CY = 664.5, 1020.0
BODY_CX, BODY_INSERT = 518.5, 58
MOUTH_CENTER = (522, 748)
MOUTH_W = 240
CAN = 1254
W = 1024
ALPHA = 40
HEAD_Y = 24
# Eyes: sclera-matched to sockets, then nudged down onto the face.
EYE_DOWN = 48
EYE_POS = {
    "L": {"xy": (329, 297), "size": (145, 211)},
    "R": {"xy": (515, 249), "size": (194, 277)},
}


def load(rel: str) -> Image.Image:
    return Image.open(os.path.join(SRC, rel)).convert("RGBA")


def punch_face_hole(hair: Image.Image) -> Image.Image:
    """Keep scalp and side hair; clear the face so eyes/nose aren't covered."""
    out = hair.copy()
    px = out.load()
    cx, cy = 520, 470
    rx, ry = 280, 360
    w, h = out.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            nx = (x - cx) / rx
            ny = (y - cy) / ry
            d2 = nx * nx + ny * ny
            if d2 >= 1.0:
                continue
            if d2 <= 0.70:
                a2 = 0
            else:
                a2 = int(a * (d2 - 0.70) / 0.30)
            px[x, y] = (r, g, b, a2)
    return out


def cut_collar(head: Image.Image) -> Image.Image:
    out = head.copy()
    px = out.load()
    w, h = out.size
    fade = 14
    for y in range(HEAD_CUT_Y, h):
        t = max(0.0, 1.0 - (y - HEAD_CUT_Y) / fade)
        mul = int(round(255 * t))
        for x in range(w):
            r, g, b, a = px[x, y]
            if a:
                px[x, y] = (r, g, b, 0 if mul == 0 else min(a, mul))
    return out


def column_runs(im: Image.Image) -> list[tuple[int, int]]:
    w, h = im.size
    px = im.load()
    on = [any(px[x, y][3] > ALPHA for y in range(h)) for x in range(w)]
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


def crop_box(im: Image.Image, x0: int, x1: int) -> Image.Image:
    w, h = im.size
    px = im.load()
    ys = [y for y in range(h) for x in range(x0, x1 + 1) if px[x, y][3] > ALPHA]
    return im.crop((x0, min(ys), x1 + 1, max(ys) + 1))


def blank() -> Image.Image:
    return Image.new("RGBA", (CAN, CAN), (0, 0, 0, 0))


def main() -> None:
    body = load("body/body.png")
    head_src = cut_collar(load("head/head_base_bald.png"))
    hair_src = punch_face_hole(load("hair/hair.png"))

    sw = int(round(CAN * HEAD_SCALE))
    head_x = int(round(BODY_CX - NECK_CX * HEAD_SCALE))
    head_y = HEAD_Y
    body_y = int(round(head_y + NECK_CY * HEAD_SCALE - BODY_INSERT))
    H = body_y + body.size[1]
    print("canvas", W, H, "body_y", body_y, "head", head_x, head_y, "scale", HEAD_SCALE)

    def to_world(src1254: Image.Image) -> Image.Image:
        scaled = src1254.resize((sw, sw), Image.Resampling.LANCZOS)
        c = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        c.paste(scaled, (head_x, head_y), scaled)
        return c

    os.makedirs(DEST, exist_ok=True)
    os.makedirs(PREVIEW, exist_ok=True)

    body_l = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    body_l.paste(body, (0, body_y), body)
    body_l.save(os.path.join(DEST, "host_body.png"))
    to_world(head_src).save(os.path.join(DEST, "host_head.png"))
    to_world(hair_src).save(os.path.join(DEST, "host_hair.png"))

    eye_sheet = load("eyes/eyes_sprite_sheet.png")
    singles = [crop_box(eye_sheet, a, b) for a, b in column_runs(eye_sheet)]
    frames = [
        ("host_eyes_open.png", singles[0], singles[1]),
        ("host_eyes_half.png", singles[2], singles[3]),
        ("host_eyes_shut.png", singles[4], singles[5]),
    ]
    for name, left, right in frames:
        layer = blank()
        for sprite, side in ((left, "L"), (right, "R")):
            spec = EYE_POS[side]
            fitted = sprite.resize(spec["size"], Image.Resampling.LANCZOS)
            x, y = spec["xy"][0], spec["xy"][1] + EYE_DOWN
            layer.paste(fitted, (x, y), fitted)
        to_world(layer).save(os.path.join(DEST, name))
        print(name)

    mouth_sheet = load("mouth/mouth_sprite_sheet.png")
    mouths = [crop_box(mouth_sheet, a, b) for a, b in column_runs(mouth_sheet)]
    max_h = 200
    for i, m in enumerate(mouths):
        ratio = MOUTH_W / m.size[0]
        mw, mh = MOUTH_W, int(round(m.size[1] * ratio))
        if mh > max_h:
            ratio = max_h / m.size[1]
            mw, mh = int(round(m.size[0] * ratio)), max_h
        m2 = m.resize((mw, mh), Image.Resampling.LANCZOS)
        layer = blank()
        lx = MOUTH_CENTER[0] - mw // 2
        ly = MOUTH_CENTER[1] - int(round(mh * 0.28))
        layer.paste(m2, (lx, ly), m2)
        to_world(layer).save(os.path.join(DEST, f"host_mouth_{i}.png"))
        print("mouth", i, m2.size)

    comp = Image.new("RGBA", (W, H), (20, 0, 43, 255))
    for name in ["host_body.png", "host_hair.png", "host_head.png", "host_eyes_open.png", "host_mouth_0.png"]:
        comp.alpha_composite(Image.open(os.path.join(DEST, name)).convert("RGBA"))
    bb = comp.split()[-1].getbbox()
    comp.crop(bb).save(os.path.join(PREVIEW, "_assembled.png"))
    comp.crop((80, 0, 960, body_y + 380)).save(os.path.join(PREVIEW, "_face_close.png"))
    print("ok")


if __name__ == "__main__":
    main()
