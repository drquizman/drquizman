"""Export puppet parts + PuppetRig.kt. Face lives in 1254 head space; body/arms in body space."""
from __future__ import annotations

import os

from PIL import Image

SRC = r"C:\Users\akhin\Downloads\doctor_puppet_assets\doctor_puppet_assets"
DEST = r"C:\Users\akhin\Documents\Pet\Dr.Quizman\tv\src\main\res\drawable-nodpi"
PREVIEW = r"C:\Users\akhin\Documents\Pet\Dr.Quizman\preview"
KOTLIN = r"C:\Users\akhin\Documents\Pet\Dr.Quizman\tv\src\main\java\com\drquizman\tv\ui\studio\PuppetRig.kt"

HEAD_SCALE = 0.42
HEAD_CUT_Y = 1038
NECK_CX, NECK_CY = 664.5, 1020.0
BODY_CX, BODY_INSERT = 518.5, 58
MOUTH_CENTER = (522, 748)
MOUTH_W = 240
CAN = 1254
BODY_W, BODY_H = 1024, 1536
ARM_NATIVE_W = 1086
ALPHA = 40
HEAD_PAD = 20
EYE_DOWN = 48
EYE_POS = {
    "L": {"xy": (329, 297), "size": (145, 211)},
    "R": {"xy": (515, 249), "size": (194, 277)},
}
BROW_CENTER = (506, 338)
BROW_W = 290


def load(rel: str) -> Image.Image:
    return Image.open(os.path.join(SRC, rel)).convert("RGBA")


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


def punch_face_hole(hair: Image.Image) -> Image.Image:
    out = hair.copy()
    px = out.load()
    cx, cy, rx, ry = 520, 470, 280, 360
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


def hide_arm_tail(arm: Image.Image, body: Image.Image) -> Image.Image:
    """Drop coat remnants that poke through the legs."""
    out = arm.copy()
    ap, bp = out.load(), body.load()
    h = min(out.size[1], body.size[1])
    w = min(out.size[0], body.size[0])
    for y in range(730, h):
        for x in range(w):
            if ap[x, y][3] == 0:
                continue
            if bp[x, y][3] < 40:
                r, g, b, _ = ap[x, y]
                ap[x, y] = (r, g, b, 0)
    return out


def save(im: Image.Image, name: str) -> None:
    im.save(os.path.join(DEST, name))
    print(" ", name, im.size)


def main() -> None:
    os.makedirs(DEST, exist_ok=True)
    os.makedirs(PREVIEW, exist_ok=True)

    body = load("body/body.png")
    head = cut_collar(load("head/head_base_bald.png"))
    hair = punch_face_hole(load("hair/hair.png"))
    save(body, "host_body.png")
    save(head, "host_head.png")
    save(hair, "host_hair.png")

    eye_sheet = load("eyes/eyes_sprite_sheet.png")
    eyes = [crop_box(eye_sheet, a, b) for a, b in column_runs(eye_sheet)]
    frames = [
        ("host_eyes_open.png", eyes[0], eyes[1]),
        ("host_eyes_half.png", eyes[2], eyes[3]),
        ("host_eyes_shut.png", eyes[4], eyes[5]),
    ]
    for name, left, right in frames:
        layer = blank()
        for sprite, side in ((left, "L"), (right, "R")):
            spec = EYE_POS[side]
            fitted = sprite.resize(spec["size"], Image.Resampling.LANCZOS)
            x, y = spec["xy"][0], spec["xy"][1] + EYE_DOWN
            layer.paste(fitted, (x, y), fitted)
        save(layer, name)

    brow_sheet = load("eyebrows/eyebrows_sprite_sheet.png")
    brows = [crop_box(brow_sheet, a, b) for a, b in column_runs(brow_sheet)]
    names = ["host_brow_idle.png", "host_brow_up.png", "host_brow_angry.png", "host_brow_sad.png"]
    for name, brow in zip(names, brows):
        ratio = BROW_W / brow.size[0]
        b2 = brow.resize((BROW_W, int(brow.size[1] * ratio)), Image.Resampling.LANCZOS)
        layer = blank()
        layer.paste(b2, (BROW_CENTER[0] - b2.size[0] // 2, BROW_CENTER[1] - b2.size[1] // 2), b2)
        save(layer, name)

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
        layer.paste(m2, (MOUTH_CENTER[0] - mw // 2, MOUTH_CENTER[1] - int(mh * 0.28)), m2)
        save(layer, f"host_mouth_{i}.png")

    arm_scale = BODY_W / ARM_NATIVE_W
    for src_name, out_name in [
        ("arm_open_palm.png", "host_arm_palm.png"),
        ("arm_pointing.png", "host_arm_point.png"),
        ("arm_raised_fist.png", "host_arm_fist.png"),
    ]:
        arm = load("arms/" + src_name)
        a2 = arm.resize(
            (int(arm.size[0] * arm_scale), int(arm.size[1] * arm_scale)),
            Image.Resampling.LANCZOS,
        )
        save(hide_arm_tail(a2, body), out_name)

    head_w = CAN * HEAD_SCALE
    head_x = BODY_CX - NECK_CX * HEAD_SCALE
    head_y = HEAD_PAD
    body_y = head_y + NECK_CY * HEAD_SCALE - BODY_INSERT
    world_h = body_y + BODY_H
    arm_h = BODY_H * arm_scale * (1448 / 1536)  # scaled arm height
    arm_h = 1448 * arm_scale

    kt = f"""package com.drquizman.tv.ui.studio

/** Layout units = source pixels. Scaled to the parent box at runtime. */
object PuppetRig {{
    const val W = {BODY_W:g}f
    const val H = {world_h:.1f}f
    const val BodyX = 0f
    const val BodyY = {body_y:.1f}f
    const val BodyW = {BODY_W:g}f
    const val BodyH = {BODY_H:g}f
    const val HeadX = {head_x:.1f}f
    const val HeadY = {head_y:.1f}f
    const val HeadW = {head_w:.1f}f
    const val HeadH = {head_w:.1f}f
    const val NeckX = {(NECK_CX / CAN):.4f}f
    const val NeckY = {(NECK_CY / CAN):.4f}f
    const val ArmX = 0f
    const val ArmY = {body_y:.1f}f
    const val ArmW = {BODY_W:g}f
    const val ArmH = {arm_h:.1f}f
}}
"""
    with open(KOTLIN, "w", encoding="utf-8") as f:
        f.write(kt)
    print("wrote", KOTLIN)

    # preview still
    comp = Image.new("RGBA", (BODY_W, int(round(world_h))), (20, 0, 43, 255))

    def paste_file(name: str, xy: tuple[int, int], size: tuple[int, int] | None = None) -> None:
        im = Image.open(os.path.join(DEST, name)).convert("RGBA")
        if size:
            im = im.resize(size, Image.Resampling.LANCZOS)
        comp.alpha_composite(im, xy)

    paste_file("host_arm_palm.png", (0, int(body_y)), (BODY_W, int(round(arm_h))))
    paste_file("host_body.png", (0, int(body_y)))
    hx, hy = int(round(head_x)), int(round(head_y))
    hs = int(round(head_w))
    for name in ["host_hair.png", "host_head.png", "host_brow_idle.png", "host_eyes_open.png", "host_mouth_0.png"]:
        paste_file(name, (hx, hy), (hs, hs))
    bb = comp.split()[-1].getbbox()
    comp.crop(bb).save(os.path.join(PREVIEW, "_rig_still.png"))
    print("preview", os.path.join(PREVIEW, "_rig_still.png"), "world", BODY_W, int(world_h))


if __name__ == "__main__":
    main()
