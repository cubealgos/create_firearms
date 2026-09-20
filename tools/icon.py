#!/usr/bin/env python3
"""Render docs/modrinth/icon.png: a cartridge on the cubealgos navy badge Create add-ons share.

Every sibling's icon puts a real in-game asset of that mod's own in front of the badge (the
vanilla diamond sprite for `create_synthetic_diamonds`, a block-model render for
`create_metered_motor`). This mod has no weapon item sprite yet at FA-15 -- FA-9 ("Item models:
composite layers, condition on slot components, display transforms") is the ticket that draws the
six weapons -- so this script draws its own placeholder subject instead of reading one out of a
built jar: a 16x16 pixel-art cartridge (a brass case, a copper bullet tip, flat per-column shading
in Create's own item-sprite style), rendered straight into `subject()` below with Pillow, no
external asset read. It is also saved on its own to `docs/modrinth/cartridge-badge-sprite.png` so
the placeholder subject can be inspected without regenerating the full badge.

**Replace this subject before release** (FA-16): once FA-9 lands, port this script to read one of
the six weapons' real item sprites the way `create_synthetic_diamonds`' own `tools/icon.py` reads
the diamond sprite out of the Minecraft jar, and drop `cartridge_sprite()` entirely.

The badge itself (a white rim, a pale band, a navy disc `#0d1226` with its edge darkened to
`#090c1b`, and a blueprint grid lifted to `#344c80`, a one-pixel white outline and a soft shadow
behind the subject) is ported unchanged from `create_synthetic_diamonds`' own `tools/icon.py`,
which is itself the cubealgos navy badge every add-on icon shares
(`standards/marketing/modrinth-collection-icon.md`). Requires Pillow.
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

SIZE = 512
CENTRE = SIZE // 2
SUPERSAMPLE = 4
RIM = (255, 255, 255, 255)
BAND = (232, 236, 244, 255)
RING = (9, 12, 27, 255)
BLUEPRINT = (13, 18, 38, 255)
GRID = (52, 76, 128, 255)
OUTLINE = (255, 255, 255, 235)
SHADOW = (20, 50, 90, 130)
OUT = Path("docs/modrinth/icon.png")
SPRITE_OUT = Path("docs/modrinth/cartridge-badge-sprite.png")
BOX = 320

# The cartridge sprite: 16x16, a 6px-wide body centred at columns 5..10. Each row is shaded across
# those six columns from a bright highlight (column 5) to a dark shadow (column 10), the same flat
# per-column shading vanilla and Create item sprites use instead of a smooth gradient.
SPRITE_SIZE = 16
BODY_COLUMNS = range(5, 11)  # 6 columns: 5, 6, 7, 8, 9, 10
TRANSPARENT = (0, 0, 0, 0)

# Copper bullet tip, highlight -> shadow across the six body columns.
COPPER_SHADING = [
    (217, 141, 85, 255),
    (196, 122, 69, 255),
    (184, 115, 51, 255),
    (166, 100, 43, 255),
    (140, 82, 34, 255),
    (122, 74, 31, 255),
]
# Brass case, highlight -> shadow across the six body columns.
BRASS_SHADING = [
    (232, 207, 122, 255),
    (215, 186, 100, 255),
    (205, 170, 80, 255),
    (188, 152, 66, 255),
    (163, 128, 50, 255),
    (138, 106, 36, 255),
]
CANNELURE = (74, 54, 32, 255)  # the dark shoulder line separating tip from case
RIM_SHADE = [
    (150, 118, 45, 255),
    (140, 108, 40, 255),
    (132, 100, 36, 255),
    (122, 92, 32, 255),
    (107, 80, 28, 255),
    (91, 70, 26, 255),
]
PRIMER = (78, 45, 24, 255)  # the primer dot struck into the case rim's centre


def cartridge_sprite() -> Image.Image:
    """Hand-pixelled 16x16 cartridge: a copper bullet tip on a brass case, our own art."""
    img = Image.new("RGBA", (SPRITE_SIZE, SPRITE_SIZE), TRANSPARENT)

    def row(y: int, shading: list[tuple[int, int, int, int]]) -> None:
        for column, colour in zip(BODY_COLUMNS, shading):
            img.putpixel((column, y), colour)

    # Rows 1-4: the copper bullet tip, narrowing to a point at the apex.
    row(1, [TRANSPARENT, COPPER_SHADING[1], COPPER_SHADING[2], COPPER_SHADING[3], COPPER_SHADING[4], TRANSPARENT])
    row(2, COPPER_SHADING)
    row(3, COPPER_SHADING)
    row(4, COPPER_SHADING)
    # Row 5: the cannelure/shoulder where the tip meets the case.
    row(5, [CANNELURE] * 6)
    # Rows 6-13: the brass case body.
    for y in range(6, 14):
        row(y, BRASS_SHADING)
    # Row 14: the case rim, darker brass with a struck primer at its centre.
    row(14, RIM_SHADE)
    img.putpixel((7, 14), PRIMER)
    img.putpixel((8, 14), PRIMER)

    return img


def badge() -> Image.Image:
    big = SIZE * SUPERSAMPLE
    img = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    for radius, colour in ((256, RIM), (250, BAND), (238, RING), (200, BLUEPRINT)):
        r = radius * SUPERSAMPLE
        c = CENTRE * SUPERSAMPLE
        draw.ellipse((c - r, c - r, c + r, c + r), fill=colour)
    img = img.resize((SIZE, SIZE), Image.LANCZOS)

    grid = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    g = ImageDraw.Draw(grid)
    for k in range(-4, 5):
        p = CENTRE + k * 48
        g.line((p, 0, p, SIZE), fill=GRID, width=3)
        g.line((0, p, SIZE, p), fill=GRID, width=3)
    glow = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse((CENTRE - 120, CENTRE - 120, CENTRE + 120, CENTRE + 120), fill=(34, 48, 92, 150))
    glow = glow.filter(ImageFilter.GaussianBlur(50))
    inner = Image.alpha_composite(glow, grid)
    mask = Image.new("L", (SIZE, SIZE), 0)
    ImageDraw.Draw(mask).ellipse((CENTRE - 238, CENTRE - 238, CENTRE + 238, CENTRE + 238), fill=255)
    clipped = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    clipped.paste(inner, (0, 0), mask)
    return Image.alpha_composite(img, clipped)


def subject(img: Image.Image, raw: Image.Image) -> Image.Image:
    raw = raw.crop(raw.getbbox())
    w, h = raw.size
    factor = max(1, BOX // max(w, h))
    size = (w * factor, h * factor)
    sprite = raw.resize(size, Image.NEAREST)
    alpha = sprite.getchannel("A")
    x = CENTRE - size[0] // 2
    y = CENTRE - size[1] // 2
    step = factor
    grown = Image.new("L", (SIZE, SIZE), 0)
    for dx in (-step, 0, step):
        for dy in (-step, 0, step):
            grown.paste(alpha, (x + dx, y + dy), alpha)
    shadow = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    shadow.paste(SHADOW, (0, 0), grown.transform(grown.size, Image.AFFINE, (1, 0, -14, 0, 1, -14)))
    shadow = shadow.filter(ImageFilter.GaussianBlur(10))
    outline = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    outline.paste(OUTLINE, (0, 0), grown)
    img = Image.alpha_composite(img, shadow)
    img = Image.alpha_composite(img, outline)
    img.alpha_composite(sprite, (x, y))
    return img


def main() -> None:
    sprite = cartridge_sprite()
    SPRITE_OUT.parent.mkdir(parents=True, exist_ok=True)
    sprite.save(SPRITE_OUT, optimize=True)
    print(f"wrote {SPRITE_OUT} ({SPRITE_OUT.stat().st_size} bytes)")

    OUT.parent.mkdir(parents=True, exist_ok=True)
    subject(badge(), sprite).save(OUT, optimize=True)
    print(f"wrote {OUT} ({OUT.stat().st_size} bytes)")


if __name__ == "__main__":
    main()
