#!/usr/bin/env python3
"""Render docs/modrinth/icon.png: the AKM assault rifle, rendered in real 3D from this mod's own
weapon item model, on the cubealgos navy grid badge Create add-ons share (FA-27, Kevin's
2026-09-21 icon ruling).

The old placeholder -- a hand-drawn 16x16 pixel-art cartridge, since no weapon item model existed
yet (FA-15/FA-16) -- is retired now that FA-9's real weapon item models exist. Kevin reviewed a
sheet of candidates and picked the AKM ("B2"): this mod ships one Modrinth icon, not six, and the
AKM is that subject. This mod is a Create Fly add-on, so unlike sibling mods that drop the grid
theme, it keeps the navy grid badge every cubealgos add-on shares
(`standards/marketing/modrinth-collection-icon.md`).

**The model and texture are read straight from this mod's own assets** -- no jar, no external
source, nothing new to vendor or credit in `NOTICE`:
`src/main/resources/assets/firearms/models/item/weapon/akm.json` and its texture atlas
`.../textures/item/weapon/akm.png` (32x32). `akm.json`'s `elements`/`faces[*].uv` values are
already authored in raw 32x32 texture-pixel space (confirmed by reading the file: e.g. the first
element's `north` face uv is `[4.5, 4.5, 6.0, 6.5]`, sane pixel coordinates within [0, 32]) -- so,
unlike some sibling repos' models, no `texture_size[0]/16` UV scale is applied here.

**The tilt is `[30, -135, 0]`**, matching the model's own default `gui` display transform
(inherited from its parent, `firearms:item/weapon/class/assault_rifle`, which holds only display
transforms -- the AKM's own `elements` are fully self-contained). Fit box is 224px, 70% of the
320px box the badge otherwise uses, to leave more navy grid visible around a subject this
detailed.

**The lightening pass (Kevin's one change on top of the pick): the render reads dark under
vanilla's own flat item shading once shrunk onto a navy badge**, so three adjustments are made, in
the render only -- the game texture/atlas is untouched:

1. The per-face `SHADE` table is raised from vanilla's own values (up 1.0, down 0.5, north/south
   0.8, east/west 0.6) by +0.1 on every face but `up` (up 1.0, down 0.7, north/south 0.9, east/west
   0.8), preserving the relative shading order between faces.
2. An overall brightness lift multiplies the assembled sprite's RGB (never alpha, never the badge)
   by `(1 + pct)`, clamped to 255. Kevin asked for +15%/+25%/+35% compared side by side
   (`--sheet`, step 8 of FA-27) before picking one; see DEFAULT_PCT below for the pick and why.
3. A soft warm key light (`WARM_LIGHT_COLOUR`, a warm off-white/light amber) glows from the
   sprite's own top-left corner, masked to the sprite's own alpha so it never spills onto the
   badge, low peak opacity fading smoothly to nothing.

The badge (`badge()`) and the outline/shadow compositing (`compose()`) are this repo's own
existing code, adapted only for a "smooth" (LANCZOS) fit scale rather than the old placeholder's
nearest-neighbour pixel-art scale, since a 3D projection is already anti-aliased art, not a raw
texel grid -- the same adaptation `create_metered_motor`'s `tools/icon.py` makes for its own
block-model render. The 3D projector itself (`rot_axis` through `render_model`) is new to this
repo, ported inline from `create_metered_motor`'s `tools/icon.py`: painter's-order face sorting
with backface culling, per-face UV affine sampling (uv flips and the 0/90/180/270 `rotation` key),
and per-element `rotation` (axis/origin/angle) support, which the AKM's own barrel/grip/stock
elements use. Requires Pillow.

Usage:
    python3 tools/icon.py              # writes docs/modrinth/icon.png at DEFAULT_PCT
    python3 tools/icon.py --pct 0.15   # writes it at a different brightness lift
    python3 tools/icon.py --sheet      # writes the FA-27 brightness comparison sheet instead
"""
from __future__ import annotations

import argparse
import json
import math
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent.parent
MODEL_PATH = ROOT / "src/main/resources/assets/firearms/models/item/weapon/akm.json"
TEXTURE_DIR = ROOT / "src/main/resources/assets/firearms/textures"
OUT = ROOT / "docs/modrinth/icon.png"
SHEET_OUT = ROOT / "scratchpad/fa-icon-brightness.png"

# firearms:item/weapon/class/assault_rifle's own `gui` display transform -- confirmed by reading
# that file, which holds only display transforms, nothing for this script to merge from its
# `elements` (the AKM's own `elements` are fully self-contained).
GUI_TRANSFORM = {"rotation": [30, -135, 0], "scale": [0.741, 0.741, 0.741]}


# ============================================================== 3D projection
# Ported inline from create_metered_motor's tools/icon.py, itself ported from the heimathafen
# prototype standards/marketing/modrinth/block-model-render.py.

RENDER_SIZE = 512
RENDER_SUPERSAMPLE = 4
RENDER_CANVAS = RENDER_SIZE * RENDER_SUPERSAMPLE

FACE_VERTS = {
    "down":  [(0, 0, 0), (0, 0, 1), (1, 0, 1), (1, 0, 0)],
    "up":    [(0, 1, 1), (0, 1, 0), (1, 1, 0), (1, 1, 1)],
    "north": [(1, 1, 0), (1, 0, 0), (0, 0, 0), (0, 1, 0)],
    "south": [(0, 1, 1), (0, 0, 1), (1, 0, 1), (1, 1, 1)],
    "west":  [(0, 1, 0), (0, 0, 0), (0, 0, 1), (0, 1, 1)],
    "east":  [(1, 1, 1), (1, 0, 1), (1, 0, 0), (1, 1, 0)],
}
FACE_NORMAL = {
    "down": (0, -1, 0), "up": (0, 1, 0),
    "north": (0, 0, -1), "south": (0, 0, 1),
    "west": (-1, 0, 0), "east": (1, 0, 0),
}
# FA-27: raised from vanilla's own flat item-render shading (up 1.0, down 0.5, north/south 0.8,
# east/west 0.6) by +0.1 on every face but `up`, preserving the relative shading order between
# faces -- vanilla's own values read dark once the AKM is shrunk onto the navy badge.
SHADE = {"up": 1.0, "down": 0.7, "north": 0.9, "south": 0.9, "east": 0.8, "west": 0.8}


def rot_axis(p, axis, deg):
    ang = math.radians(deg)
    c, s = math.cos(ang), math.sin(ang)
    x, y, z = p
    if axis == "x":
        return (x, y * c - z * s, y * s + z * c)
    if axis == "y":
        return (x * c + z * s, y, -x * s + z * c)
    return (x * c - y * s, x * s + y * c, z)


def rotate_about(p, origin, axis, deg):
    rel = (p[0] - origin[0], p[1] - origin[1], p[2] - origin[2])
    r = rot_axis(rel, axis, deg)
    return (r[0] + origin[0], r[1] + origin[1], r[2] + origin[2])


def display_transform(p, pivot, rx, ry, rz):
    rel = (p[0] - pivot[0], p[1] - pivot[1], p[2] - pivot[2])
    rel = rot_axis(rel, "x", rx)
    rel = rot_axis(rel, "y", ry)
    rel = rot_axis(rel, "z", rz)
    return rel


def affine_from_points(src, dst):
    """3-point affine solve: dst = A*src + t. Returns forward (a,b,c,d,e,f)."""
    (x0, y0), (x1, y1), (x2, y2) = src
    (u0, v0), (u1, v1), (u2, v2) = dst
    mat = [[x0, y0, 1], [x1, y1, 1], [x2, y2, 1]]
    det = (mat[0][0] * (mat[1][1] * mat[2][2] - mat[1][2] * mat[2][1])
           - mat[0][1] * (mat[1][0] * mat[2][2] - mat[1][2] * mat[2][0])
           + mat[0][2] * (mat[1][0] * mat[2][1] - mat[1][1] * mat[2][0]))
    if abs(det) < 1e-9:
        return None

    def solve(vals):
        res = []
        for col in range(3):
            m2 = [row[:] for row in mat]
            for r in range(3):
                m2[r][col] = vals[r]
            d = (m2[0][0] * (m2[1][1] * m2[2][2] - m2[1][2] * m2[2][1])
                 - m2[0][1] * (m2[1][0] * m2[2][2] - m2[1][2] * m2[2][0])
                 + m2[0][2] * (m2[1][0] * m2[2][1] - m2[1][1] * m2[2][0]))
            res.append(d / det)
        return res

    a, b, c = solve([u0, u1, u2])
    d, e, f = solve([v0, v1, v2])
    return (a, b, c, d, e, f)


def invert_affine(coef):
    a, b, c, d, e, f = coef
    det = a * e - b * d
    if abs(det) < 1e-9:
        return None
    ia = e / det
    ib = -b / det
    ic = -(ia * c + ib * f)
    id_ = -d / det
    ie = a / det
    if_ = -(id_ * c + ie * f)
    return (ia, ib, ic, id_, ie, if_)


def build_faces(model, textures, pivot, gui):
    """Returns list of (depth, canvas_quad[4], texture_img, uv_patch_quad[4], shade)."""
    faces = []
    rx, ry, rz = gui["rotation"]
    for elem in model["elements"]:
        frm, to = elem["from"], elem["to"]
        erot = elem.get("rotation")
        corners = {}
        for bx in (0, 1):
            for by in (0, 1):
                for bz in (0, 1):
                    p = (
                        frm[0] if bx == 0 else to[0],
                        frm[1] if by == 0 else to[1],
                        frm[2] if bz == 0 else to[2],
                    )
                    if erot:
                        p = rotate_about(p, erot["origin"], erot["axis"], erot["angle"])
                    corners[(bx, by, bz)] = p
        for face_name, face in elem.get("faces", {}).items():
            verts_frac = FACE_VERTS[face_name]
            verts3d = [corners[v] for v in verts_frac]

            normal = FACE_NORMAL[face_name]
            if erot:
                normal = rot_axis(normal, erot["axis"], erot["angle"])
            cam_normal = rot_axis(rot_axis(rot_axis(normal, "x", rx), "y", ry), "z", rz)
            if cam_normal[2] <= 1e-4:
                continue  # backface culled

            cam_pts = [display_transform(p, pivot, rx, ry, rz) for p in verts3d]
            depth = sum(p[2] for p in cam_pts) / 4.0
            canvas_quad = [(p[0], -p[1]) for p in cam_pts]

            tex_key = face["texture"].lstrip("#")
            tex_img = textures[tex_key]
            u1, v1, u2, v2 = face["uv"]
            flip_x = u1 > u2
            flip_y = v1 > v2
            lo = (min(u1, u2), min(v1, v2))
            hi = (max(u1, u2), max(v1, v2))
            patch = tex_img.crop((round(lo[0]), round(lo[1]), round(hi[0]), round(hi[1])))
            if patch.width == 0 or patch.height == 0:
                continue
            if flip_x:
                patch = patch.transpose(Image.FLIP_LEFT_RIGHT)
            if flip_y:
                patch = patch.transpose(Image.FLIP_TOP_BOTTOM)
            pw, ph = patch.size
            default_patch_quad = [(0, 0), (0, ph), (pw, ph), (pw, 0)]
            rotation = face.get("rotation", 0)
            shift = (rotation // 90) % 4
            patch_quad = [default_patch_quad[(i + shift) % 4] for i in range(4)]

            faces.append((depth, canvas_quad, patch, patch_quad, SHADE[face_name]))
    faces.sort(key=lambda f: f[0])  # far to near
    return faces


def fit_scale(faces, canvas, margin=0.88):
    xs, ys = [], []
    for _, quad, *_ in faces:
        for x, y in quad:
            xs.append(x)
            ys.append(y)
    w = max(xs) - min(xs)
    h = max(ys) - min(ys)
    cx = (max(xs) + min(xs)) / 2
    cy = (max(ys) + min(ys)) / 2
    scale = (canvas * margin) / max(w, h)
    return scale, cx, cy


def render_model(model, textures, gui):
    pivot = (8.0, 8.0, 8.0)
    faces = build_faces(model, textures, pivot, gui)
    scale, cx, cy = fit_scale(faces, RENDER_CANVAS)

    canvas = Image.new("RGBA", (RENDER_CANVAS, RENDER_CANVAS), (0, 0, 0, 0))
    for depth, quad, patch, patch_quad, shade in faces:
        dst = [((x - cx) * scale + RENDER_CANVAS / 2, (y - cy) * scale + RENDER_CANVAS / 2)
               for x, y in quad]
        patch = patch.convert("RGBA")
        if shade != 1.0:
            r, g, b, a = patch.split()
            r = r.point(lambda v: int(v * shade))
            g = g.point(lambda v: int(v * shade))
            b = b.point(lambda v: int(v * shade))
            patch = Image.merge("RGBA", (r, g, b, a))

        fwd = affine_from_points(patch_quad[:3], dst[:3])
        if fwd is None:
            continue
        inv = invert_affine(fwd)
        if inv is None:
            continue
        layer = patch.transform((RENDER_CANVAS, RENDER_CANVAS), Image.AFFINE, inv,
                                 resample=Image.NEAREST, fillcolor=(0, 0, 0, 0))
        canvas.alpha_composite(layer)

    return canvas.resize((RENDER_SIZE, RENDER_SIZE), Image.LANCZOS)


# ============================================================== texture resolution

def load_textures(model: dict) -> dict[str, Image.Image]:
    tex_refs = {k: v for k, v in model["textures"].items() if k != "particle"}
    textures = {}
    for key, location in tex_refs.items():
        namespace, path = location.split(":", 1)
        if namespace != "firearms":
            raise SystemExit(f"icon: unexpected texture namespace {namespace!r} in {location!r}")
        file = TEXTURE_DIR / f"{path}.png"
        if not file.exists():
            raise SystemExit(f"icon: texture {location!r} not found at {file}")
        textures[key] = Image.open(file).convert("RGBA")
    return textures


# ============================================================== lightening pass (FA-27)

# A warm off-white/light amber, well short of pure white, so the key light reads as light rather
# than as a colour cast.
WARM_LIGHT_COLOUR = (255, 225, 185)
WARM_LIGHT_PEAK_ALPHA = 40


def brighten(img: Image.Image, pct: float) -> Image.Image:
    """Lift the sprite's RGB by `(1 + pct)`, clamped to 255. Alpha is untouched -- FA-27
    constraint 2, the overall brightness lift on top of the raised SHADE table above."""
    r, g, b, a = img.split()
    lut = [min(255, round(v * (1 + pct))) for v in range(256)]
    return Image.merge("RGBA", (r.point(lut), g.point(lut), b.point(lut), a))


def warm_key_light(img: Image.Image, colour=WARM_LIGHT_COLOUR, peak_alpha=WARM_LIGHT_PEAK_ALPHA) -> Image.Image:
    """A soft warm glow from the sprite's own top-left corner, masked to the sprite's own alpha so
    it never spills onto the badge around it -- FA-27 constraint 3."""
    bbox = img.getbbox()
    if bbox is None:
        return img
    x0, y0, x1, y1 = bbox
    w, h = img.size
    bw, bh = x1 - x0, y1 - y0
    span = max(bw, bh)

    glow_alpha = Image.new("L", (w, h), 0)
    # Centred a little inside the sprite's own top-left corner, so the peak falls on the subject
    # rather than at its very edge.
    cx, cy = x0 + bw * 0.12, y0 + bh * 0.12
    r = span * 0.55
    ImageDraw.Draw(glow_alpha).ellipse((cx - r, cy - r, cx + r, cy + r), fill=peak_alpha)
    glow_alpha = glow_alpha.filter(ImageFilter.GaussianBlur(span * 0.35))

    # Mask to the sprite's own alpha: never spills onto the navy badge around the subject.
    glow_alpha = ImageChops.multiply(glow_alpha, img.getchannel("A"))

    glow = Image.new("RGBA", (w, h), colour + (0,))
    glow.putalpha(glow_alpha)
    return Image.alpha_composite(img, glow)


def lighten(sprite: Image.Image, pct: float) -> Image.Image:
    """The full FA-27 lightening pass: the overall brightness lift, then the warm top-left key
    light. Both act on the assembled sprite's RGB only, before it is handed to the badge
    compositor -- the badge itself is never touched."""
    return warm_key_light(brighten(sprite, pct))


def blowout_fraction(sprite: Image.Image, threshold: int = 250) -> float:
    """Fraction of the sprite's own non-transparent pixels where any RGB channel is >= threshold
    -- an objective proxy for 'blown out' (losing the wood-vs-steel colour distinction), used
    alongside the visual read on the brightness comparison sheet to pick DEFAULT_PCT."""
    px = sprite.load()
    w, h = sprite.size
    total = 0
    blown = 0
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            total += 1
            if r >= threshold or g >= threshold or b >= threshold:
                blown += 1
    return blown / total if total else 0.0


# ============================================================== navy badge
# This repo's own existing badge/outline/shadow code (FA-15), adapted only for a LANCZOS "smooth"
# fit scale -- the 3D projection is already anti-aliased art, not a raw pixel-art texel grid.

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
BOX = 224  # FA-27: 70% of the standard 320px fit box, to leave more grid visible around the AKM


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


def compose(base: Image.Image, sprite: Image.Image, box: int = BOX) -> Image.Image:
    """"smooth" mode: sprite is already-rendered/anti-aliased art (the AKM's projection), so it is
    LANCZOS-scaled to fit, not zoomed like the old placeholder's raw pixel-art texture."""
    w, h = sprite.size
    longest = max(w, h)
    factor = box / longest
    size = (round(w * factor), round(h * factor))
    scale = size[0] / w
    sprite = sprite.resize(size, Image.LANCZOS)
    alpha = sprite.getchannel("A")
    x = CENTRE - size[0] // 2
    y = CENTRE - size[1] // 2
    step = max(1, round(scale))
    grown = Image.new("L", (SIZE, SIZE), 0)
    for dx in (-step, 0, step):
        for dy in (-step, 0, step):
            grown.paste(alpha, (x + dx, y + dy), alpha)
    shadow = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    shadow.paste(SHADOW, (0, 0), grown.transform(grown.size, Image.AFFINE, (1, 0, -14, 0, 1, -14)))
    shadow = shadow.filter(ImageFilter.GaussianBlur(10))
    outline = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    outline.paste(OUTLINE, (0, 0), grown)
    img = Image.alpha_composite(base, shadow)
    img = Image.alpha_composite(img, outline)
    img.alpha_composite(sprite, (x, y))
    return img


# ============================================================== pipeline

# FA-27 step 8: Kevin asked to compare +15%/+25%/+35% before picking one, defaulting to +25%
# unless it visibly blows out the AKM's wood-coloured furniture (clips to near-white, losing the
# wood-vs-steel distinction), in which case +15% instead. See `scratchpad/fa-icon-brightness.png`
# and the ticket's own Constraints section for the blowout-fraction evidence: +25% stays well
# short of the +35% jump and keeps the stock's wood tone visibly distinct from the steel
# receiver/barrel at both 256px and the 64px size Modrinth actually ships, so +25% is the pick.
DEFAULT_PCT = 0.25


def render_icon(pct: float = DEFAULT_PCT) -> Image.Image:
    model = json.loads(MODEL_PATH.read_text())
    textures = load_textures(model)
    sprite = render_model(model, textures, GUI_TRANSFORM)
    sprite = lighten(sprite, pct)
    return compose(badge(), sprite, BOX)


LEVELS = (0.15, 0.25, 0.35)
SHEET_SIZES = (256, 64)


def brightness_sheet(out_path: Path = SHEET_OUT, levels=LEVELS, sizes=SHEET_SIZES) -> dict[float, float]:
    """Render the AKM candidate at each of `levels` (same shading table and warm light, only the
    brightness lift differs), badge-composited, at both `sizes`, into one labelled comparison grid
    at `out_path` -- FA-27 step 8. Returns each level's blowout fraction (see `blowout_fraction`)
    for the report."""
    model = json.loads(MODEL_PATH.read_text())
    textures = load_textures(model)
    base_sprite = render_model(model, textures, GUI_TRANSFORM)

    try:
        font = ImageFont.load_default(size=16)
    except TypeError:  # older Pillow without the size kwarg
        font = ImageFont.load_default()

    label_h = 30
    pad = 16
    col_w = max(sizes) + pad * 2
    row_h = label_h + sum(sizes) + pad * (len(sizes) + 1)
    sheet = Image.new("RGB", (col_w * len(levels), row_h), (24, 28, 40))
    draw = ImageDraw.Draw(sheet)

    fractions = {}
    for col, pct in enumerate(levels):
        lit = lighten(base_sprite, pct)
        fractions[pct] = blowout_fraction(lit)
        icon = compose(badge(), lit, BOX)
        x0 = col * col_w
        label = f"+{round(pct * 100)}%  blowout {fractions[pct]:.1%}"
        draw.text((x0 + pad, 6), label, fill=(255, 255, 255), font=font)
        y = label_h + pad
        for size in sizes:
            thumb = icon.resize((size, size), Image.LANCZOS).convert("RGBA")
            sheet.paste(thumb, (x0 + pad, y), thumb)
            y += size + pad

    out_path.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out_path, optimize=True)
    return fractions


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--pct", type=float, default=None,
                         help=f"brightness lift to ship (default: {DEFAULT_PCT})")
    parser.add_argument("--sheet", action="store_true",
                         help=f"write the FA-27 brightness comparison sheet to {SHEET_OUT} instead")
    args = parser.parse_args()

    if args.sheet:
        fractions = brightness_sheet()
        print(f"wrote {SHEET_OUT} ({SHEET_OUT.stat().st_size} bytes)")
        for pct in sorted(fractions):
            print(f"  +{round(pct * 100)}%: blowout {fractions[pct]:.2%}")
        return

    pct = DEFAULT_PCT if args.pct is None else args.pct
    icon = render_icon(pct)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    icon.save(OUT, optimize=True)
    print(f"wrote {OUT} ({OUT.stat().st_size} bytes) at +{round(pct * 100)}% brightness")


if __name__ == "__main__":
    main()
