#!/usr/bin/env python3
"""Cuboid weapon and attachment-part item models (WEAPON-REQ-016, `decisions/DEC-018-art-direction.md`).

Every base weapon becomes a Blockbench-style cuboid element model with its own texture atlas,
the way Create renders its potato cannon, and every mounted attachment becomes a cuboid part
model positioned at its slot's anchor in model units. This module owns everything FA-9/FA-21's
`sprites.py` used to own under `models/item/weapon/**` and `items/weapon.json`; `sprites.py`
keeps drawing the standalone attachment icons, cartridges and their own item definitions
untouched, because FA-21 round two edits those in parallel on another branch.

The box DSL: each weapon and each attachment part is a list of `Box(from_xyz, to_xyz, material)`
in model units (16 units = 1 block), optionally with a `rotation` (a single-axis Blockbench-style
element rotation, angle one of -45/-22.5/0/22.5/45) or a `face_material` override for a detail
(a scope's lens face, a compensator's copper port, the red dot's reticle). The weapon lies along
the Z axis exactly as Create's cannon does: -Z is the stock/grip end, near the player's hand,
+Z is the muzzle end.

The atlas packer gives every box face its own pixel rectangle -- one texel per model unit -- in a
per-weapon or per-part PNG, shelf-packed into the smallest power-of-two square that fits, painted
from `tools/palette.py`'s ramps: `light` on the up face and the face toward the light (this
module's convention: west), `base` on the remaining side face (north), `shade` on down/east/south,
and a one-texel `outline` border on every face -- no gradients, no anti-aliasing (`WEAPON-REQ-017`).

Deterministic, committed output: run `python3 tools/models.py` and commit whatever changes under
`src/main/resources/assets/firearms/{items,models,textures}`. A preview sheet (side-view
projections of every weapon, bare and fully loaded, plus the atlases themselves) is written
alongside for review.
"""
from __future__ import annotations

import copy
import json
from dataclasses import dataclass, field
from pathlib import Path

from PIL import Image, ImageDraw

from palette import RAMPS, RED_DOT, Ramp, TRANSPARENT
from sprites import (
    ASSETS,
    ATTACHMENTS,
    ITEMS,
    MODELS,
    NS,
    SLOTS,
    TEXTURES,
    WEAPON_CLASSES,
    WEAPONS,
    model_ref,
    save_png,
    write_json,
)

SHEET_PATH = Path(
    "/private/tmp/claude-501/-Users-kevin-Documents-git-personal-create-civilization/"
    "bb7bc244-01b6-4890-9ca3-da5d384dc21e/scratchpad/fa22-round1.png"
)

FACES = ("north", "south", "east", "west", "up", "down")

# Lighting convention (WEAPON-REQ-017, DEC-018): up and west are lit; north is the one plain "base"
# side face; down, east and south are in shade. Kept as one fixed table so every box, weapon and
# part is shaded the same way regardless of which function built it.
FACE_TONE = {"up": "light", "west": "light", "north": "base", "south": "shade", "east": "shade", "down": "shade"}


@dataclass(frozen=True)
class Box:
    frm: tuple[float, float, float]
    to: tuple[float, float, float]
    material: str
    face_material: dict[str, str] = field(default_factory=dict)
    rotation: dict | None = None  # {"angle": ..., "axis": "x"|"y"|"z", "origin": (x, y, z)}


def _shade(colour: tuple[int, int, int, int], delta: int) -> tuple[int, int, int, int]:
    r, g, b, a = colour
    return (max(0, min(255, r + delta)), max(0, min(255, g + delta)), max(0, min(255, b + delta)), a)


EXTRA_RAMPS: dict[str, Ramp] = {
    # The red dot's own reticle: not a structural material, just an accent tone (`RED_DOT`).
    "red_dot": Ramp(outline=_shade(RED_DOT, -90), shade=_shade(RED_DOT, -40), base=RED_DOT, light=_shade(RED_DOT, 40)),
}


def ramp_for(material: str) -> Ramp:
    return RAMPS.get(material) or EXTRA_RAMPS[material]


# ---------------------------------------------------------------- base weapon boxes (WEAPON-REQ-016)
#
# Every weapon shares the receiver band y6-10 (the sprite generation's own BODY_TOP/BODY_BOTTOM),
# so a class's slot anchors keep landing somewhere sensible regardless of which weapon occupies the
# class. +Z is the muzzle end, -Z the stock/grip end, matching Create's own potato cannon. Each
# weapon keeps the one or two fixed, non-attachment silhouette features FA-21 round one and
# `DEC-018` named for it.

WEAPON_BOXES: dict[str, list[Box]] = {
    "m1911": [
        Box((7, 7, -3), (9, 10, 2), "steel"),  # slide
        Box((7, 6, -3), (9, 7, 0), "gunmetal", face_material={"east": "black"}),  # frame, ejection port east
        Box((7.5, 10, -3.2), (8.5, 10.6, -2.8), "gunmetal"),  # hammer spur
        Box((7.3, 8, 2), (8.7, 9, 3.2), "steel"),  # barrel, barely proud of the slide
        Box((7, 5.3, -1.7), (9, 6, -0.5), "gunmetal"),  # trigger guard
        Box((7, 2, -3.6), (9, 6, -1.6), "dark_oak", rotation={"angle": -22.5, "axis": "x", "origin": (8, 6, -1.6)}),
    ],
    "micro_uzi": [
        Box((7, 7, -2), (9, 10, 3), "gunmetal"),  # stubby box receiver
        Box((7.5, 1, -0.5), (8.5, 7, 1.5), "black"),  # magazine-in-grip: one long straight housing
        Box((7, 10, -3.2), (9, 10.6, -1.8), "steel"),  # folded wire stock, flat along the top
        Box((7, 8.6, -4.2), (7.6, 10.6, -3.2), "steel"),  # its rear hoop, dropping down
        Box((7.3, 8, 3), (8.7, 9, 4), "gunmetal"),  # short barrel
    ],
    "akm": [
        Box((6.5, 7, -3), (9.5, 10, 4), "steel"),  # receiver
        Box((7.5, 8, 4), (8.5, 9, 9), "steel"),  # long barrel
        Box((7, 7.3, 1), (9, 8.7, 4), "oak"),  # wooden handguard
        Box((7.6, 9.2, 1), (8.4, 9.6, 8), "gunmetal"),  # gas tube over the barrel
        Box((7.3, 1, -1), (8.7, 6.8, 1.5), "black", rotation={"angle": 22.5, "axis": "x", "origin": (8, 6.8, 1.5)}),
        Box((7, 2, -3.6), (9, 6, -2), "dark_oak"),  # pistol grip
        Box((6.7, 6.5, -7), (9.3, 9, -3), "spruce"),  # wooden stock, rear half
        Box((7, 8.5, -7.6), (9, 9, -7), "spruce"),  # comb taper into the butt
    ],
    "ruger_mini_14": [
        Box((6.5, 7, -3), (9.5, 10, 4), "gunmetal"),  # receiver
        Box((7.5, 8, 4), (8.5, 9, 11), "steel"),  # long barrel
        Box((6.7, 6, -8), (9.3, 9.5, -3), "spruce"),  # full wood stock, rear half
        Box((7, 8.5, -8.6), (9, 9.5, -8), "spruce"),  # comb taper into the butt
        Box((7.3, 3, -1), (8.7, 6.8, 1), "gunmetal"),  # small box magazine
    ],
    "awm": [
        Box((6.5, 7, -3), (9.5, 10, 4), "gunmetal"),  # receiver
        Box((7.6, 8, 4), (8.4, 9, 12), "steel"),  # longest barrel
        Box((9.4, 9, -0.5), (10.2, 9.6, 0.5), "steel"),  # bolt handle
        Box((7.3, 10, -2), (8.7, 10.4, 3), "steel"),  # scope rail along the receiver top
        Box((6.8, 5.5, -9), (9.2, 9, -3), "polymer"),  # olive polymer stock
        Box((6.7, 5, 10), (7.3, 6, 10.6), "steel"),  # bipod stub, left leg
        Box((8.7, 5, 10), (9.3, 6, 10.6), "steel"),  # bipod stub, right leg
    ],
    "winchester_model_1897": [
        Box((6.5, 7, -3), (9.5, 10, 3), "steel"),  # receiver
        Box((7.6, 8, 3), (8.4, 9, 8), "steel"),  # barrel
        Box((7, 10, -3.2), (7.8, 10.8, -2.6), "gunmetal"),  # exposed hammer
        Box((7.7, 6.6, 0), (8.3, 7.2, 8), "brass"),  # brass tube magazine, under the barrel
        Box((7.3, 5.6, 1), (8.7, 6.4, 4), "oak"),  # pump forend, dropped a row
        Box((6.7, 6.5, -6), (9.3, 9.5, -3), "oak"),  # wooden stock
    ],
}


# ---------------------------------------------------------------- attachment part boxes
#
# Local coordinates, centred near the origin; `PART_ANCHOR[class][slot]` is added to every
# coordinate (and every rotation origin) to place a part in a given class's weapon space. Kept
# axis-aligned (no per-box rotation) so that per-class placement is a plain translation.

def _scope_boxes(magnification: int) -> list[Box]:
    end_z = 2.0 + min(4, magnification // 3)  # same length-scales-with-magnification rule as FA-21's icon
    return [Box((-1, -1, -2.5), (1, 1, end_z), "steel", face_material={"south": "glass"})]  # objective lens, +Z


PART_BOXES: dict[str, dict[str, list[Box]]] = {
    "muzzle": {
        "suppressor": [Box((-1, -1, 0), (1, 1, 6), "gunmetal")],
        "flash_hider": [
            Box((-1, -1, 0), (1, 1, 2), "gunmetal"),
            Box((-0.3, -0.3, 2), (0.3, 0.3, 3.5), "gunmetal"),
            Box((-1, -0.3, 2), (-0.4, 0.3, 3.2), "gunmetal"),
            Box((0.4, -0.3, 2), (1, 0.3, 3.2), "gunmetal"),
        ],
        "compensator": [Box((-1, -1, 0), (1, 1, 2.5), "steel", face_material={"up": "copper"})],
    },
    "optic": {
        "scope_2x": _scope_boxes(2),
        "scope_3x": _scope_boxes(3),
        "scope_4x": _scope_boxes(4),
        "scope_6x": _scope_boxes(6),
        "scope_8x": _scope_boxes(8),
        "scope_15x": _scope_boxes(15),
        "red_dot": [Box((-1, -0.3, -1), (1, 1.3, 1), "black", face_material={"up": "red_dot"})],
        "holo": [Box((-1.2, -0.2, -1.2), (1.2, 1.5, 1.2), "black", face_material={"south": "glass", "north": "glass"})],
    },
    "magazine": {
        "quickdraw_magazine": [Box((-1, -4, -0.7), (1, 0, 0.7), "black")],
        "extended_magazine": [Box((-1, -6, -0.7), (1, 0, 0.7), "black")],
        "extended_quickdraw_magazine": [Box((-1, -6, -0.7), (1, 0, 0.7), "black", face_material={"west": "brass"})],
    },
    "grip": {
        "light_grip": [Box((-0.4, -2, -0.4), (0.4, 0, 0.4), "dark_oak")],
        "half_grip": [Box((-0.6, -1.5, -0.6), (0.6, 0, 0.6), "dark_oak")],
        "vertical_grip": [Box((-0.6, -3, -0.6), (0.6, 0, 0.6), "black")],
        "angled_grip": [Box((-0.8, -2, -0.8), (0.8, 0, 0.8), "polymer")],
        "thumb_grip": [Box((-1, -0.5, -1), (1, 1.2, 1), "dark_oak")],
    },
    "stock": {
        "tactical_stock": [Box((-1.5, -1.5, 0), (1.5, 1.5, 1), "black")],
        "cheek_pad": [Box((-1.2, 0, 0.2), (1.2, 1, 2.2), "dark_oak")],
        "bullet_loops": [Box((-1.5, -0.2, 0), (1.5, 0.2, 4), "dark_oak", face_material={"south": "brass"})],
    },
}

# class -> slot -> (x, y, z) translation placing that slot's part in the class's own weapon space.
# Muzzle sits at the class's own barrel tip; optic on the receiver top or scope rail; magazine below
# the receiver, at or over the weapon's own fixed magazine; grip and stock at their attachable ends.
PART_ANCHOR: dict[str, dict[str, tuple[float, float, float]]] = {
    "pistol": {"muzzle": (8, 8.5, 3.2), "optic": (8, 10.6, -1), "magazine": (8, 2, -2.6)},
    "smg": {
        "muzzle": (8, 8.5, 4), "optic": (8, 10.6, 0.5), "magazine": (8, 1, 0.5),
        "grip": (8, 6, 2.5), "stock": (8, 8, -4.5),
    },
    "assault_rifle": {
        "muzzle": (8, 8.5, 9), "optic": (8, 10, 0), "magazine": (8, 1, 0.2),
        "grip": (8, 6.5, 2.5), "stock": (8, 7.5, -7.2),
    },
    "dmr": {"muzzle": (8, 8.5, 11), "optic": (8, 10, 0), "magazine": (8, 2.8, 0), "stock": (8, 7.5, -8.8)},
    "sniper_rifle": {"muzzle": (8, 8.5, 12), "optic": (8, 10.6, 0.5), "magazine": (8, 5.5, 0.2), "stock": (8, 7, -9.2)},
    "shotgun": {"muzzle": (8, 8.5, 8), "magazine": (8, 6.9, 8)},
}

# Display transforms, tuned per class from Create's potato cannon (thirdperson pushed further back
# for a longer gun so it does not clip into the player model; gui/ground/fixed scaled down for a
# long rifle and up for a compact pistol, so neither look tiny nor overflow the inventory slot).
BASE_DISPLAY = {
    "thirdperson_righthand": {"rotation": [0, -15, 0], "translation": [0, 0, -4]},
    "thirdperson_lefthand": {"rotation": [0, -15, 0], "translation": [0, 0, -4]},
    "firstperson_righthand": {"rotation": [5, 5, 5], "translation": [0.25, 5, 0.75]},
    "firstperson_lefthand": {"rotation": [5, 5, 5], "translation": [0.25, 5, 0.75]},
    "ground": {"rotation": [0, 0, 90], "scale": [0.77, 0.77, 0.77]},
    "gui": {"rotation": [64, 47, -47], "translation": [0.25, -0.25, 0], "scale": [0.86, 0.86, 0.86]},
    "fixed": {"rotation": [0, 90, 0], "scale": [0.72, 0.72, 0.72]},
}

CLASS_TUNING = {
    "pistol": {"scale": 1.05, "z_push": 0.0},
    "smg": {"scale": 0.95, "z_push": 0.0},
    "assault_rifle": {"scale": 0.75, "z_push": -1.5},
    "dmr": {"scale": 0.65, "z_push": -2.0},
    "sniper_rifle": {"scale": 0.52, "z_push": -2.5},
    "shotgun": {"scale": 0.78, "z_push": -1.0},
}


def class_display(class_name: str) -> dict:
    tuning = CLASS_TUNING[class_name]
    display = copy.deepcopy(BASE_DISPLAY)
    for context in ("ground", "gui", "fixed"):
        display[context]["scale"] = [round(s * tuning["scale"], 4) for s in display[context]["scale"]]
    for context in ("thirdperson_righthand", "thirdperson_lefthand"):
        display[context]["translation"][2] = round(display[context]["translation"][2] + tuning["z_push"], 4)
    return display


# ---------------------------------------------------------------- the atlas packer

def _face_dims(box: Box) -> dict[str, tuple[int, int]]:
    x0, y0, z0 = box.frm
    x1, y1, z1 = box.to
    dx, dy, dz = abs(x1 - x0), abs(y1 - y0), abs(z1 - z0)
    dx, dy, dz = max(1, round(dx)), max(1, round(dy)), max(1, round(dz))
    return {"up": (dx, dz), "down": (dx, dz), "north": (dx, dy), "south": (dx, dy), "east": (dz, dy), "west": (dz, dy)}


def _shelf_pack(sizes: list[tuple[int, int]], canvas: int) -> list[tuple[int, int]] | None:
    order = sorted(range(len(sizes)), key=lambda i: (-sizes[i][1], -sizes[i][0], i))
    placements: list[tuple[int, int] | None] = [None] * len(sizes)
    x = y = shelf_h = 0
    for i in order:
        w, h = sizes[i]
        if w > canvas or h > canvas:
            return None
        if x + w > canvas:
            x, y, shelf_h = 0, y + shelf_h, 0
        if y + h > canvas:
            return None
        placements[i] = (x, y)
        x += w
        shelf_h = max(shelf_h, h)
    return placements  # type: ignore[return-value]


def _paint_face(pixels, x: int, y: int, w: int, h: int, tone, outline) -> None:
    for xi in range(x, x + w):
        for yi in range(y, y + h):
            on_edge = xi in (x, x + w - 1) or yi in (y, y + h - 1)
            pixels[xi, yi] = outline if on_edge else tone


def build_atlas(boxes: list[Box]) -> tuple[int, Image.Image, dict[tuple[int, str], tuple[int, int, int, int]]]:
    """One texel per model unit, every box face its own rectangle, shelf-packed into the smallest
    power-of-two square that fits, painted from `palette.py`'s ramps per `FACE_TONE`."""
    faces: list[tuple[int, str, int, int]] = []
    for i, box in enumerate(boxes):
        dims = _face_dims(box)
        for face_name in FACES:
            w, h = dims[face_name]
            faces.append((i, face_name, w, h))

    size = 16
    placement = None
    while placement is None:
        size *= 2
        placement = _shelf_pack([(w, h) for _, _, w, h in faces], size)
        if size > 1024:
            raise RuntimeError("atlas: no box list should need a texture this large")

    atlas = Image.new("RGBA", (size, size), TRANSPARENT)
    pixels = atlas.load()
    uv_map: dict[tuple[int, str], tuple[int, int, int, int]] = {}
    for (box_index, face_name, w, h), (px, py) in zip(faces, placement):
        box = boxes[box_index]
        material = box.face_material.get(face_name, box.material)
        ramp = ramp_for(material)
        tone = getattr(ramp, FACE_TONE[face_name])
        _paint_face(pixels, px, py, w, h, tone, ramp.outline)
        uv_map[(box_index, face_name)] = (px, py, px + w, py + h)
    return size, atlas, uv_map


# ---------------------------------------------------------------- model JSON

def _element(box: Box, uv_map, box_index: int, offset: tuple[float, float, float] = (0, 0, 0)) -> dict:
    ox, oy, oz = offset
    frm = [box.frm[0] + ox, box.frm[1] + oy, box.frm[2] + oz]
    to = [box.to[0] + ox, box.to[1] + oy, box.to[2] + oz]
    faces = {}
    for face_name in FACES:
        x0, y0, x1, y1 = uv_map[(box_index, face_name)]
        faces[face_name] = {"uv": [x0, y0, x1, y1], "texture": "#0"}
    element: dict = {"from": frm, "to": to, "faces": faces}
    if box.rotation:
        rx, ry, rz = box.rotation["origin"]
        element["rotation"] = {
            "angle": box.rotation["angle"],
            "axis": box.rotation["axis"],
            "origin": [rx + ox, ry + oy, rz + oz],
        }
    return element


def weapon_model_json(weapon_id: str, boxes: list[Box], size: int, uv_map, class_name: str) -> dict:
    texture_ref = f"{NS}:item/weapon/{weapon_id}"
    return {
        "parent": f"{NS}:item/weapon/class/{class_name}",
        "texture_size": [size, size],
        "textures": {"0": texture_ref, "particle": texture_ref},
        "elements": [_element(box, uv_map, i) for i, box in enumerate(boxes)],
    }


def part_model_json(slot: str, name: str, class_name: str, boxes: list[Box], anchor, size: int, uv_map) -> dict:
    texture_ref = f"{NS}:item/weapon/part/{slot}_{name}"
    return {
        "parent": f"{NS}:item/weapon/class/{class_name}",
        "texture_size": [size, size],
        "textures": {"0": texture_ref, "particle": texture_ref},
        "elements": [_element(box, uv_map, i, offset=anchor) for i, box in enumerate(boxes)],
    }


def generate_weapon_item_json() -> None:
    """`items/weapon.json`: the same select/composite/condition tree FA-9 built, every
    `minecraft:model` reference now pointing at a cuboid model instead of a sprite
    (`WEAPON-REQ-012`, `ARCH-DEC-006`, unchanged shape)."""
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
                {"when": f"{NS}:{name}", "model": model_ref(f"item/weapon/part/{weapon_class}/{slot}_{name}")}
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


# ---------------------------------------------------------------- sheet renderer (review only, not
# a game asset): an orthographic side-view projection of every weapon, bare and fully loaded, plus
# the atlases themselves, labelled like FA-21's round-one sheet.

def _side_project(boxes: list[Box], offset=(0, 0, 0)) -> tuple[float, float, float, float]:
    ox, oy, oz = offset
    zs = [c[2] + oz for b in boxes for c in (b.frm, b.to)]
    ys = [c[1] + oy for b in boxes for c in (b.frm, b.to)]
    return min(zs), max(zs), min(ys), max(ys)


def _draw_side_view(draw_boxes: list[tuple[list[Box], tuple[float, float, float]]], canvas: Image.Image,
                     x0: int, y0: int, scale: int, z_min: float, y_max: float) -> None:
    px = canvas.load()
    for boxes, offset in draw_boxes:
        ox, oy, oz = offset
        for box in boxes:
            ramp = ramp_for(box.material)
            bx0 = min(box.frm[0], box.to[0]) + ox
            bx1 = max(box.frm[0], box.to[0]) + ox
            depth = bx1 - bx0
            z0, z1 = sorted((box.frm[2] + oz, box.to[2] + oz))
            y0b, y1b = sorted((box.frm[1] + oy, box.to[1] + oy))
            sx0 = x0 + round((z0 - z_min) * scale)
            sx1 = x0 + round((z1 - z_min) * scale)
            sy0 = y0 + round((y_max - y1b) * scale)
            sy1 = y0 + round((y_max - y0b) * scale)
            tone = _shade(ramp.base, round(depth * 3))  # thicker boxes read a touch lighter
            for xi in range(sx0, max(sx1, sx0 + 1)):
                for yi in range(sy0, max(sy1, sy0 + 1)):
                    on_edge = xi in (sx0, sx1 - 1) or yi in (sy0, sy1 - 1)
                    if 0 <= xi < canvas.width and 0 <= yi < canvas.height:
                        px[xi, yi] = ramp.outline if on_edge else tone


def _loaded_parts(weapon_id: str) -> list[tuple[list[Box], tuple[float, float, float]]]:
    weapon_class = WEAPONS[weapon_id][0]
    parts = []
    for slot in SLOTS:
        if slot not in WEAPON_CLASSES[weapon_class]:
            continue
        name = next(iter(ATTACHMENTS[slot]))
        anchor = PART_ANCHOR[weapon_class][slot]
        parts.append((PART_BOXES[slot][name], anchor))
    return parts


def render_sheet(path: Path) -> None:
    scale = 8
    pad = 16
    cell_w, cell_h = 340, 200
    columns = 2  # bare, loaded
    rows = len(WEAPONS)
    header = 56
    sheet = Image.new("RGBA", (columns * cell_w, header + rows * cell_h + 140), (235, 235, 235, 255))
    draw = ImageDraw.Draw(sheet)
    draw.text((8, 6), "FA-22 round 1 -- cuboid weapon models, side projection (8x)", fill=(20, 20, 20, 255))
    draw.text((8, 38), "bare", fill=(90, 90, 90, 255))
    draw.text((cell_w + 8, 38), "fully loaded", fill=(90, 90, 90, 255))

    for row, weapon_id in enumerate(WEAPONS):
        boxes = WEAPON_BOXES[weapon_id]
        loaded = _loaded_parts(weapon_id)
        z_min, z_max, y_min, y_max = _side_project(boxes)
        for parts, offset in loaded:
            lz0, lz1, ly0, ly1 = _side_project(parts, offset)
            z_min, z_max = min(z_min, lz0), max(z_max, lz1)
            y_min, y_max = min(y_min, ly0), max(y_max, ly1)
        cy = header + row * cell_h
        draw.text((8, cy + 4), weapon_id, fill=(20, 20, 20, 255))
        origin_x = pad - round(z_min * scale)
        origin_y = cy + 22
        _draw_side_view([(boxes, (0, 0, 0))], sheet, origin_x, origin_y, scale, z_min, y_max)
        _draw_side_view([(boxes, (0, 0, 0)), *loaded], sheet, cell_w + origin_x, origin_y, scale, z_min, y_max)

    # Atlases, labelled, along the bottom.
    atlas_y = header + rows * cell_h + 20
    draw.text((8, atlas_y - 16), "atlases", fill=(20, 20, 20, 255))
    x = 8
    for weapon_id, boxes in WEAPON_BOXES.items():
        _, atlas, _ = build_atlas(boxes)
        shown = atlas.resize((atlas.width * 2, atlas.height * 2), Image.NEAREST)
        sheet.paste(shown, (x, atlas_y), shown)
        draw.text((x, atlas_y + shown.height + 2), weapon_id, fill=(60, 60, 60, 255))
        x += shown.width + 16

    path.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(path)


# ---------------------------------------------------------------- entry point

def main() -> None:
    written: list[Path] = []

    for weapon_id, (weapon_class, *_rest) in WEAPONS.items():
        boxes = WEAPON_BOXES[weapon_id]
        size, atlas, uv_map = build_atlas(boxes)
        save_png(atlas, TEXTURES / "weapon" / f"{weapon_id}.png")
        write_json(MODELS / "weapon" / f"{weapon_id}.json", weapon_model_json(weapon_id, boxes, size, uv_map, weapon_class))
        written.append(TEXTURES / "weapon" / f"{weapon_id}.png")

    for class_name in WEAPON_CLASSES:
        write_json(MODELS / "weapon" / "class" / f"{class_name}.json", {"display": class_display(class_name)})

    for slot, names in ATTACHMENTS.items():
        for name in names:
            boxes = PART_BOXES[slot][name]
            size, atlas, uv_map = build_atlas(boxes)
            save_png(atlas, TEXTURES / "weapon" / "part" / f"{slot}_{name}.png")
            written.append(TEXTURES / "weapon" / "part" / f"{slot}_{name}.png")
            for class_name, class_slots in WEAPON_CLASSES.items():
                if slot not in class_slots:
                    continue
                anchor = PART_ANCHOR[class_name][slot]
                write_json(
                    MODELS / "weapon" / "part" / class_name / f"{slot}_{name}.json",
                    part_model_json(slot, name, class_name, boxes, anchor, size, uv_map),
                )

    generate_weapon_item_json()
    render_sheet(SHEET_PATH)

    print(f"wrote {len(written)} atlases; every weapon model, class display parent, part model "
          f"and items/weapon.json is under {ASSETS}; sheet at {SHEET_PATH}")


if __name__ == "__main__":
    main()
