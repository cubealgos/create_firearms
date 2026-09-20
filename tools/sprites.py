#!/usr/bin/env python3
"""Draw every FA-9 item sprite and write the item model files that reference them: six base-weapon
silhouettes, 22 attachment glyphs (each both a standalone 16x16 icon and a 32x16 weapon-layer
overlay pre-positioned at its slot's fixed anchor), and six cartridges -- nothing read from any game
or copied asset (`docs/spec/operations/compliance.md` `COMP-REQ-002`). The 28 standalone attachment
icons and cartridges (`attachment_icon`, `cartridge_sprite`, `shotshell_sprite`) are, as of round
five, plain decodes of the hand-authored grids in `pixel_art.py` -- see that module's own docstring
for the construction rules and why the mask/capsule geometry that used to live here is gone. The six
base-weapon silhouettes and the 22 weapon-layer overlays (`weapon_sprite`, `attachment_glyph`) are
unrelated, own code, untouched by round five.

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

import pixel_art

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

# every caliber id (`Caliber`) except the shotshell, which is drawn by `shotshell_sprite()`
# instead -- each one is its own hand-authored grid in `pixel_art.py` (round five), no longer a
# geometry tuple: .45 ACP short and fat, 9mm short and slim, 7.62 medium, 5.56 slim with an olive
# tip, .300 Magnum longest with a boat-tail base (DEC-018).
CARTRIDGES = ("acp_45", "mm_9", "mm_7_62", "mm_5_56", "magnum_300")


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
# cartridges (16x16), round five (Kevin, round-four review: "they are better but all still bad;
# take a look at the spyglass versus our scopes"). Every one of these 28 icons is now a
# hand-authored 16x16 pixel grid in `pixel_art.py` -- these three functions do nothing but decode a
# grid into an image, pixel by pixel; no capsule, mask or box geometry lives here any more (that
# machinery, round three and four's own, only ever served these three functions and is gone with
# them -- `attachment_glyph` above, `weapon_sprite` and everything below keep their own,
# independent code unchanged). See `pixel_art.py`'s own docstring for the construction rules every
# grid follows.

def _decode(name: str) -> Image.Image:
    rows, legend = pixel_art.ICONS[name]
    img = Image.new("RGBA", ICON, TRANSPARENT)
    px = img.load()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                px[x, y] = legend[ch]
    return img


def attachment_icon(slot: str, name: str) -> Image.Image:
    """The attachment's own standalone item-stack icon: `pixel_art.ICONS[name]`, decoded verbatim.
    `slot` isn't needed to find the grid (every attachment name is unique across slots) but is kept
    in the signature so every call site still reads `attachment_icon(slot, name)` next to its
    sibling `attachment_glyph(slot, name)` above."""
    del slot
    return _decode(name)


# ---------------------------------------------------------------- cartridges (16x16): the same
# decode, keyed by caliber id.

def cartridge_sprite(caliber: str) -> Image.Image:
    return _decode(caliber)


def shotshell_sprite() -> Image.Image:
    """12 gauge: `pixel_art.ICONS["gauge_12"]`, decoded verbatim."""
    return _decode("gauge_12")


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

    # Base weapons and the weapon-layer overlays are `tools/models.py`'s own cuboid models and
    # atlases since `decisions/DEC-018-art-direction.md` (FA-22); this generator no longer writes
    # `models/item/weapon/**` or `textures/item/weapon/**` for the base or the slot layers.

    # Attachments: standalone icon only (the weapon-layer overlay moved to `tools/models.py`).
    for slot, names in ATTACHMENTS.items():
        for name in names:
            save_png(attachment_icon(slot, name), TEXTURES / "attachment" / f"{name}.png")
            write_json(MODELS / "attachment" / f"{name}.json", generated_model(f"{NS}:item/attachment/{name}"))
            written.append(TEXTURES / "attachment" / f"{name}.png")

    # Cartridges.
    for caliber in CARTRIDGES:
        save_png(cartridge_sprite(caliber), TEXTURES / "cartridge" / f"{caliber}.png")
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

    # items/weapon.json is `tools/models.py`'s own since FA-22 (DEC-018): it composites the cuboid
    # base and part models, not these flat layers, so it is generated there, not here.

    print(f"wrote {len(written)} sampled paths; every attachment icon, cartridge and wiring file is under {ASSETS}")


if __name__ == "__main__":
    main()
