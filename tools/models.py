#!/usr/bin/env python3
"""Cuboid weapon and attachment-part item models (WEAPON-REQ-016, `decisions/DEC-018-art-direction.md`).

Every base weapon becomes a Blockbench-style cuboid element model with its own texture atlas,
the way Create renders its potato cannon, and every mounted attachment becomes a cuboid part
model positioned at its slot's anchor in model units. This module owns everything FA-9/FA-21's
`sprites.py` used to own under `models/item/weapon/**` and `items/weapon.json`; `sprites.py`
keeps drawing the standalone attachment icons, cartridges and their own item definitions
untouched, because FA-21 round two edits those in parallel on another branch.

Round 2 (Kevin's review of round 1): every base is now a *connected* assembly -- barrel, sight
nubs, trigger guard, grip, stock and magazine all share a face or overlap by at least half a unit
with a neighbour, checked by `boxes_connected` and `tools/test_models.py::test_no_floating_elements`
-- 9 to 14 elements per weapon instead of round 1's 5 to 8 flat boxes, and every attachment part is
placed so it touches the base's own geometry at its class's anchor (a suppressor overlapping the
barrel's last unit, an optic sitting flush on the receiver top, a magazine inserted a unit into its
well, a grip flush against the receiver's underside, a stock butted against the rearmost fixed
surface). Every atlas face also carries a pixel detail on top of its flat tone -- serrations, an
ejection port, receiver rivets, wood grain, a lens glint, the red dot, magazine witness lines --
from `_apply_face_detail`.

The box DSL: each weapon and each attachment part is a list of `Box(from_xyz, to_xyz, material)`
in model units (16 units = 1 block), optionally with a `rotation` (a single-axis Blockbench-style
element rotation, angle one of -45/-22.5/0/22.5/45), a `face_material` override for a detail's own
material (a scope's glass lens, a compensator's copper port), and a `face_detail` naming a pixel
pattern to paint on top of a face's flat tone. The weapon lies along the Z axis exactly as Create's
cannon does: -Z is the stock/grip end, near the player's hand, +Z is the muzzle end.

The atlas packer gives every box face its own pixel rectangle -- one texel per model unit -- in a
per-weapon or per-part PNG, shelf-packed into the smallest power-of-two square that fits (up to
64x64 or larger for the busier rifles), painted from `tools/palette.py`'s ramps: `light` on the up
face and the face toward the light (this module's convention: west), `base` on the remaining side
face (north), `shade` on down/east/south, a one-texel `outline` border on every face, then any
named pixel detail (`WEAPON-REQ-017`).

Deterministic, committed output: run `python3 tools/models.py` and commit whatever changes under
`src/main/resources/assets/firearms/{items,models,textures}`. A preview sheet -- a real rotation-
and-projection renderer sampling the actual atlas pixels with vanilla's per-face directional
shading, in both the `gui` and `thirdperson_righthand` display poses, bare and fully loaded, plus a
2x mock 3x3 inventory grid -- is written alongside for review.
"""
from __future__ import annotations

import copy
import math
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
    "bb7bc244-01b6-4890-9ca3-da5d384dc21e/scratchpad/fa22-round2.png"
)

FACES = ("north", "south", "east", "west", "up", "down")

# Lighting convention (WEAPON-REQ-017, DEC-018): up and west are lit; north is the one plain "base"
# side face; down, east and south are in shade. Kept as one fixed table so every box, weapon and
# part is shaded the same way regardless of which function built it.
FACE_TONE = {"up": "light", "west": "light", "north": "base", "south": "shade", "east": "shade", "down": "shade"}

# Vanilla's own directional face shading, applied by the preview renderer on top of the atlas's own
# painted tones -- the same multiplier the game engine applies to every block/item face.
DIRECTIONAL_SHADE = {"up": 1.0, "down": 0.5, "north": 0.8, "south": 0.8, "east": 0.6, "west": 0.6}


@dataclass(frozen=True)
class Box:
    frm: tuple[float, float, float]
    to: tuple[float, float, float]
    material: str
    face_material: dict[str, str] = field(default_factory=dict)
    face_detail: dict[str, str] = field(default_factory=dict)
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


# ---------------------------------------------------------------- connectivity (Kevin, round 2:
# "every element of a base must share a face or overlap by at least 0.5 units with another element
# of the same base, and every part's elements must touch or overlap the base's geometry")

def _axis_overlap(a0: float, a1: float, b0: float, b1: float) -> float:
    return min(a1, b1) - max(a0, b0)


def boxes_connected(a: Box, b: Box, min_overlap: float = 0.5) -> bool:
    """Two boxes are connected if they share a face (one axis exactly flush, the other two
    overlapping by any positive amount -- a real shared face, not just an edge or a corner) or if
    they overlap volumetrically by at least `min_overlap` units on every axis."""
    ox = _axis_overlap(a.frm[0], a.to[0], b.frm[0], b.to[0])
    oy = _axis_overlap(a.frm[1], a.to[1], b.frm[1], b.to[1])
    oz = _axis_overlap(a.frm[2], a.to[2], b.frm[2], b.to[2])
    if ox < -1e-9 or oy < -1e-9 or oz < -1e-9:
        return False
    overlaps = (ox, oy, oz)
    zero_axes = sum(1 for o in overlaps if abs(o) < 1e-9)
    positive_axes = sum(1 for o in overlaps if o > 1e-9)
    if zero_axes >= 1:
        return positive_axes == 2
    return min(overlaps) >= min_overlap - 1e-9


def is_internally_connected(boxes: list[Box]) -> tuple[bool, list[int]]:
    """Whether every box is reachable from box 0 through `boxes_connected` edges. Returns the
    unreachable indices too, so a failing test can say exactly which element floats."""
    n = len(boxes)
    if n == 0:
        return True, []
    seen = {0}
    frontier = [0]
    while frontier:
        i = frontier.pop()
        for j in range(n):
            if j in seen:
                continue
            if boxes_connected(boxes[i], boxes[j]):
                seen.add(j)
                frontier.append(j)
    missing = [i for i in range(n) if i not in seen]
    return not missing, missing


def part_touches_base(base_boxes: list[Box], part_boxes: list[Box]) -> bool:
    return any(boxes_connected(pb, bb) for pb in part_boxes for bb in base_boxes)


# ---------------------------------------------------------------- base weapon boxes (WEAPON-REQ-016)
#
# Every element shares a face or overlaps by >=0.5 units with another (connectivity, above). Built
# as a connected assembly from a receiver "spine": barrel and sights flush to the receiver, trigger
# guard a thin two- or three-element loop flush to the receiver's underside, grip/stock/magazine
# each flush to the receiver or to another already-connected element. +Z is the muzzle end, -Z the
# stock/grip end, matching Create's own potato cannon.

WEAPON_BOXES: dict[str, list[Box]] = {
    "m1911": [
        Box((7, 6, -3.4), (9, 8, 0.6), "gunmetal", face_detail={"north": "rivets"}),  # lower frame
        Box((7, 8, -3.8), (9, 10, 1.4), "steel", face_material={"east": "black"},
            face_detail={"west": "serrations", "east": "ejection_port"}),  # slide
        Box((7.4, 8, 1.4), (8.6, 9, 2.6), "steel"),  # barrel, flush to the slide's own front face
        Box((7.7, 10, 1.0), (8.3, 10.4, 1.4), "steel"),  # front sight, flush on the slide's top
        Box((7.7, 10, -3.8), (8.3, 10.4, -3.4), "steel"),  # rear sight, flush on the slide's top
        Box((7.7, 10.4, -3.9), (8.3, 10.9, -3.5), "gunmetal"),  # hammer spur, atop the rear sight
        Box((7.3, 5.3, -1.4), (7.7, 6, -1.0), "gunmetal"),  # trigger guard, front leg
        Box((7.3, 5.0, -1.4), (7.7, 5.3, 0.3), "gunmetal"),  # trigger guard, bottom
        Box((7.3, 5.3, 0.0), (7.7, 6, 0.4), "gunmetal"),  # trigger guard, rear leg
        Box((7, 2, -3.6), (9, 6, -1.4), "dark_oak", face_detail={"north": "wood_grain"},
            rotation={"angle": -22.5, "axis": "x", "origin": (8, 6, -1.6)}),  # grip, angled back and down
        Box((7.3, 1, -3.0), (8.7, 2, -2.0), "black", face_detail={"north": "witness_lines"}),  # magazine
    ],
    "micro_uzi": [
        Box((7, 7, -2.2), (9, 10, 2.6), "gunmetal", face_detail={"north": "rivets"}),  # stubby receiver
        Box((7.4, 8, 2.6), (8.6, 9, 3.8), "gunmetal"),  # short barrel, flush to the receiver's front
        Box((7.7, 10, 2.2), (8.3, 10.4, 2.6), "steel"),  # front sight
        Box((7.7, 10, -2.2), (8.3, 10.4, -1.8), "steel"),  # rear sight
        Box((7.5, 1, -0.5), (8.5, 7, 1.5), "black", face_detail={"north": "witness_lines"}),  # magazine-in-grip
        Box((7, 10, -4.5), (9, 10.4, -2.2), "steel"),  # folded wire stock, flat along the top
        Box((7, 8.6, -5.0), (7.6, 10.4, -4.5), "steel"),  # its rear hoop, dropping down
        Box((7.3, 6.3, -0.6), (7.7, 7, -0.2), "gunmetal"),  # trigger guard, front leg
        Box((7.3, 6, -0.6), (7.7, 6.3, 0.3), "gunmetal"),  # trigger guard, bottom
        Box((7.3, 6.3, 0.0), (7.7, 7, 0.4), "gunmetal"),  # trigger guard, rear leg
    ],
    "akm": [
        Box((6.5, 6, -3.4), (9.5, 10, 4.6), "steel", face_detail={"north": "rivets"}),  # receiver
        Box((7.4, 7.5, 4.6), (8.6, 8.5, 9.6), "steel"),  # long barrel, flush to the receiver's front
        Box((7.8, 8.5, 9.0), (8.2, 8.9, 9.4), "gunmetal"),  # front sight, flush on the barrel's top
        Box((7.8, 10, -1.0), (8.2, 10.4, -0.6), "gunmetal"),  # rear sight, flush on the receiver's top
        Box((7.7, 8.5, 1.0), (8.3, 8.9, 8.8), "gunmetal"),  # gas tube, flush on the barrel's top
        Box((7, 6.5, 1.0), (9, 7.5, 5.0), "oak", face_detail={"west": "wood_grain"}),  # handguard, flush to the barrel
        Box((7, 5.3, -1.6), (7.5, 6, -1.2), "gunmetal"),  # trigger guard, front leg
        Box((7, 5.0, -1.6), (7.5, 5.3, 0.2), "gunmetal"),  # trigger guard, bottom
        Box((7, 2, -3.8), (9, 6, -1.8), "dark_oak", face_detail={"north": "wood_grain"},
            rotation={"angle": -22.5, "axis": "x", "origin": (8, 6, -2)}),  # pistol grip
        Box((7.3, 3.5, 0.0), (8.7, 6, 1.5), "black", face_detail={"north": "witness_lines"},
            rotation={"angle": 22.5, "axis": "x", "origin": (8, 6, 1.5)}),  # curved magazine, upper segment
        Box((7.3, 1.0, 0.8), (8.7, 3.5, 2.3), "black",
            rotation={"angle": 45, "axis": "x", "origin": (8, 3.5, 1.55)}),  # curved magazine, lower segment
        Box((6.7, 6.4, -7.6), (9.3, 9, -3.4), "spruce", face_detail={"west": "wood_grain"}),  # wood stock, flush to the receiver's rear
        Box((6.7, 6.4, -8.0), (9.3, 9, -7.6), "spruce"),  # buttplate
    ],
    "ruger_mini_14": [
        Box((6.5, 6, -3.4), (9.5, 10, 4.6), "gunmetal", face_detail={"north": "rivets"}),  # receiver
        Box((7.4, 7.5, 4.6), (8.6, 8.5, 11.6), "steel"),  # long barrel, flush to the receiver's front
        Box((7.8, 8.5, 11.0), (8.2, 8.9, 11.4), "gunmetal"),  # front sight, flush on the barrel's top
        Box((7.8, 10, -1.0), (8.2, 10.4, -0.6), "gunmetal"),  # rear sight, flush on the receiver's top
        Box((6.7, 6.4, -8.6), (9.3, 9, -3.4), "spruce", face_detail={"west": "wood_grain"}),  # wood stock, flush to the receiver's rear
        Box((6.7, 6.4, -9.0), (9.3, 9, -8.6), "spruce"),  # buttplate
        Box((7, 9, -8.2), (9, 9.5, -7.6), "spruce"),  # comb, flush on the stock's top
        Box((7, 5.6, 1.0), (9, 6, 5.0), "spruce", face_detail={"west": "wood_grain"}),  # full stock running under the barrel
        Box((7, 5.3, -1.6), (7.5, 6, -1.2), "gunmetal"),  # trigger guard, front leg
        Box((7, 5.0, -1.6), (7.5, 5.3, 0.2), "gunmetal"),  # trigger guard, bottom
        Box((7.3, 3.5, -0.5), (8.7, 6, 1.0), "gunmetal", face_detail={"north": "witness_lines"}),  # small box magazine
    ],
    "awm": [
        Box((6.5, 6, -3.4), (9.5, 10, 4.6), "gunmetal", face_detail={"north": "rivets"}),  # receiver
        Box((7.4, 7.5, 4.6), (8.6, 8.5, 12.6), "steel"),  # longest barrel, flush to the receiver's front
        Box((9.5, 8, -0.5), (10.3, 8.6, 0.5), "steel"),  # bolt handle, flush on the receiver's side
        Box((7.3, 10, -2.0), (8.7, 10.4, 3.0), "steel"),  # scope rail, flush on the receiver's top
        Box((6.6, 4.8, 11.6), (7.4, 7.6, 12.2), "steel"),  # bipod, left leg, flush to the barrel
        Box((8.6, 4.8, 11.6), (9.4, 7.6, 12.2), "steel"),  # bipod, right leg, flush to the barrel
        Box((6.8, 5.5, -9.2), (9.2, 9, -3.4), "polymer"),  # olive stock, flush to the receiver's rear
        Box((6.8, 5.5, -9.6), (9.2, 9, -9.2), "polymer"),  # buttplate
        Box((7, 9, -7.0), (9, 9.6, -5.0), "polymer"),  # cheek riser, flush on the stock's top
        Box((7.3, 4.8, -0.5), (8.7, 6, 1.0), "gunmetal", face_detail={"north": "witness_lines"}),  # flush magazine
        Box((7, 5.3, -1.6), (7.5, 6, -1.2), "gunmetal"),  # trigger guard, front leg
        Box((7, 5.0, -1.6), (7.5, 5.3, 0.2), "gunmetal"),  # trigger guard, bottom
    ],
    "winchester_model_1897": [
        Box((6.5, 6, -3.4), (9.5, 10, 3.4), "steel", face_detail={"north": "rivets"}),  # receiver
        Box((7.4, 7.5, 3.4), (8.6, 8.5, 8.4), "steel"),  # barrel, flush to the receiver's front
        Box((7.8, 8.5, 7.8), (8.2, 8.9, 8.2), "steel"),  # bead front sight, flush on the barrel's top
        Box((7.7, 10, -3.7), (8.3, 10.6, -3.3), "gunmetal"),  # exposed hammer, flush on the receiver's top
        Box((7.7, 5.4, 0.0), (8.3, 6.0, 8.0), "brass", face_detail={"north": "witness_lines"}),  # brass tube magazine
        Box((7.3, 4.6, 1.5), (8.7, 5.4, 4.5), "oak", face_detail={"west": "wood_grain"}),  # pump forend, flush to the tube
        Box((6.7, 6.4, -6.4), (9.3, 9, -3.4), "oak", face_detail={"west": "wood_grain"}),  # wood stock, flush to the receiver's rear
        Box((6.7, 6.4, -6.8), (9.3, 9, -6.4), "oak"),  # buttplate
        Box((7, 5.3, -1.6), (7.5, 6, -1.2), "gunmetal"),  # trigger guard, front leg
        Box((7, 5.0, -1.6), (7.5, 5.3, 0.2), "gunmetal"),  # trigger guard, bottom
    ],
}

# weapon_id -> its own class, for the anchor/display machinery below (WEAPONS is keyed the other
# way round, one weapon per class at 1.0 per `WEAPON-DEC-002`).
WEAPON_FOR_CLASS: dict[str, str] = {weapon_class: weapon_id for weapon_id, (weapon_class, *_r) in WEAPONS.items()}


# ---------------------------------------------------------------- attachment part boxes
#
# Local coordinates: the axis that touches the base is exactly 0 (or, for the muzzle, -1, so the
# part overlaps the barrel's last unit) so that `PART_ANCHOR[class][slot]` -- a plain translation --
# always lands the part flush against or inserted into the base's own geometry, per Kevin's round-2
# rule: muzzle overlaps the barrel end by 1 unit at the barrel's own thickness and centre, optics
# sit flush on the receiver top, magazines insert 1 unit into the well, grips hang flush from the
# underside, stocks butt flush against the rearmost fixed surface.

def _scope_boxes(magnification: int) -> list[Box]:
    end_z = 2.0 + min(4, magnification // 3)  # same length-scales-with-magnification rule as FA-21's icon
    return [Box((-1, 0, -2.5), (1, 2, end_z), "steel", face_material={"south": "glass"},
                face_detail={"south": "glint"})]


PART_BOXES: dict[str, dict[str, list[Box]]] = {
    "muzzle": {
        "suppressor": [Box((-0.6, -0.5, -1), (0.6, 0.5, 5), "gunmetal", face_detail={"north": "rivets"})],
        "flash_hider": [
            Box((-0.6, -0.5, -1), (0.6, 0.5, 1), "gunmetal"),
            Box((-0.3, -0.3, 1), (0.3, 0.3, 2.5), "gunmetal"),
            Box((-0.6, -0.3, 1), (-0.35, 0.3, 2.2), "gunmetal"),
            Box((0.35, -0.3, 1), (0.6, 0.3, 2.2), "gunmetal"),
        ],
        "compensator": [Box((-0.6, -0.5, -1), (0.6, 0.5, 1.5), "steel", face_material={"up": "copper"})],
    },
    "optic": {
        "scope_2x": _scope_boxes(2),
        "scope_3x": _scope_boxes(3),
        "scope_4x": _scope_boxes(4),
        "scope_6x": _scope_boxes(6),
        "scope_8x": _scope_boxes(8),
        "scope_15x": _scope_boxes(15),
        "red_dot": [Box((-1, 0, -1), (1, 1.6, 1), "black", face_material={"up": "red_dot"}, face_detail={"up": "glint"})],
        "holo": [Box((-1.2, 0, -1.2), (1.2, 1.7, 1.2), "black", face_material={"south": "glass", "north": "glass"})],
    },
    "magazine": {
        "quickdraw_magazine": [Box((-0.7, -1.5, -0.6), (0.7, 1.0, 0.6), "black", face_detail={"north": "witness_lines"})],
        "extended_magazine": [Box((-0.7, -2.5, -0.6), (0.7, 1.0, 0.6), "black", face_detail={"north": "witness_lines"})],
        "extended_quickdraw_magazine": [
            Box((-0.7, -2.5, -0.6), (0.7, 1.0, 0.6), "black", face_material={"west": "brass"},
                face_detail={"north": "witness_lines"}),
        ],
    },
    "grip": {
        "light_grip": [Box((-0.4, -2, -0.4), (0.4, 0, 0.4), "dark_oak", face_detail={"north": "wood_grain"})],
        "half_grip": [Box((-0.6, -1.5, -0.6), (0.6, 0, 0.6), "dark_oak", face_detail={"north": "wood_grain"})],
        "vertical_grip": [Box((-0.6, -3, -0.6), (0.6, 0, 0.6), "black")],
        "angled_grip": [Box((-0.8, -2, -0.8), (0.8, 0, 0.8), "polymer",
                             rotation={"angle": -22.5, "axis": "x", "origin": (0, 0, 0)})],
        "thumb_grip": [Box((-1, -1.2, -1), (1, 0, 1), "dark_oak", face_detail={"north": "wood_grain"})],
    },
    "stock": {
        # local z ends at 0 -- flush against the base's rearmost fixed surface -- and extends
        # backward (negative z) from there, so the part sits cleanly behind the fixed stock rather
        # than overlapping into it.
        "tactical_stock": [Box((-1.5, -1.5, -1), (1.5, 1.5, 0), "black")],
        "cheek_pad": [Box((-1.2, 0, -2), (1.2, 1, 0), "dark_oak", face_detail={"north": "wood_grain"})],
        "bullet_loops": [Box((-1.5, -0.2, -4), (1.5, 0.2, 0), "dark_oak", face_material={"north": "brass"})],
    },
}

# class -> slot -> (x, y, z) translation placing that slot's part in the class's own weapon space,
# read directly off each class's own weapon geometry above so every anchor is provably flush with
# or overlapping it (verified by `tools/test_models.py::test_no_floating_elements`).
PART_ANCHOR: dict[str, dict[str, tuple[float, float, float]]] = {
    "pistol": {"muzzle": (8, 8.5, 2.6), "optic": (8, 10, -1), "magazine": (8, 1, -2.5)},
    "smg": {
        "muzzle": (8, 8.5, 3.8), "optic": (8, 10, 0), "magazine": (8, 1, 0.5),
        "grip": (8, 7, 2), "stock": (8, 9.5, -5.0),
    },
    "assault_rifle": {
        "muzzle": (8, 8, 9.6), "optic": (8, 10, -0.8), "magazine": (8, 1.0, 1.55),
        "grip": (8, 6.5, 3.0), "stock": (8, 7.7, -8.0),
    },
    "dmr": {"muzzle": (8, 8, 11.6), "optic": (8, 10, -0.8), "magazine": (8, 3.5, 0.25), "stock": (8, 7.7, -9.0)},
    "sniper_rifle": {"muzzle": (8, 8, 12.6), "optic": (8, 10.4, 0.5), "magazine": (8, 4.8, 0.25), "stock": (8, 7.25, -9.6)},
    "shotgun": {"muzzle": (8, 8, 8.4), "magazine": (8, 5.4, 4.0)},
}

# Display transforms, started from Create's potato cannon. `_tuned_scale` (below) computes the
# gui/ground/fixed scale per class from the class's own loaded bounding box under the gui rotation,
# so the longest rifle fills the 16-unit slot without clipping and a pistol does not look tiny
# (Kevin, round 2, item 5); `_z_push` pulls a longer gun's thirdperson pose further back so it does
# not clip into the player model.
BASE_DISPLAY = {
    "thirdperson_righthand": {"rotation": [0, -15, 0], "translation": [0, 0, -4]},
    "thirdperson_lefthand": {"rotation": [0, -15, 0], "translation": [0, 0, -4]},
    "firstperson_righthand": {"rotation": [5, 5, 5], "translation": [0.25, 5, 0.75]},
    "firstperson_lefthand": {"rotation": [5, 5, 5], "translation": [0.25, 5, 0.75]},
    "ground": {"rotation": [0, 0, 90], "scale": [0.77, 0.77, 0.77]},
    "gui": {"rotation": [64, 47, -47], "translation": [0.25, -0.25, 0], "scale": [0.86, 0.86, 0.86]},
    "fixed": {"rotation": [0, 90, 0], "scale": [0.72, 0.72, 0.72]},
}

PIVOT = (8.0, 8.0, 8.0)


def _box_corners(box: Box) -> list[tuple[float, float, float]]:
    x0, y0, z0 = box.frm
    x1, y1, z1 = box.to
    return [
        (x0, y0, z0), (x1, y0, z0), (x0, y1, z0), (x1, y1, z0),
        (x0, y0, z1), (x1, y0, z1), (x0, y1, z1), (x1, y1, z1),
    ]


def _rotate_axis(p: tuple[float, float, float], angle_deg: float, axis: str,
                  origin: tuple[float, float, float]) -> tuple[float, float, float]:
    x, y, z = p[0] - origin[0], p[1] - origin[1], p[2] - origin[2]
    a = math.radians(angle_deg)
    ca, sa = math.cos(a), math.sin(a)
    if axis == "x":
        y, z = y * ca - z * sa, y * sa + z * ca
    elif axis == "y":
        x, z = x * ca + z * sa, -x * sa + z * ca
    else:
        x, y = x * ca - y * sa, x * sa + y * ca
    return (x + origin[0], y + origin[1], z + origin[2])


def _rotate_view(p: tuple[float, float, float], angles_deg: list[float],
                  pivot: tuple[float, float, float]) -> tuple[float, float, float]:
    p = _rotate_axis(p, angles_deg[0], "x", pivot)
    p = _rotate_axis(p, angles_deg[1], "y", pivot)
    p = _rotate_axis(p, angles_deg[2], "z", pivot)
    return p


def _element_corners_after_own_rotation(box: Box) -> list[tuple[float, float, float]]:
    corners = _box_corners(box)
    if box.rotation:
        corners = [_rotate_axis(c, box.rotation["angle"], box.rotation["axis"], box.rotation["origin"]) for c in corners]
    return corners


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


def _all_elements(weapon_id: str, loaded: bool) -> list[Box]:
    elements = list(WEAPON_BOXES[weapon_id])
    if loaded:
        for parts, (ox, oy, oz) in _loaded_parts(weapon_id):
            for p in parts:
                elements.append(Box(
                    (p.frm[0] + ox, p.frm[1] + oy, p.frm[2] + oz),
                    (p.to[0] + ox, p.to[1] + oy, p.to[2] + oz),
                    p.material, p.face_material, p.face_detail, p.rotation,
                ))
    return elements


def _gui_bbox_extent(weapon_id: str) -> tuple[float, float]:
    """The (width, height) of the fully-loaded weapon's own bounding box after only the `gui`
    rotation (no scale, no translation) -- what `_tuned_scale` fits into the 16-unit slot."""
    xs: list[float] = []
    ys: list[float] = []
    for box in _all_elements(weapon_id, loaded=True):
        for corner in _element_corners_after_own_rotation(box):
            rx, ry, _rz = _rotate_view(corner, BASE_DISPLAY["gui"]["rotation"], PIVOT)
            xs.append(rx)
            ys.append(ry)
    return max(xs) - min(xs), max(ys) - min(ys)


def _tuned_scale(weapon_id: str, target_fill: float = 0.94, min_scale: float = 0.35, max_scale: float = 1.15) -> float:
    width, height = _gui_bbox_extent(weapon_id)
    extent = max(width, height)
    if extent <= 0:
        return 1.0
    return max(min_scale, min(max_scale, target_fill * 16 / extent))


def _z_push(weapon_id: str) -> float:
    zs = [c for box in WEAPON_BOXES[weapon_id] for c in (box.frm[2], box.to[2])]
    extent = max(zs) - min(zs)
    return -round(max(0.0, (extent - 8.0) * 0.2), 4)


def class_display(class_name: str) -> dict:
    weapon_id = WEAPON_FOR_CLASS[class_name]
    scale = round(_tuned_scale(weapon_id), 4)
    display = copy.deepcopy(BASE_DISPLAY)
    for context in ("ground", "gui", "fixed"):
        display[context]["scale"] = [scale, scale, scale]
    for context in ("thirdperson_righthand", "thirdperson_lefthand"):
        display[context]["translation"][2] = round(display[context]["translation"][2] + _z_push(weapon_id), 4)
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


# ---------------------------------------------------------------- atlas pixel detail (Kevin, round
# 2, item 3): a named pattern painted over a face's flat tone, using that face's own resolved ramp.

def _interior(x: int, y: int, w: int, h: int) -> tuple[int, int, int, int]:
    """The face rectangle's interior, inside its 1px outline border."""
    return x + 1, y + 1, max(x + 1, x + w - 1), max(y + 1, y + h - 1)


def _detail_serrations(pixels, x: int, y: int, w: int, h: int, ramp: Ramp) -> None:
    ix0, iy0, ix1, iy1 = _interior(x, y, w, h)
    for i, xi in enumerate(range(ix0, ix1)):
        tone = ramp.light if i % 2 == 0 else ramp.shade
        for yi in range(iy0, iy1):
            pixels[xi, yi] = tone


def _detail_ejection_port(pixels, x: int, y: int, w: int, h: int, ramp: Ramp) -> None:
    ix0, iy0, ix1, iy1 = _interior(x, y, w, h)
    px0, px1 = ix0 + max(1, (ix1 - ix0) // 4), ix1 - max(1, (ix1 - ix0) // 4)
    py0, py1 = iy0 + max(1, (iy1 - iy0) // 4), iy1 - max(1, (iy1 - iy0) // 4)
    for xi in range(px0, max(px1, px0 + 1)):
        for yi in range(py0, max(py1, py0 + 1)):
            if ix0 <= xi < ix1 and iy0 <= yi < iy1:
                pixels[xi, yi] = ramp.shade


def _detail_rivets(pixels, x: int, y: int, w: int, h: int, ramp: Ramp) -> None:
    ix0, iy0, ix1, iy1 = _interior(x, y, w, h)
    for (xi, yi) in ((ix0, iy0), (max(ix0, ix1 - 1), max(iy0, iy1 - 1))):
        if ix0 <= xi < ix1 and iy0 <= yi < iy1:
            pixels[xi, yi] = ramp.outline


def _detail_wood_grain(pixels, x: int, y: int, w: int, h: int, ramp: Ramp) -> None:
    ix0, iy0, ix1, iy1 = _interior(x, y, w, h)
    for yi in {iy0 + max(1, (iy1 - iy0) // 3), iy0 + max(1, 2 * (iy1 - iy0) // 3)}:
        if iy0 <= yi < iy1:
            for xi in range(ix0, ix1):
                pixels[xi, yi] = ramp.shade


def _detail_witness_lines(pixels, x: int, y: int, w: int, h: int, ramp: Ramp) -> None:
    ix0, iy0, ix1, iy1 = _interior(x, y, w, h)
    for i, yi in enumerate(range(iy0, iy1)):
        if i % 2 == 0:
            for xi in range(ix0, ix1):
                pixels[xi, yi] = ramp.light


def _detail_glint(pixels, x: int, y: int, w: int, h: int, ramp: Ramp) -> None:
    ix0, iy0, ix1, iy1 = _interior(x, y, w, h)
    xi, yi = ix0, iy0
    if ix0 <= xi < ix1 and iy0 <= yi < iy1:
        pixels[xi, yi] = ramp.light


DETAILS = {
    "serrations": _detail_serrations,
    "ejection_port": _detail_ejection_port,
    "rivets": _detail_rivets,
    "wood_grain": _detail_wood_grain,
    "witness_lines": _detail_witness_lines,
    "glint": _detail_glint,
}


def build_atlas(boxes: list[Box]) -> tuple[int, Image.Image, dict[tuple[int, str], tuple[int, int, int, int]]]:
    """One texel per model unit, every box face its own rectangle, shelf-packed into the smallest
    power-of-two square that fits, painted from `palette.py`'s ramps per `FACE_TONE`, then any named
    `face_detail` pattern on top."""
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
        detail = box.face_detail.get(face_name)
        if detail:
            DETAILS[detail](pixels, px, py, w, h, ramp)
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


# ---------------------------------------------------------------- preview renderer (review only,
# not a game asset): rotate every element (its own rotation, then a display pose's rotation) around
# the model's own pivot (8, 8, 8), project orthographically, sample each face's own atlas pixels
# through an inverse affine map, shade by vanilla's directional table, and paint back-to-front.

def _affine_from_correspondence(p_from: list[tuple[float, float]], p_to: list[tuple[float, float]]):
    (x0, y0), (x1, y1), (x2, y2) = p_from
    (u0, v0), (u1, v1), (u2, v2) = p_to
    denom = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0)
    if abs(denom) < 1e-9:
        return None
    a = ((u1 - u0) * (y2 - y0) - (u2 - u0) * (y1 - y0)) / denom
    b = ((u2 - u0) * (x1 - x0) - (u1 - u0) * (x2 - x0)) / denom
    c = u0 - a * x0 - b * y0
    d = ((v1 - v0) * (y2 - y0) - (v2 - v0) * (y1 - y0)) / denom
    e = ((v2 - v0) * (x1 - x0) - (v1 - v0) * (x2 - x0)) / denom
    f = v0 - d * x0 - e * y0
    return a, b, c, d, e, f


# For each face direction, the four 3D corners at (0,0), (w,0), (0,h), (w,h) in that face's own
# atlas-space parametrisation -- matching `_face_dims`'s own (w, h) axis order exactly.
def _face_corner_points(box: Box) -> dict[str, list[tuple[float, float, float]]]:
    x0, y0, z0 = box.frm
    x1, y1, z1 = box.to
    return {
        "up": [(x0, y1, z0), (x1, y1, z0), (x0, y1, z1), (x1, y1, z1)],
        "down": [(x0, y0, z0), (x1, y0, z0), (x0, y0, z1), (x1, y0, z1)],
        "north": [(x0, y0, z0), (x1, y0, z0), (x0, y1, z0), (x1, y1, z0)],
        "south": [(x0, y0, z1), (x1, y0, z1), (x0, y1, z1), (x1, y1, z1)],
        "west": [(x0, y0, z0), (x0, y0, z1), (x0, y1, z0), (x0, y1, z1)],
        "east": [(x1, y0, z0), (x1, y0, z1), (x1, y1, z0), (x1, y1, z1)],
    }


class RenderedFace:
    __slots__ = ("screen", "depth", "atlas", "uv", "wh", "shade")

    def __init__(self, screen, depth, atlas, uv, wh, shade):
        self.screen = screen  # 4 (x, y) screen-space points (unscaled model units), TL,TR,BL,BR
        self.depth = depth
        self.atlas = atlas
        self.uv = uv  # (x0, y0) of this face's rect in the atlas
        self.wh = wh
        self.shade = shade


def _collect_faces(elements: list[Box], atlas_of: dict, uv_of: dict, view_rotation: list[float]) -> list[RenderedFace]:
    faces: list[RenderedFace] = []
    for element_index, box in enumerate(elements):
        atlas = atlas_of[element_index]
        dims = _face_dims(box)
        for face_name in FACES:
            corners3d = _face_corner_points(box)[face_name]
            transformed = []
            for c in corners3d:
                if box.rotation:
                    c = _rotate_axis(c, box.rotation["angle"], box.rotation["axis"], box.rotation["origin"])
                c = _rotate_view(c, view_rotation, PIVOT)
                transformed.append(c)
            screen = [(p[0], 16 - p[1]) for p in transformed]
            depth = sum(p[2] for p in transformed) / 4
            uv = uv_of[(element_index, face_name)][:2]
            faces.append(RenderedFace(screen, depth, atlas, uv, dims[face_name], DIRECTIONAL_SHADE[face_name]))
    return faces


def _draw_faces(canvas: Image.Image, faces: list[RenderedFace], origin_x: float, origin_y: float, px_per_unit: float) -> None:
    faces = sorted(faces, key=lambda f: f.depth)
    out = canvas.load()
    for face in faces:
        tl, tr, bl, br = face.screen
        screen_pts = [(tl[0], tl[1]), (tr[0], tr[1]), (bl[0], bl[1])]
        w, h = face.wh
        atlas_pts = [(0, 0), (w, 0), (0, h)]
        m = _affine_from_correspondence(screen_pts, atlas_pts)
        if m is None:
            continue
        a, b, c, d, e, f = m
        screen_xs = [p[0] for p in (tl, tr, bl, br)]
        screen_ys = [p[1] for p in (tl, tr, bl, br)]
        px0 = int(math.floor(min(screen_xs) * px_per_unit + origin_x))
        px1 = int(math.ceil(max(screen_xs) * px_per_unit + origin_x)) + 1
        py0 = int(math.floor(min(screen_ys) * px_per_unit + origin_y))
        py1 = int(math.ceil(max(screen_ys) * px_per_unit + origin_y)) + 1
        atlas_pixels = face.atlas.load()
        uvx, uvy = face.uv
        for py in range(py0, py1):
            if py < 0 or py >= canvas.height:
                continue
            for px in range(px0, px1):
                if px < 0 or px >= canvas.width:
                    continue
                sx = (px - origin_x) / px_per_unit
                sy = (py - origin_y) / px_per_unit
                ax = a * sx + b * sy + c
                ay = d * sx + e * sy + f
                if ax < 0 or ax >= w or ay < 0 or ay >= h:
                    continue
                sample = atlas_pixels[int(uvx + ax), int(uvy + ay)]
                if len(sample) == 4 and sample[3] == 0:
                    continue
                r, g, bch, al = sample
                out[px, py] = (int(r * face.shade), int(g * face.shade), int(bch * face.shade), al)


def _faces_bbox_center(faces: list[RenderedFace]) -> tuple[float, float]:
    xs = [p[0] for f in faces for p in f.screen]
    ys = [p[1] for f in faces for p in f.screen]
    return (min(xs) + max(xs)) / 2, (min(ys) + max(ys)) / 2


def _centered_origin(faces: list[RenderedFace], target_x: float, target_y: float, px_per_unit: float) -> tuple[float, float]:
    """A render's post-rotation bounding box is rarely centred on the pivot (8, 8, 8) it was
    rotated around -- a rifle's stock and barrel extend unevenly either side of it. Centre on the
    box's own bounding box instead, so it lands in the middle of its cell (or its inventory slot)
    rather than drifting toward whichever end the rotation happened to swing furthest."""
    cx, cy = _faces_bbox_center(faces)
    return target_x - cx * px_per_unit, target_y - cy * px_per_unit


def render_view(canvas: Image.Image, weapon_id: str, loaded: bool, rotation: list[float],
                 target_x: float, target_y: float, px_per_unit: float) -> None:
    elements = _all_elements(weapon_id, loaded)
    atlas_of, uv_of = {}, {}
    for i, box in enumerate(elements):
        size, atlas, uv_map = build_atlas([box])
        atlas_of[i] = atlas
        for face_name in FACES:
            uv_of[(i, face_name)] = uv_map[(0, face_name)]
    faces = _collect_faces(elements, atlas_of, uv_of, rotation)
    origin_x, origin_y = _centered_origin(faces, target_x, target_y, px_per_unit)
    _draw_faces(canvas, faces, origin_x, origin_y, px_per_unit)


def render_sheet(path: Path) -> None:
    px_per_unit = 8
    cell_w, cell_h = 300, 260
    header = 70
    label_h = 20
    columns = ("gui bare", "gui loaded", "thirdperson bare", "thirdperson loaded")
    rows = list(WEAPONS)
    sheet = Image.new("RGBA", (len(columns) * cell_w, header + len(rows) * cell_h + 260), (235, 235, 235, 255))
    draw = ImageDraw.Draw(sheet)
    draw.text((8, 6), "FA-22 round 2 -- rotated, textured preview (gui and thirdperson poses, 8x)", fill=(20, 20, 20, 255))
    for col, title in enumerate(columns):
        draw.text((col * cell_w + 8, 40), title, fill=(90, 90, 90, 255))

    for row, weapon_id in enumerate(rows):
        cy = header + row * cell_h
        draw.text((8, cy + 2), weapon_id, fill=(20, 20, 20, 255))
        origin_x = cell_w // 2
        origin_y = cy + label_h + cell_h // 2
        gui_rot = BASE_DISPLAY["gui"]["rotation"]
        third_rot = BASE_DISPLAY["thirdperson_righthand"]["rotation"]
        for col, (loaded, rotation) in enumerate([(False, gui_rot), (True, gui_rot), (False, third_rot), (True, third_rot)]):
            render_view(sheet, weapon_id, loaded, rotation, col * cell_w + origin_x, origin_y, px_per_unit)

    # Atlases, labelled, along the bottom.
    atlas_y = header + len(rows) * cell_h + 20
    draw.text((8, atlas_y - 16), "atlases", fill=(20, 20, 20, 255))
    x = 8
    for weapon_id, boxes in WEAPON_BOXES.items():
        _, atlas, _ = build_atlas(boxes)
        shown = atlas.resize((atlas.width * 2, atlas.height * 2), Image.NEAREST)
        sheet.paste(shown, (x, atlas_y), shown)
        draw.text((x, atlas_y + shown.height + 2), weapon_id, fill=(60, 60, 60, 255))
        x += shown.width + 16

    # A mock 3x3 inventory grid at 2x, gui pose, gui scale -- the actual slot read.
    grid_y = atlas_y + 100
    draw.text((8, grid_y - 16), "3x3 inventory mock (2x, gui pose and scale)", fill=(20, 20, 20, 255))
    slot_px = 32  # a 16-unit slot at 2 pixels per unit
    gap = 6
    weapon_ids = list(WEAPONS)
    for i in range(9):
        gx, gy = i % 3, i // 3
        slot_x = 8 + gx * (slot_px + gap)
        slot_y = grid_y + gy * (slot_px + gap)
        draw.rectangle([slot_x, slot_y, slot_x + slot_px, slot_y + slot_px], outline=(120, 120, 120, 255), width=1)
        if i >= len(weapon_ids):
            continue
        weapon_id = weapon_ids[i]
        weapon_class = WEAPONS[weapon_id][0]
        display = class_display(weapon_class)
        scale = display["gui"]["scale"][0]
        rotation = display["gui"]["rotation"]
        elements = _all_elements(weapon_id, loaded=True)
        atlas_of, uv_of = {}, {}
        for idx, box in enumerate(elements):
            size, atlas, uv_map = build_atlas([box])
            atlas_of[idx] = atlas
            for face_name in FACES:
                uv_of[(idx, face_name)] = uv_map[(0, face_name)]
        faces = _collect_faces(elements, atlas_of, uv_of, rotation)
        # `_collect_faces` already applied rotation around PIVOT; apply the gui scale around the
        # render's own bounding-box centre (not the pivot -- see `_centered_origin`), then centre
        # the scaled result in the slot, at 2 pixels per model unit.
        cx, cy = _faces_bbox_center(faces)
        for face in faces:
            face.screen = [(cx + (x - cx) * scale, cy + (y - cy) * scale) for (x, y) in face.screen]
        origin_x, origin_y = _centered_origin(faces, slot_x + slot_px / 2, slot_y + slot_px / 2, 2)
        _draw_faces(sheet, faces, origin_x, origin_y, 2)

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
