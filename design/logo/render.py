"""Renders the launcher icons and the TV banner for tri.photo.

Usage: python3 design/logo/render.py <path to a Noto Sans SC Regular .ttf>
The mark is two rounded photo cards, the back one tilted, on the pastel gradient (cream, orange,
lavender, indigo, ruby). Also renders the gradient band drawn behind the top of the home screen.
"""
import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "app/src/main/res"
INTER = RES / "font/inter_regular.ttf"

INK = (13, 37, 61)
INDIGO = (83, 58, 253)
INDIGO_DEEP = (68, 52, 212)
RUBY = (234, 34, 97)
CREAM = (245, 233, 212)
ORANGE = (255, 179, 107)
LAVENDER = (201, 182, 255)
# Gradient stops as (x, y, radius x, radius y, colour), in fractions of the image.
MESH = [
    (0.12, 0.55, 0.30, 0.60, CREAM),
    (0.32, 0.42, 0.28, 0.55, ORANGE),
    (0.55, 0.55, 0.30, 0.60, LAVENDER),
    (0.74, 0.45, 0.26, 0.55, INDIGO),
    (0.92, 0.60, 0.26, 0.55, RUBY),
]


def lerp(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def mesh(size, base=(255, 255, 255), stops=MESH, strength=1.0):
    """Soft colour blobs over [base]; drawn small and scaled up, which also blurs them."""
    w, h = size
    sw, sh = max(8, w // 8), max(8, h // 8)
    img = Image.new("RGB", (sw, sh), base)
    px = img.load()
    for y in range(sh):
        for x in range(sw):
            c = base
            for cx, cy, rx, ry, col in stops:
                d = math.hypot((x / sw - cx) / rx, (y / sh - cy) / ry)
                c = lerp(c, col, max(0.0, 1 - d) ** 1.4 * strength)
            px[x, y] = c
    return img.resize(size, Image.BICUBIC).filter(ImageFilter.GaussianBlur(max(w, h) / 60))


def card(size, radius, fill):
    img = Image.new("RGBA", size, (0, 0, 0, 0))
    ImageDraw.Draw(img).rounded_rectangle((0, 0, size[0] - 1, size[1] - 1), radius, fill=fill)
    return img


def front_card(size, radius):
    """White photo card with a sun and two hills cut in Blurple and magenta."""
    w, h = size
    img = card(size, radius, (255, 255, 255, 255))
    pad = round(w * 0.11)
    inner = (pad, pad, w - pad, h - pad)
    photo = Image.new("RGBA", size, (0, 0, 0, 0))
    d = ImageDraw.Draw(photo)
    d.rounded_rectangle(inner, round(radius * 0.55), fill=CREAM + (255,))
    iw, ih = inner[2] - inner[0], inner[3] - inner[1]
    sx, sy, sr = inner[0] + iw * 0.72, inner[1] + ih * 0.3, iw * 0.11
    d.ellipse((sx - sr, sy - sr, sx + sr, sy + sr), fill=RUBY + (255,))
    d.polygon([(inner[0], inner[3]), (inner[0] + iw * 0.38, inner[1] + ih * 0.42), (inner[0] + iw * 0.78, inner[3])], fill=INDIGO + (255,))
    d.polygon([(inner[0] + iw * 0.42, inner[3]), (inner[0] + iw * 0.7, inner[1] + ih * 0.6), (inner[2], inner[1] + ih * 0.85), (inner[2], inner[3])], fill=INK + (255,))
    mask = Image.new("L", size, 0)
    ImageDraw.Draw(mask).rounded_rectangle(inner, round(radius * 0.55), fill=255)
    img.paste(photo, (0, 0), mask)
    return img


def mark(size):
    """The two cards, transparent background, centred in a square of [size]."""
    s = size
    out = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    cw, ch = round(s * 0.58), round(s * 0.46)
    r = round(s * 0.07)
    back = card((cw, ch), r, (255, 255, 255, 120)).rotate(-12, resample=Image.BICUBIC, expand=True)
    out.alpha_composite(back, (round(s * 0.5 - back.width / 2 + s * 0.06), round(s * 0.5 - back.height / 2 - s * 0.07)))
    front = front_card((cw, ch), r).rotate(6, resample=Image.BICUBIC, expand=True)
    shadow = Image.new("RGBA", front.size, (0, 55, 112, 0))
    shadow.putalpha(front.getchannel("A").point(lambda a: a * 0.22))
    shadow = shadow.filter(ImageFilter.GaussianBlur(s * 0.02))
    pos = (round(s * 0.5 - front.width / 2 - s * 0.03), round(s * 0.5 - front.height / 2 + s * 0.06))
    out.alpha_composite(shadow, (pos[0], pos[1] + round(s * 0.015)))
    out.alpha_composite(front, pos)
    return out


def square_icon(px):
    big = 1024
    bg = mesh((big, big), base=LAVENDER, stops=[
        (0.05, 0.05, 0.7, 0.7, CREAM), (0.15, 0.85, 0.6, 0.6, ORANGE),
        (0.95, 0.15, 0.7, 0.7, INDIGO), (0.95, 0.95, 0.55, 0.55, RUBY),
    ]).convert("RGBA")
    m = Image.new("L", (big, big), 0)
    ImageDraw.Draw(m).rounded_rectangle((0, 0, big - 1, big - 1), round(big * 0.22), fill=255)
    icon = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    icon.paste(bg, (0, 0), m)
    icon.alpha_composite(mark(round(big * 0.86)), (round(big * 0.07), round(big * 0.07)))
    return icon.resize((px, px), Image.LANCZOS)


def adaptive_foreground(px):
    # 108dp canvas with the 72dp safe zone in the middle; the mark fills about the safe zone.
    big = 1080
    out = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    m = mark(round(big * 0.66))
    out.alpha_composite(m, ((big - m.width) // 2, (big - m.height) // 2))
    return out.resize((px, px), Image.LANCZOS)


def banner(cjk_font):
    scale = 4
    w, h = 320 * scale, 180 * scale
    img = Image.new("RGBA", (w, h), (255, 255, 255, 255))
    img.paste(mesh((w, round(h * 0.55))), (0, 0))
    fade = Image.linear_gradient("L").resize((w, round(h * 0.55)))
    white = Image.new("RGBA", (w, round(h * 0.55)), (255, 255, 255, 255))
    img.paste(white, (0, 0), fade)
    m = mark(round(h * 0.82))
    img.alpha_composite(m, (round(w * 0.02), round((h - m.height) / 2)))
    d = ImageDraw.Draw(img)
    title = ImageFont.truetype(cjk_font, round(h * 0.30))
    sub = ImageFont.truetype(str(INTER), round(h * 0.10))
    x = round(w * 0.47)
    d.text((x, round(h * 0.22)), "回忆", font=title, fill=INK)
    d.text((x + round(h * 0.01), round(h * 0.64)), "tri.photo", font=sub, fill=(100, 116, 141))
    return img.resize((320, 180), Image.LANCZOS)


def main():
    cjk = sys.argv[1]
    sizes = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    for name, px in sizes.items():
        d = RES / f"mipmap-{name}"
        d.mkdir(parents=True, exist_ok=True)
        square_icon(px).save(d / "ic_launcher.png")
        adaptive_foreground(round(px * 108 / 48)).save(d / "ic_launcher_foreground.png")
    d = RES / "drawable-xhdpi"
    d.mkdir(parents=True, exist_ok=True)
    banner(cjk).convert("RGB").save(d / "banner.png")
    square_icon(256).save(ROOT / "docs/icon.png")
    # The band behind the top of the home screen: 960 x 360 dp at xhdpi, fading to white at the bottom.
    band = mesh((960, 360)).convert("RGBA")
    fade = Image.linear_gradient("L").resize(band.size)
    band.paste(Image.new("RGBA", band.size, (255, 255, 255, 255)), (0, 0), fade)
    band.convert("RGB").resize((1920, 720), Image.BICUBIC).save(RES / "drawable-xhdpi/mesh_band.jpg", quality=88)


if __name__ == "__main__":
    main()
