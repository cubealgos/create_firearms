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
    "m1911": ("pistol", (74, 76, 82, 255), (48, 49, 54, 255), (9, 24)),
    "micro_uzi": ("smg", (40, 41, 46, 255), (22, 22, 26, 255), (5, 27)),
    "akm": ("assault_rifle", (96, 78, 52, 255), (58, 46, 30, 255), (3, 30)),
    "ruger_mini_14": ("dmr", (112, 90, 58, 255), (70, 55, 35, 255), (2, 31)),
    "awm": ("sniper_rifle", (74, 84, 68, 255), (48, 55, 44, 255), (1, 31)),
    "winchester_model_1897": ("shotgun", (100, 74, 44, 255), (60, 44, 26, 255), (3, 29)),
}

# slot -> (name, accent colour) -- every attachment in `data/firearms/attachment/*.json`. Kept
# bright enough to read against Minecraft's own light inventory background, not just against a
# dark preview: no "black-on-black" part, every attachment's own hue or value distinct from its
# slot siblings.
ATTACHMENTS = {
    "muzzle": {
        "suppressor": (172, 172, 180, 255),  # pale gunmetal tube
        "flash_hider": (90, 60, 40, 255),  # dark bronze prongs
        "compensator": (96, 100, 110, 255),  # mid gunmetal block
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

# caliber -> (case length in the 6-tall body block, tip colour, case colour); `Caliber`.
CARTRIDGES = {
    "acp_45": (7, (196, 122, 69, 255), (205, 170, 80, 255)),
    "mm_9": (5, (196, 122, 69, 255), (215, 186, 100, 255)),
    "mm_7_62": (8, (166, 100, 43, 255), (188, 152, 66, 255)),
    "mm_5_56": (6, (110, 140, 90, 255), (188, 152, 66, 255)),
    "magnum_300": (9, (140, 82, 34, 255), (163, 128, 50, 255)),
}


def shade(colour: tuple[int, int, int, int], delta: int) -> tuple[int, int, int, int]:
    r, g, b, a = colour
    return (max(0, min(255, r + delta)), max(0, min(255, g + delta)), max(0, min(255, b + delta)), a)


# ---------------------------------------------------------------- base weapons (32x16)

def weapon_sprite(weapon_id: str) -> Image.Image:
    """A simple flat side-view silhouette: a body bar with a highlighted top edge and a shadowed
    bottom edge (Create's own flat per-row shading), a barrel stub toward the muzzle end, and for a
    class with a stock/grip, a dropped-down rear/underside block. Recognisably different per weapon
    by body span, height and colour, not by fine detail -- `docs/spec/operations/compliance.md`
    keeps this our own simple geometry, not a copy of any real silhouette."""
    weapon_class, body, accent, (x0, x1) = WEAPONS[weapon_id]
    img = Image.new("RGBA", CANVAS, TRANSPARENT)
    px = img.load()

    top, bottom = 6, 9  # the receiver/body band, three rows tall
    for x in range(x0, x1):
        px[x, top] = shade(body, 35)
        for y in range(top + 1, bottom):
            px[x, y] = body
        px[x, bottom] = shade(body, -35)

    # barrel: a thin two-row stub reaching toward the right edge.
    barrel_end = min(31, x1 + 3)
    for x in range(x1 - 2, barrel_end):
        px[x, top] = shade(accent, 20)
        px[x, top + 1] = accent

    # grip/stock classes drop a block below the body toward the left of the span.
    if weapon_class in ("pistol", "smg", "assault_rifle"):
        gx0 = x0 + 2
        gx1 = min(x1, gx0 + 4)
        for x in range(gx0, gx1):
            for y in range(bottom + 1, min(15, bottom + 5)):
                px[x, y] = accent if y < 14 else shade(accent, -25)

    # a stocked class (dmr, sniper_rifle, shotgun) extends a narrow butt to the left edge.
    if weapon_class in ("dmr", "sniper_rifle", "shotgun"):
        for x in range(0, x0):
            px[x, top + 1] = shade(accent, 15)
            px[x, top + 2] = accent

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

    elif slot == "grip":
        shapes = {
            "light_grip": (3, 4),
            "half_grip": (3, 5),
            "vertical_grip": (3, 6),
            "angled_grip": (5, 4),
            "thumb_grip": (4, 4),
        }
        width, height = shapes[name]
        if name == "angled_grip":
            for i in range(height):
                _rect(img, ax + i, ay + i, ax + i + width - i, ay + i + 1, accent)
        else:
            _rect(img, ax, ay, ax + width, ay + height, accent)
            if name == "thumb_grip":
                img.load()[ax + width, ay] = shade(accent, 20)

    else:  # stock
        if name == "tactical_stock":
            _rect(img, ax, ay + 1, ax + 6, ay + 3, accent)
            _rect(img, ax, ay, ax + 1, ay + 5, shade(accent, -20))  # buttplate
        elif name == "cheek_pad":
            _rect(img, ax + 2, ay + 1, ax + 7, ay + 3, accent)
        else:  # bullet_loops
            _rect(img, ax, ay + 2, ax + 8, ay + 3, (74, 54, 32, 255))  # the strap
            for i, x in enumerate(range(ax + 1, ax + 8, 2)):
                img.load()[x, ay + 2] = accent

    return img


def attachment_icon(slot: str, name: str) -> Image.Image:
    """The glyph re-anchored onto its own 16x16 canvas, centred -- `items/attachment_<slot>.json`'s
    own icon for the attachment item stack itself, independent of any weapon layer."""
    layer = attachment_glyph(slot, name)
    bbox = layer.getbbox()
    if bbox is None:
        return Image.new("RGBA", ICON, TRANSPARENT)
    glyph = layer.crop(bbox)
    icon = Image.new("RGBA", ICON, TRANSPARENT)
    x = (ICON[0] - glyph.width) // 2
    y = (ICON[1] - glyph.height) // 2
    icon.paste(glyph, (x, y), glyph)
    return icon


# ---------------------------------------------------------------- cartridges (16x16), the
# `tools/icon.py` cartridge precedent, parametrised per calibre, plus a distinct 12-gauge shotshell.

def cartridge_sprite(case_rows: int, tip: tuple[int, int, int, int], case: tuple[int, int, int, int]) -> Image.Image:
    img = Image.new("RGBA", ICON, TRANSPARENT)
    columns = range(5, 11)
    top = 14 - case_rows - 5

    def row(y: int, colour: tuple[int, int, int, int], shrink: bool = False) -> None:
        cols = list(columns)[1:-1] if shrink else list(columns)
        for i, x in enumerate(cols):
            img.load()[x, y] = shade(colour, -6 * i)

    row(top, tip, shrink=True)
    for y in range(top + 1, top + 5):
        row(y, tip)
    row(top + 5, (74, 54, 32, 255))  # cannelure
    for y in range(top + 6, top + 6 + case_rows):
        row(y, case)
    row(13, case)  # the rim
    img.load()[7, 13] = (78, 45, 24, 255)
    img.load()[8, 13] = (78, 45, 24, 255)
    return img


def shotshell_sprite() -> Image.Image:
    """12 gauge: a squat hull, not a bottlenecked bullet -- `Caliber.GAUGE_12`'s own shape."""
    img = Image.new("RGBA", ICON, TRANSPARENT)
    hull = (196, 88, 40, 255)
    brass = (188, 152, 66, 255)
    for y in range(2, 11):
        for i, x in enumerate(range(4, 12)):
            img.load()[x, y] = shade(hull, -4 * i)
    for y in range(11, 14):
        for i, x in enumerate(range(4, 12)):
            img.load()[x, y] = shade(brass, -4 * i)
    img.load()[7, 12] = (78, 45, 24, 255)
    img.load()[8, 12] = (78, 45, 24, 255)
    for x in range(4, 12):
        img.load()[x, 1] = shade(hull, -20)  # the crimped top
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
    for caliber, (rows, tip, case) in CARTRIDGES.items():
        save_png(cartridge_sprite(rows, tip, case), TEXTURES / "cartridge" / f"{caliber}.png")
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
