#!/usr/bin/env python3
"""Draw every FA-9 item sprite and write the item model files that reference them: six base-weapon
silhouettes, 22 attachment glyphs (each both a standalone 16x16 icon and a 32x16 weapon-layer
overlay pre-positioned at its slot's fixed anchor), and six cartridges -- all hand-pixelled here with
Pillow in Create's own flat, per-column-shaded item-sprite style (`tools/icon.py`'s own cartridge
precedent), nothing read from any game or copied asset (`docs/spec/operations/compliance.md`
`COMP-REQ-002`).

Layering approach (`docs/spec/04-architecture.md` `ARCH-DEC-006`): a slot's attachment overlay is
drawn on a transparent canvas the same 32x16 size as every base weapon texture, with the glyph
placed at that slot's own fixed anchor -- not baked into a per-weapon composite, so the same 22
overlay textures stack under `minecraft:composite` against any of the six base layers regardless of
which weapon it is. `items/weapon.json` and its five `items/attachment_<slot>.json` siblings then
wire these into the `minecraft:select`/`minecraft:condition` tree the 26.2 item-model schema
defines (research `smithing-and-item-model-layers-26-2.md` §D; `ARCH-DEC-006`).

Deterministic, committed output: run `python3 tools/sprites.py` (or `just sprites` once wired) and
commit whatever changes under `src/main/resources/assets/firearms/{items,models,textures}`.
"""
from __future__ import annotations

import json
from pathlib import Path

from PIL import Image

from palette import BLACK, BRASS, COPPER, DARK_OAK, GLASS, GUNMETAL, OAK, POLYMER, RED, \
    RED_DOT, SPRUCE, STEEL, Ramp

ASSETS = Path("src/main/resources/assets/firearms")
ITEMS = ASSETS / "items"
MODELS = ASSETS / "models" / "item"
TEXTURES = ASSETS / "textures" / "item"
NS = "firearms"

TRANSPARENT = (0, 0, 0, 0)
CANVAS = (32, 16)  # every base weapon texture, and every weapon-layer overlay, share this size.
ICON = (16, 16)  # every standalone attachment icon and cartridge.

# ---------------------------------------------------------------- the roster (`ItemRegistration`,
# `WeaponClass`, `Slot`, `Caliber`, the six `data/firearms/weapon/*.json` files)

SLOTS = ("muzzle", "optic", "magazine", "grip", "stock")

WEAPON_CLASSES = {
    "pistol": ("muzzle", "optic", "magazine"),
    "smg": ("muzzle", "optic", "magazine", "grip", "stock"),
    "assault_rifle": ("muzzle", "optic", "magazine", "grip", "stock"),
    "dmr": ("muzzle", "optic", "magazine", "stock"),
    "sniper_rifle": ("muzzle", "optic", "magazine", "stock"),
    "shotgun": ("muzzle", "magazine"),
}

# weapon id -> (class, body colour, accent colour, body span (x0, x1 exclusive))
WEAPONS = {
    "m1911": ("pistol", (86, 88, 94, 255), (120, 78, 46, 255), (9, 22)),
    "micro_uzi": ("smg", (34, 35, 39, 255), (18, 18, 21, 255), (6, 24)),
    "akm": ("assault_rifle", (78, 80, 86, 255), (150, 108, 58, 255), (4, 25)),
    "ruger_mini_14": ("dmr", (54, 60, 70, 255), (128, 92, 50, 255), (3, 25)),
    "awm": ("sniper_rifle", (70, 74, 78, 255), (92, 112, 66, 255), (2, 24)),
    "winchester_model_1897": ("shotgun", (72, 74, 80, 255), (140, 100, 54, 255), (4, 23)),
}

# body colour = receiver/slide steel; accent colour = this weapon's own wood/polymer/grip tone,
# reused across all its wood or polymer furniture so each weapon reads as one coherent material
# pairing rather than a single flat stick (Kevin, 2026-09-20 FA-21: the FA-9 pass read as flat
# sticks and grey blocks).

# slot -> (name, accent colour) -- every attachment in `data/firearms/attachment/*.json`. Kept
# bright enough to read against Minecraft's own light inventory background, not just against a
# dark preview: no "black-on-black" part, every attachment's own hue or value distinct from its
# slot siblings.
ATTACHMENTS = {
    "muzzle": {
        "suppressor": (58, 58, 64, 255),  # dark tube, matte -- longest of the three
        "flash_hider": (90, 60, 40, 255),  # dark bronze prongs
        "compensator": (96, 100, 110, 255),  # mid gunmetal block, slotted
    },
    "optic": {
        "scope_2x": (74, 96, 80, 255),
        "scope_3x": (74, 96, 80, 255),
        "scope_4x": (66, 88, 96, 255),
        "scope_6x": (66, 88, 96, 255),
        "scope_8x": (58, 70, 110, 255),
        "scope_15x": (58, 70, 110, 255),
        "red_dot": (70, 70, 76, 255),
        "holo": (86, 86, 96, 255),
    },
    "magazine": {
        "quickdraw_magazine": (108, 110, 118, 255),
        "extended_magazine": (90, 92, 100, 255),
        "extended_quickdraw_magazine": (90, 92, 100, 255),
    },
    "grip": {
        "light_grip": (68, 68, 74, 255),
        "half_grip": (74, 74, 80, 255),
        "angled_grip": (86, 108, 78, 255),
        "vertical_grip": (60, 60, 66, 255),
        "thumb_grip": (140, 100, 62, 255),
    },
    "stock": {
        "tactical_stock": (64, 64, 70, 255),
        "cheek_pad": (168, 128, 78, 255),
        "bullet_loops": (140, 100, 58, 255),
    },
}

# slot -> (x, y) top-left where every attachment glyph in that slot is pasted on the 32x16 canvas.
SLOT_ANCHOR = {
    "muzzle": (22, 5),
    "optic": (13, 1),
    "magazine": (14, 9),
    "grip": (8, 9),
    "stock": (0, 4),
}

# caliber -> (case rows, body width in px, tip ramp, boat-tail base); `Caliber`. Brass case always
# (`tools/palette.py` BRASS); the tip ramp is the only material that varies per calibre, per DEC-018
# ("short and fat", "short and slim", "slim with an olive tip", "longest with a boat-tail tip").
CARTRIDGES = {
    "acp_45": (5, 6, COPPER, False),  # .45 ACP: short and fat
    "mm_9": (5, 4, COPPER, False),  # 9mm: short and slim
    "mm_7_62": (7, 5, COPPER, False),  # 7.62: medium
    "mm_5_56": (6, 4, POLYMER, False),  # 5.56: slim, olive tip
    "magnum_300": (9, 5, COPPER, True),  # .300 Magnum: longest, boat-tail
}


def shade(colour: tuple[int, int, int, int], delta: int) -> tuple[int, int, int, int]:
    r, g, b, a = colour
    return (max(0, min(255, r + delta)), max(0, min(255, g + delta)), max(0, min(255, b + delta)), a)


def _outlined_block(img: Image.Image, x0: int, y0: int, x1: int, y1: int,
                     colour: tuple[int, int, int, int]) -> None:
    """A filled rectangle in Create's own flat item-sprite style (`tools/icon.py` precedent,
    the extracted-for-reference-only `create:wrench`/`create:precision_mechanism` look): a dark
    1px outline ring, a lightened highlight row just inside the top edge, flat fill between --
    never a copy of any reference pixel, just the same shading convention applied to our own
    geometry."""
    px = img.load()
    for x in range(x0, x1):
        for y in range(y0, y1):
            if not (0 <= x < img.width and 0 <= y < img.height):
                continue
            on_edge = y in (y0, y1 - 1) or x in (x0, x1 - 1)
            px[x, y] = shade(colour, -70) if on_edge else colour
    if y1 - y0 >= 3 and x1 - x0 >= 3:
        for x in range(x0 + 1, x1 - 1):
            if 0 <= x < img.width and 0 <= y0 + 1 < img.height:
                px[x, y0 + 1] = shade(colour, 35)


def _taper(img: Image.Image, x: int, y: int, rows: int, width: int,
           colour: tuple[int, int, int, int], dx: int = -1, shrink: int = 0) -> None:
    """A staircase of shrinking rows stepping sideways one column per row -- a curved magazine, an
    angled grip, a sloped comb, anything that reads as leaning rather than square."""
    px = img.load()
    for i in range(rows):
        w = max(1, width - shrink * i)
        rx = x + dx * i
        for xi in range(rx, rx + w):
            if 0 <= xi < img.width and 0 <= y + i < img.height:
                px[xi, y + i] = colour if i not in (0, rows - 1) else shade(colour, -30)


def _line(img: Image.Image, x0: int, y0: int, x1: int, y1: int,
          colour: tuple[int, int, int, int]) -> None:
    """A thin 1px straight accent line (gas tube, tube magazine, wire stock strut)."""
    px = img.load()
    dx = 1 if x1 >= x0 else -1
    dy = 1 if y1 >= y0 else -1
    steps = max(abs(x1 - x0), abs(y1 - y0), 1)
    for i in range(steps + 1):
        x = x0 + round((x1 - x0) * i / steps)
        y = y0 + round((y1 - y0) * i / steps)
        if 0 <= x < img.width and 0 <= y < img.height:
            px[x, y] = colour


# ---------------------------------------------------------------- base weapons (32x16)

# Every weapon shares one 4-row receiver/slide band (rows 6-9) so the fixed slot anchors --
# `SLOT_ANCHOR`, FA-9's, unchanged -- keep landing in the same sensible place (muzzle at the
# barrel, optic above the receiver, magazine/grip hanging off its bottom edge, stock at the rear)
# no matter which weapon's silhouette is drawn under them. What makes each weapon recognisable is
# everything added around that shared band: barrel length, furniture material and shape, and the
# one or two fixed (non-attachment) features called out in the ticket per weapon.
BODY_TOP, BODY_BOTTOM = 6, 10


def weapon_sprite(weapon_id: str) -> Image.Image:
    """Each of the six weapons gets its own hand-composed silhouette built from `_outlined_block`,
    `_taper` and `_line` -- a receiver/slide band common to all six (so the fixed per-slot anchors
    keep lining up), plus the weapon's own barrel length, material and one or two distinguishing
    fixed features named in the ticket. Own simple geometry throughout, not a copy of any real gun
    or any reference sprite's silhouette (`docs/spec/operations/compliance.md` `COMP-REQ-002`)."""
    weapon_class, body, accent, (x0, x1) = WEAPONS[weapon_id]
    img = Image.new("RGBA", CANVAS, TRANSPARENT)
    top, bottom = BODY_TOP, BODY_BOTTOM

    _outlined_block(img, x0, top, x1, bottom, body)

    if weapon_id == "m1911":
        # compact slide/frame, hammer spur, grip angled back and down.
        img.load()[x0 + 1, top - 1] = shade(body, -60)  # hammer spur
        _outlined_block(img, x0 + 3, bottom - 1, x0 + 6, bottom, shade(body, -20))  # trigger guard
        _taper(img, x0 + 1, bottom, 4, 5, accent, dx=-1, shrink=1)  # grip, angled back
        _line(img, x1, top, x1 + 2, top, shade(body, -40))  # short barrel, barely proud of the slide

    elif weapon_id == "micro_uzi":
        # stubby box receiver; the long straight magazine doubles as the grip (the real Uzi's own
        # magazine-in-grip layout), one tall straight housing, not two separate stubs; a folded
        # wire stock lies flat along the receiver's top rather than dangling below.
        _outlined_block(img, 13, bottom - 1, 16, 15, accent)  # long straight magazine-in-grip
        _line(img, x0 - 5, top, x0, top, shade(body, 25))  # folded wire stock, flat along the top
        _line(img, x0 - 5, top + 1, x0 - 5, bottom - 1, shade(body, 25))  # its rear hoop, dropping down
        _line(img, x1, top, x1 + 3, top, shade(body, -40))  # short barrel

    elif weapon_id == "akm":
        # curved magazine sweeping forward, wooden handguard and stock, gas tube over the barrel.
        _taper(img, 14, bottom, 6, 3, accent, dx=1, shrink=0)  # curved magazine, sweeps toward muzzle
        _outlined_block(img, x0 - 4, top, x0, bottom, accent)  # wooden stock block
        _outlined_block(img, x1 - 7, bottom - 1, x1 - 1, bottom, shade(accent, 20))  # wooden handguard
        _line(img, x1 - 8, top, x1 + 1, top, shade(body, -50))  # gas tube over the barrel
        _outlined_block(img, x0 + 2, bottom - 1, x0 + 5, bottom + 2, shade(body, -15))  # pistol grip
        _line(img, x1, top, x1 + 4, top, shade(body, -40))  # barrel

    elif weapon_id == "ruger_mini_14":
        # full wooden stock spanning the rear half, long barrel, small box magazine.
        _outlined_block(img, x0 - 3, top, x0 + 6, bottom + 2, accent)  # full wood stock, rear half
        _taper(img, x0 + 3, bottom + 2, 2, 6, accent, dx=1, shrink=2)  # comb taper into the butt
        _outlined_block(img, 14, bottom - 1, 17, bottom + 3, shade(body, -20))  # small box magazine
        _line(img, x1, top, x1 + 6, top, shade(body, -40))  # long barrel

    elif weapon_id == "awm":
        # long barrel, bolt handle, scope rail, green polymer stock, bipod stubs near the muzzle.
        _outlined_block(img, x0 - 2, top + 3, x0 + 5, bottom + 3, accent)  # green polymer stock
        _line(img, x0 + 3, bottom + 3, x0 + 1, bottom + 5, shade(accent, -30))  # angled toe
        _line(img, 10, top, 20, top, shade(body, -50))  # scope rail along the receiver top
        img.load()[18, top - 1] = shade(body, -60)  # bolt handle
        img.load()[18, top - 2] = shade(body, -60)
        img.load()[x1 - 4, bottom + 1] = shade(body, -50)  # bipod stub, left leg
        img.load()[x1 - 2, bottom + 1] = shade(body, -50)  # bipod stub, right leg
        _line(img, x1, top, x1 + 7, top, shade(body, -40))  # longest barrel

    else:  # winchester_model_1897
        # pump forend detached from the receiver line, tube magazine under the barrel, exposed
        # hammer, wooden stock.
        img.load()[x0 + 1, top - 1] = shade(body, -60)  # exposed hammer
        _outlined_block(img, x0 - 4, top, x0, bottom, accent)  # wooden stock block
        _line(img, x0 + 2, bottom - 1, x1 - 2, bottom - 1, (168, 150, 108, 255))  # tube magazine, brass
        _outlined_block(img, 10, bottom, 17, bottom + 2, shade(accent, -10))  # pump forend, dropped a row
        _line(img, x1, top, x1 + 5, top, shade(body, -40))  # barrel

    return img


# ---------------------------------------------------------------- attachment glyphs

def _rect(img: Image.Image, x0: int, y0: int, x1: int, y1: int, colour: tuple[int, int, int, int]) -> None:
    px = img.load()
    for x in range(x0, x1):
        for y in range(y0, y1):
            if 0 <= x < img.width and 0 <= y < img.height:
                px[x, y] = colour


def attachment_glyph(slot: str, name: str) -> Image.Image:
    """One small transparent-canvas glyph per attachment, shaped by its slot (a tube for muzzle, a
    scope/dot for optic, a curved box for magazine, an angled block for grip, a butt shape for
    stock) and coloured by its own accent (`ATTACHMENTS`) -- recognisably different by class and by
    colour, kept simple and readable rather than detailed."""
    img = Image.new("RGBA", CANVAS, TRANSPARENT)
    accent = ATTACHMENTS[slot][name]
    ax, ay = SLOT_ANCHOR[slot]

    if slot == "muzzle":
        width = {"suppressor": 9, "flash_hider": 5, "compensator": 6}[name]
        _rect(img, ax, ay, ax + width, ay + 3, shade(accent, 15))
        _rect(img, ax, ay + 1, ax + width, ay + 2, accent)
        _rect(img, ax + width - 2, ay, ax + width, ay + 3, shade(accent, -30))
        if name == "compensator":
            for x in range(ax + 1, ax + width - 1, 2):
                img.load()[x, ay + 1] = shade(accent, -50)
        if name == "flash_hider":
            for i in range(3):
                img.load()[ax + width, ay + i] = shade(accent, -10)

    elif slot == "optic":
        if name.startswith("scope_"):
            magnification = int(name.split("_")[1].rstrip("x"))
            length = 5 + min(4, magnification // 3)
            _rect(img, ax, ay + 2, ax + length, ay + 4, accent)
            _rect(img, ax + 1, ay, ax + 3, ay + 2, shade(accent, 10))  # rear turret
            _rect(img, ax + length - 3, ay, ax + length - 1, ay + 2, shade(accent, 10))  # front turret
            img.load()[ax + length - 1, ay + 3] = (60, 120, 200, 255)  # lens glint
        elif name == "red_dot":
            _rect(img, ax, ay + 1, ax + 4, ay + 4, accent)
            img.load()[ax + 1, ay + 2] = (200, 40, 40, 255)
        else:  # holo
            _rect(img, ax, ay, ax + 5, ay + 4, accent)
            _rect(img, ax + 1, ay + 1, ax + 4, ay + 3, (40, 150, 150, 255))

    elif slot == "magazine":
        height = {"quickdraw_magazine": 5, "extended_magazine": 7, "extended_quickdraw_magazine": 7}[name]
        _rect(img, ax, ay, ax + 3, ay + height, accent)
        _rect(img, ax, ay + height - 1, ax + 3, ay + height, shade(accent, -30))
        if name == "extended_quickdraw_magazine":
            _rect(img, ax, ay + 1, ax + 3, ay + 2, (150, 130, 70, 255))  # a banded accent stripe
        if "quickdraw" in name:
            img.load()[ax + 3, ay] = shade(accent, 30)  # the quickdraw tab, sticking out sideways
            img.load()[ax + 3, ay + 1] = shade(accent, 30)

    elif slot == "grip":
        # light: a thin post. half: a short post, only the lower half of a full grip. vertical: a
        # full post. angled: a wedge, leaning forward. thumb: a small rest with a notch.
        if name == "light_grip":
            _rect(img, ax, ay, ax + 2, ay + 4, accent)
        elif name == "half_grip":
            _rect(img, ax, ay + 3, ax + 3, ay + 6, accent)  # only the lower half, dropped down
            _rect(img, ax, ay + 5, ax + 3, ay + 6, shade(accent, -30))
        elif name == "vertical_grip":
            _rect(img, ax, ay, ax + 3, ay + 6, accent)
            _rect(img, ax, ay + 5, ax + 3, ay + 6, shade(accent, -30))
        elif name == "angled_grip":
            width, height = 5, 4
            for i in range(height):
                _rect(img, ax + i, ay + i, ax + i + width - i, ay + i + 1, accent)
        else:  # thumb_grip: a small rest with a notch cut for the thumb
            _rect(img, ax, ay, ax + 4, ay + 4, accent)
            img.load()[ax + 1, ay] = TRANSPARENT
            img.load()[ax + 1, ay + 1] = TRANSPARENT
            img.load()[ax + 3, ay] = shade(accent, 20)

    else:  # stock
        if name == "tactical_stock":
            _rect(img, ax, ay + 1, ax + 6, ay + 5, accent)  # a squared pad
            _rect(img, ax, ay + 1, ax + 1, ay + 5, shade(accent, -20))  # buttplate
        elif name == "cheek_pad":
            _rect(img, ax + 2, ay + 2, ax + 7, ay + 4, accent)  # the pad
            _rect(img, ax + 3, ay + 1, ax + 6, ay + 2, shade(accent, 15))  # raised above the comb line
        else:  # bullet_loops
            _rect(img, ax, ay + 2, ax + 8, ay + 3, (74, 54, 32, 255))  # the strap
            for i, x in enumerate(range(ax + 1, ax + 8, 2)):
                img.load()[x, ay + 2] = accent

    return img


# ---------------------------------------------------------------- standalone attachment icons and
# cartridges (16x16), round two (`DEC-018-art-direction.md`, `WEAPON-REQ-017`): drawn independently
# of `attachment_glyph` above (which stays as FA-22's composite-layer path, untouched), in the
# vanilla/Create habit -- a chunky object filling roughly 10-14px of the canvas, a one-pixel outline
# in the material's own `outline` tone, `light` on the top/left faces, `base` fill, `shade` on the
# bottom/right faces, no gradients, no isolated pixels, coloured only from `tools/palette.py`'s
# ramps (never a hand-picked RGB literal).

def _set(img: Image.Image, x: int, y: int, colour: tuple[int, int, int, int]) -> None:
    if 0 <= x < img.width and 0 <= y < img.height:
        img.load()[x, y] = colour


def _block(img: Image.Image, x0: int, y0: int, x1: int, y1: int, ramp: Ramp) -> None:
    """An outlined, top-left-lit rectangular block in a material ramp: a one-pixel `outline` ring,
    `light` on the top/left interior pixels, `shade` on the bottom/right interior pixels, `base`
    fill between -- the one shading routine every boxy icon part (mag bodies, sight housings, grip
    blocks, pads) is built from, so every part reads by the same rule DEC-018 names."""
    px = img.load()
    for x in range(x0, x1):
        for y in range(y0, y1):
            if not (0 <= x < img.width and 0 <= y < img.height):
                continue
            if x in (x0, x1 - 1) or y in (y0, y1 - 1):
                colour = ramp.outline
            elif x == x0 + 1 or y == y0 + 1:
                colour = ramp.light
            elif x == x1 - 2 or y == y1 - 2:
                colour = ramp.shade
            else:
                colour = ramp.base
            px[x, y] = colour


def _h_tube(img: Image.Image, x0: int, y0: int, length: int, thickness: int, ramp: Ramp,
            bulge_front: bool = False, bulge_rear: bool = False,
            rings: dict[int, Ramp] | None = None) -> None:
    """A horizontal cylinder, column by column, its own outline/light/base/shade rows, with an
    optional wider `bulge` (a scope's objective/ocular bell) at either end -- the muzzle-device and
    scope precedent, alongside vanilla's spyglass tube. `rings` swaps one column's ramp (a brass
    band wrapping the tube at that column's own local thickness, bulge included) rather than
    stamping a fixed row, so a ring always reads as wrapping the barrel, never floating off it."""
    px = img.load()
    rings = rings or {}
    mid = y0 + thickness // 2
    for i in range(length):
        x = x0 + i
        t = thickness
        if bulge_front and i >= length - 3:
            t = thickness + 2
        if bulge_rear and i < 3:
            t = thickness + 2
        ry0, ry1 = mid - t // 2, mid - t // 2 + t
        col_ramp = rings.get(i, ramp)
        for y in range(ry0, ry1):
            if not (0 <= x < img.width and 0 <= y < img.height):
                continue
            if y in (ry0, ry1 - 1) or i in (0, length - 1):
                colour = col_ramp.outline
            elif y == ry0 + 1:
                colour = col_ramp.light
            elif y == ry1 - 2:
                colour = col_ramp.shade
            else:
                colour = col_ramp.base
            px[x, y] = colour


def _stairs(img: Image.Image, x: int, y: int, rows: int, width: int, ramp: Ramp,
            dx: int = 1, shrink: int = 0) -> None:
    """A staircase of `_block`-shaded rows stepping sideways one column per row -- a wedge leaning
    forward (the angled grip), a comb sloping into a butt."""
    for i in range(rows):
        w = max(1, width - shrink * i)
        rx = x + dx * i
        _block(img, rx, y + i, rx + w, y + i + 1, ramp)


def attachment_icon(slot: str, name: str) -> Image.Image:
    """The attachment's own standalone item-stack icon, drawn fresh at 16x16 in the vanilla/Create
    style rather than cropped from the weapon-layer glyph: a small, chunky, recognisable object
    (a tube, a box, a post) filling most of the canvas, shaded per `Ramp` and coloured only from
    `tools/palette.py` (`WEAPON-REQ-017`, `DEC-018`)."""
    img = Image.new("RGBA", ICON, TRANSPARENT)

    if slot == "muzzle":
        if name == "suppressor":
            # a fat matte-gunmetal tube with a lighter steel end cap ring at each end -- the
            # vanilla spyglass's own banded-tube precedent, not a single flat bar.
            _h_tube(img, 2, 5, 12, 5, STEEL)
            _h_tube(img, 4, 5, 8, 5, GUNMETAL)  # matte body between the two caps
        elif name == "flash_hider":
            # a cone opening toward the muzzle (right), three tines beyond it -- gapped rows, not
            # a solid block, so each prong reads separately.
            x0, cone_len = 3, 5
            for i in range(cone_len):
                t = 3 + i // 2  # 3,3,4,4,5: widening toward the muzzle
                ry0 = 7 - t // 2
                for y in range(ry0, ry0 + t):
                    edge = y in (ry0, ry0 + t - 1) or i in (0, cone_len - 1)
                    colour = STEEL.outline if edge else (STEEL.light if y == ry0 + 1 else STEEL.base)
                    _set(img, x0 + i, y, colour)
            tip_ry0, tip_t = 7 - 5 // 2, 5
            for y in (tip_ry0, tip_ry0 + tip_t // 2, tip_ry0 + tip_t - 1):
                _set(img, x0 + cone_len, y, STEEL.outline)
                _set(img, x0 + cone_len + 1, y, STEEL.outline)
        else:  # compensator
            # a short steel block with two separate copper vent ports on top, not one merged bar.
            _h_tube(img, 3, 6, 8, 4, STEEL)
            for cx in (5, 8):
                _set(img, cx, 7, COPPER.base)
                _set(img, cx, 8, COPPER.shade)

    elif slot == "optic":
        if name.startswith("scope_"):
            magnification = int(name.split("_")[1].rstrip("x"))
            if magnification <= 4:
                length = {2: 7, 3: 8, 4: 9}[magnification]
                bulge_front = bulge_rear = False
                ring_cols = (length - 3,)
            elif magnification <= 8:
                length = {6: 10, 8: 11}[magnification]
                bulge_front, bulge_rear = True, False
                ring_cols = (2, length - 4)
            else:
                length, bulge_front, bulge_rear = 13, True, True
                ring_cols = (2, length // 2, length - 4)
            x0 = 8 - length // 2
            rings = {r: BRASS for r in ring_cols}
            _h_tube(img, x0, 5, length, 4, STEEL, bulge_front=bulge_front, bulge_rear=bulge_rear,
                    rings=rings)
            _set(img, x0 + length - 2, 6, GLASS.light)
            _set(img, x0 + length - 2, 7, GLASS.base)  # objective lens, two panes wide
        elif name == "red_dot":
            _block(img, 6, 5, 11, 10, BLACK)
            _block(img, 7, 6, 10, 9, GLASS)
            _set(img, 8, 7, RED_DOT)
            _set(img, 7, 10, STEEL.outline)
            _set(img, 9, 10, STEEL.outline)  # mount legs
        else:  # holo
            _block(img, 4, 5, 12, 10, BLACK)
            _block(img, 5, 6, 11, 9, GLASS)
            _set(img, 7, 7, GLASS.light)
            _set(img, 8, 7, GLASS.light)
            _set(img, 5, 10, STEEL.outline)
            _set(img, 10, 10, STEEL.outline)  # mount legs

    elif slot == "magazine":
        if name == "quickdraw_magazine":
            _block(img, 6, 4, 10, 10, STEEL)
            _block(img, 6, 10, 10, 12, BRASS)
        elif name == "extended_magazine":
            _block(img, 6, 3, 10, 13, STEEL)
        else:  # extended_quickdraw_magazine
            _block(img, 6, 3, 10, 12, STEEL)
            _block(img, 6, 12, 10, 14, BRASS)

    elif slot == "grip":
        if name == "light_grip":
            _block(img, 7, 6, 9, 12, BLACK)
        elif name == "half_grip":
            _block(img, 5, 7, 11, 12, BLACK)
        elif name == "angled_grip":
            _stairs(img, 5, 6, 6, 5, POLYMER, dx=1, shrink=0)
        elif name == "vertical_grip":
            _block(img, 7, 4, 10, 12, BLACK)
            for y in (6, 8, 10):
                _set(img, 8, y, BLACK.outline)  # grooves
        else:  # thumb_grip
            _block(img, 5, 8, 10, 12, DARK_OAK)
            _set(img, 6, 8, TRANSPARENT)
            _set(img, 6, 9, TRANSPARENT)  # thumb notch

    else:  # stock
        if name == "tactical_stock":
            _block(img, 2, 5, 5, 11, BLACK)  # buttplate
            for x in range(5, 13):
                _set(img, x, 5, BLACK.outline)
                _set(img, x, 10, BLACK.outline)  # skeleton rails, open middle
            _set(img, 12, 5, BLACK.outline)
            _set(img, 12, 6, BLACK.outline)
            _set(img, 12, 9, BLACK.outline)
            _set(img, 12, 10, BLACK.outline)  # interface strut
        elif name == "cheek_pad":
            _block(img, 3, 6, 13, 10, SPRUCE)
            for x in range(4, 12):
                _set(img, x, 7, SPRUCE.outline)
                _set(img, x, 9, SPRUCE.outline)  # two straps
        else:  # bullet_loops
            _block(img, 2, 8, 14, 11, OAK)  # leather strip, tanned rather than the dark-oak grip's
            for x in (3, 5, 7, 9, 11):
                _set(img, x, 6, BRASS.outline)
                _set(img, x, 7, BRASS.base)
                _set(img, x, 8, BRASS.base)  # five cartridges standing in their loops

    return img


# ---------------------------------------------------------------- cartridges (16x16), a vanilla-
# style cartridge standing upright per calibre, plus a distinct 12-gauge shotshell.

def cartridge_sprite(case_rows: int, width: int, tip_ramp: Ramp, boat_tail: bool = False) -> Image.Image:
    img = Image.new("RGBA", ICON, TRANSPARENT)
    cx = 8
    x0, x1 = cx - width // 2, cx - width // 2 + width
    top = 14 - case_rows - 4

    _set(img, cx - 1 if width > 3 else x0, top, tip_ramp.outline)
    _set(img, cx, top, tip_ramp.outline)  # pointed nose, narrower than the body
    _block(img, x0, top + 1, x1, top + 4, tip_ramp)  # bullet tip/ogive
    for x in range(x0, x1):
        _set(img, x, top + 4, BRASS.outline)  # cannelure
    case_y0 = top + 5
    if boat_tail:
        _block(img, x0, case_y0, x1, case_y0 + case_rows - 1, BRASS)
        _block(img, x0 + 1, case_y0 + case_rows - 1, x1 - 1, case_y0 + case_rows, BRASS)
    else:
        _block(img, x0, case_y0, x1, case_y0 + case_rows, BRASS)
    rim_y = case_y0 + case_rows
    _block(img, x0, rim_y, x1, rim_y + 1, BRASS)
    _set(img, cx - 1, rim_y, BRASS.outline)
    _set(img, cx, rim_y, BRASS.outline)  # primer
    return img


def shotshell_sprite() -> Image.Image:
    """12 gauge: a squat red hull with a brass head, not a bottlenecked bullet -- `Caliber.GAUGE_12`'s
    own shape."""
    img = Image.new("RGBA", ICON, TRANSPARENT)
    _block(img, 4, 2, 12, 11, RED)
    for x in range(4, 12):
        _set(img, x, 2, RED.outline)  # crimped top
    _block(img, 4, 11, 12, 14, BRASS)
    _set(img, 7, 12, BRASS.outline)
    _set(img, 8, 12, BRASS.outline)  # primer
    return img


# ---------------------------------------------------------------- model/item-definition JSON

def write_json(path: Path, data: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n")


def generated_model(texture_ref: str) -> dict:
    return {"parent": "minecraft:item/generated", "textures": {"layer0": texture_ref}}


def model_ref(model_path: str) -> dict:
    return {"type": "minecraft:model", "model": f"{NS}:{model_path}"}


def save_png(img: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path, optimize=True)


def main() -> None:
    written: list[Path] = []

    # Base weapons.
    for weapon_id in WEAPONS:
        save_png(weapon_sprite(weapon_id), TEXTURES / "weapon" / f"{weapon_id}.png")
        write_json(MODELS / "weapon" / f"{weapon_id}.json", generated_model(f"{NS}:item/weapon/{weapon_id}"))
        written.append(TEXTURES / "weapon" / f"{weapon_id}.png")

    # Attachments: standalone icon + weapon-layer overlay, one glyph reused for both.
    for slot, names in ATTACHMENTS.items():
        for name in names:
            save_png(attachment_icon(slot, name), TEXTURES / "attachment" / f"{name}.png")
            write_json(MODELS / "attachment" / f"{name}.json", generated_model(f"{NS}:item/attachment/{name}"))

            save_png(attachment_glyph(slot, name), TEXTURES / "weapon" / "layer" / f"{slot}_{name}.png")
            write_json(MODELS / "weapon" / "layer" / f"{slot}_{name}.json",
                       generated_model(f"{NS}:item/weapon/layer/{slot}_{name}"))
            written.append(TEXTURES / "attachment" / f"{name}.png")

    # Cartridges.
    for caliber, (rows, width, tip_ramp, boat_tail) in CARTRIDGES.items():
        save_png(cartridge_sprite(rows, width, tip_ramp, boat_tail), TEXTURES / "cartridge" / f"{caliber}.png")
        write_json(MODELS / "cartridge" / f"{caliber}.json", generated_model(f"{NS}:item/cartridge/{caliber}"))
    save_png(shotshell_sprite(), TEXTURES / "cartridge" / "gauge_12.png")
    write_json(MODELS / "cartridge" / "gauge_12.json", generated_model(f"{NS}:item/cartridge/gauge_12"))
    written.append(TEXTURES / "cartridge" / "gauge_12.png")

    # items/attachment_<slot>.json: select on that slot's own component -> that attachment's icon.
    for slot, names in ATTACHMENTS.items():
        cases = [{"when": f"{NS}:{name}", "model": model_ref(f"item/attachment/{name}")} for name in names]
        first = next(iter(names))
        write_json(ITEMS / f"attachment_{slot}.json", {
            "model": {
                "type": "minecraft:select",
                "property": "minecraft:component",
                "component": f"{NS}:attachment_{slot}",
                "cases": cases,
                "fallback": model_ref(f"item/attachment/{first}"),
            }
        })

    # items/cartridge_<caliber>.json: plain.
    for caliber in list(CARTRIDGES) + ["gauge_12"]:
        write_json(ITEMS / f"cartridge_{caliber}.json", {"model": model_ref(f"item/cartridge/{caliber}")})

    # items/weapon.json: select on firearms:base -> per-base composite of base layer + slot layers.
    weapon_cases = []
    for weapon_id, (weapon_class, *_rest) in WEAPONS.items():
        base_model = model_ref(f"item/weapon/{weapon_id}")
        base_layer = {
            "type": "minecraft:condition",
            "property": "minecraft:using_item",
            "on_true": {**base_model, "transformation": {"translation": [0.0, 0.05, -0.1]}},
            "on_false": base_model,
        }
        slot_layers = []
        for slot in SLOTS:
            if slot not in WEAPON_CLASSES[weapon_class]:
                continue
            slot_cases = [
                {"when": f"{NS}:{name}", "model": model_ref(f"item/weapon/layer/{slot}_{name}")}
                for name in ATTACHMENTS[slot]
            ]
            slot_layers.append({
                "type": "minecraft:condition",
                "property": "minecraft:has_component",
                "component": f"{NS}:attachment_{slot}",
                "on_true": {
                    "type": "minecraft:select",
                    "property": "minecraft:component",
                    "component": f"{NS}:attachment_{slot}",
                    "cases": slot_cases,
                    "fallback": {"type": "minecraft:empty"},
                },
                "on_false": {"type": "minecraft:empty"},
            })
        weapon_cases.append({
            "when": {"weapon_id": f"{NS}:{weapon_id}"},
            "model": {"type": "minecraft:composite", "models": [base_layer, *slot_layers]},
        })

    write_json(ITEMS / "weapon.json", {
        "model": {
            "type": "minecraft:select",
            "property": "minecraft:component",
            "component": f"{NS}:base",
            "cases": weapon_cases,
            "fallback": model_ref("item/weapon/m1911"),
        }
    })

    print(f"wrote {len(written)} sampled paths; every base, attachment, cartridge and wiring file is under {ASSETS}")


if __name__ == "__main__":
    main()
