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

Round 3 (Kevin's in-game verdict on round 2: "all weapons kind of look like sniper rifles; they
point at the player instead of away from him, which is weird; the rotation in the inventory/hotbar
also looks weird"): two bugs and one proportion problem, all fixed here.

1. **Orientation was backwards.** Round 2's docstring claimed "+Z is the muzzle end" -- the
   opposite of Create Fly's own potato cannon, decompiled and checked directly
   (`unzip -p ... assets/create/models/item/potato_cannon/item.json`): its elements run from
   `z=-2.5` (the muzzle tip) to `z=17.5` (the stock/grip end), and its `display` block -- the exact
   block round one and two copied verbatim into `BASE_DISPLAY` below -- was authored against *that*
   orientation. Every base and part box, and every rotation angle and origin, is mirrored on Z
   (`z' = -z`, folded into the round-3 geometry directly rather than kept as a separate pass) so
   **-Z is the muzzle end, +Z is the stock/grip end**, matching the cannon exactly. Mirroring a
   single axis flips handedness, so any element `rotation` around the `x` or `y` axis (whose plane
   includes Z) has its angle negated too; a `z`-axis rotation (whose plane excludes Z) is
   unaffected. `test_models.py::test_muzzle_end_is_the_smallest_z` locks this down per weapon.
2. **Every class was scaled to fill the 16-unit slot**, so a pistol and the AWM read at the same
   size and nothing looked like its own weapon. Round 3 uses one `SHARED_SCALE` (below) for every
   class, derived from the AWM alone -- the longest weapon at its own real 24-unit length -- so a
   pistol is visibly small and a shotgun visibly mid-length, the way Create's own items keep their
   real relative sizes. Every base's own box list was rebuilt to real relative proportions: pistol
   and Micro Uzi bulked up (a full-size grip, a thicker stubby receiver) rather than thin sticks
   stretched to fit; every barrel is 1.5 units across, not 1.
3. **The inventory/hotbar rotation was the raw potato-cannon values**, which happen to work for the
   cannon's own geometry but not for a rifle-shaped assembly authored independently. `gui`, `ground`
   and `fixed` are retuned below to the diagonal side-on read vanilla tools and the cannon's own
   icon use, `thirdperson_righthand` to point the barrel down the arm, `firstperson_righthand` to a
   hip-fire pose; `AIM_TRANSFORMATION` (unchanged, still additive) still moves the model on top.

The box DSL: each weapon and each attachment part is a list of `Box(from_xyz, to_xyz, material)`
in model units (16 units = 1 block), optionally with a `rotation` (a single-axis Blockbench-style
element rotation, angle one of -45/-22.5/0/22.5/45), a `face_material` override for a detail's own
material (a scope's glass lens, a compensator's copper port), and a `face_detail` naming a pixel
pattern to paint on top of a face's flat tone. The weapon lies along the Z axis exactly as Create's
cannon does: -Z is the muzzle end, +Z is the stock/grip end, near the player's hand.

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
import io
import json
import math
import zipfile
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
    "bb7bc244-01b6-4890-9ca3-da5d384dc21e/scratchpad/fa22-round3.png"
)

FACES = ("north", "south", "east", "west", "up", "down")

# Lighting convention (WEAPON-REQ-017, DEC-018): up and west are lit; north is the one plain "base"
# side face; down, east and south are in shade. Kept as one fixed table so every box, weapon and
# part is shaded the same way regardless of which function built it.
FACE_TONE = {"up": "light", "west": "light", "north": "base", "south": "shade", "east": "shade", "down": "shade"}

# Vanilla's own directional face shading, applied by the preview renderer on top of the atlas's own
# painted tones -- the same multiplier the game engine applies to every block/item face.
DIRECTIONAL_SHADE = {"up": 1.0, "down": 0.5, "north": 0.8, "south": 0.8, "east": 0.6, "west": 0.6}

# The aiming pose of the base layer (FA-9): 26.2's `minecraft:model` item-model `transformation` is
# `com.mojang.math.Transformation.CODEC`, a record of all four fields -- a translation alone fails to
# parse and takes the whole `firearms:weapon` definition down to the missing model (FA-23).
#
# FA-24 (docs/spec/decisions/DEC-019-controls.md): this transformation composes additively with
# whatever per-ItemDisplayContext BASE_DISPLAY translation the referenced weapon model already
# carries (only ever rendered while minecraft:using_item is true, i.e. first/thirdperson only --
# nothing "uses" an item in a GUI/inventory/ground/fixed slot). The round-1 value above was a
# near-zero nudge, not a deliberate pose; now that use() is aim-only (WEAPON-REQ-019) the aiming
# pose needs to read as a clear, deliberate change from the hip-fire pose toward screen centre.
#
# Round 3 (Kevin, item 3: "the aiming transformation from AIM_TRANSFORMATION stays additive"): this
# delta itself is unchanged -- it was chosen relative to round 2's own
# `BASE_DISPLAY["firstperson_righthand"]` translation, which round 3 replaced (`[0.25, 5, 0.75]` ->
# `[1, 3, 1]`) along with the muzzle convention it was reasoned against (round 2's docstring
# claimed +Z was the muzzle end; the module docstring's orientation section above corrects that to
# -Z). Composed on top of the new hip pose the numbers still move the gun up and in toward screen
# centre and slightly toward the camera (z is negative, and -Z is now the muzzle direction, so this
# still pulls the weapon back toward the player's hand rather than pushing the muzzle away) -- the
# same direction of change as before, just starting from a different hip pose underneath it.
# Proposed, retune at the sweep -- this is a numeric aesthetic tuning that needs in-game visual
# confirmation, the same convention this project's own spec uses for every unconfirmed number.
AIM_TRANSFORMATION = {
    "translation": [-0.2, -3.0, -0.35],
    "left_rotation": [0.0, 0.0, 0.0, 1.0],
    "scale": [1.0, 1.0, 1.0],
    "right_rotation": [0.0, 0.0, 0.0, 1.0],
}


@dataclass(frozen=True)

class Box:
    frm: tuple[float, float, float]
    to: tuple[float, float, float]
    material: str
    face_material: dict[str, str] = field(default_factory=dict)
    face_detail: dict[str, str] = field(default_factory=dict)
    rotation: dict | None = None  # {"angle": ..., "axis": "x"|"y"|"z", "origin": (x, y, z)}
    # Metadata only -- never touches the written model JSON. Tags the one box that is this base's
    # own muzzle/barrel or its own stock/grip end, so `test_models.py` can check round 3's
    # orientation fix (`test_muzzle_end_is_the_smallest_z`) without guessing which element is which.
    role: str | None = None  # "muzzle" | "stock" | None


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
    # Pistol and Micro Uzi are hand-built to their round-3 proportions rather than a scale of round
    # 2's geometry (Kevin's round-2 verdict: "all weapons kind of look like sniper rifles" -- round
    # 2's pistol/SMG receivers were the same thin 2x2 cross-section as the rifles' 3x4, just shorter,
    # so nothing but length told them apart). Both now share the rifles' own 3-wide receiver
    # footprint (`WEAPON-REQ-016`), a full-size grip/stock, and total length close to their own
    # `WEAPON-REQ-002` roster proportions (a pistol dwarfed by the AWM, not a shrunk copy of it).
    "m1911": [
        Box((7.25, 7.75, -2.6), (8.75, 9.25, -0.6), "steel", role="muzzle"),  # barrel, 1.5 thick, flush to the slide
        Box((6.5, 8, -0.6), (9.5, 10, 2.2), "steel", face_material={"east": "black"},
            face_detail={"west": "serrations", "east": "ejection_port"}),  # slide, full 3-wide receiver footprint
        Box((6.5, 6, -0.6), (9.5, 8, 2.6), "gunmetal", face_detail={"north": "rivets"}),  # lower frame, flush to the slide
        Box((7.6, 10, -0.5), (8.4, 10.4, -0.1), "steel"),  # front sight, flush on the slide's top, near the muzzle
        Box((7.6, 10, 1.8), (8.4, 10.4, 2.2), "steel"),  # rear sight, flush on the slide's top, near the grip
        Box((7.6, 10.4, 1.8), (8.4, 10.9, 2.4), "gunmetal"),  # hammer spur, atop the rear sight
        Box((7.3, 5.6, 0.3), (7.7, 6, 0.7), "gunmetal"),  # trigger guard, front leg
        Box((7.3, 5.3, 0.3), (7.7, 5.6, 1.5), "gunmetal"),  # trigger guard, bottom
        Box((7.3, 5.6, 1.1), (7.7, 6, 1.5), "gunmetal"),  # trigger guard, rear leg
        Box((6.5, 2, 2.0), (9.5, 6, 5.4), "dark_oak", face_detail={"north": "wood_grain"},
            rotation={"angle": -22.5, "axis": "x", "origin": (8, 6, 2.3)}, role="stock"),  # full grip, angled back and down
        Box((7.3, 1, 2.8), (8.7, 3, 4.6), "black", face_detail={"north": "witness_lines"}),  # magazine, inserted into the grip
    ],
    "micro_uzi": [
        Box((7.25, 7.75, -4.0), (8.75, 9.25, -2.0), "gunmetal", role="muzzle"),  # short barrel, 1.5 thick
        Box((6, 6.5, -2.0), (10, 10.5, 2.0), "gunmetal", face_detail={"north": "rivets"}),  # stubby 4-thick receiver
        Box((7.6, 10.5, -1.9), (8.4, 10.9, -1.5), "steel"),  # front sight
        Box((7.6, 10.5, 1.6), (8.4, 10.9, 2.0), "steel"),  # rear sight
        Box((7, 1, 0.2), (9, 6.5, 1.8), "black", face_detail={"north": "witness_lines"}),  # magazine-in-grip
        Box((7, 9.8, 2.0), (9, 10.6, 6.0), "steel"),  # folded wire stock, flat along the top, flush to the receiver
        Box((7, 8.6, 5.3), (7.6, 10.6, 6.0), "steel", role="stock"),  # its rear hoop, dropping down
        Box((7.3, 6.1, -1.0), (7.7, 6.5, -0.6), "gunmetal"),  # trigger guard, front leg
        Box((7.3, 5.8, -1.0), (7.7, 6.1, 0.2), "gunmetal"),  # trigger guard, bottom
        Box((7.3, 6.1, -0.2), (7.7, 6.5, 0.2), "gunmetal"),  # trigger guard, rear leg
    ],
    "akm": [
        Box((6.5, 6, -5.2273), (9.5, 10, 3.8636), "steel", face_detail={"north": "rivets"}),  # receiver
        Box((7.25, 7.25, -10.9091), (8.75, 8.75, -5.2273), "steel", role="muzzle"),  # long barrel, 1.5 thick
        Box((7.8, 8.75, -10.6818), (8.2, 9.15, -10.2273), "gunmetal"),  # front sight, flush on the barrel's new top
        Box((7.8, 10, 0.6818), (8.2, 10.4, 1.1364), "gunmetal"),  # rear sight, flush on the receiver's top
        Box((7.7, 8.75, -10.0), (8.3, 9.15, -1.1364), "gunmetal"),  # gas tube, flush on the barrel's new top
        Box((7, 6.5, -5.6818), (9, 7.25, -1.1364), "oak", face_detail={"west": "wood_grain"}),  # handguard, flush to the barrel
        Box((7, 5.3, 1.3636), (7.5, 6, 1.8182), "gunmetal"),  # trigger guard, front leg
        Box((7, 5.0, -0.2273), (7.5, 5.3, 1.8182), "gunmetal"),  # trigger guard, bottom
        Box((7, 2, 2.0455), (9, 6, 4.3182), "dark_oak", face_detail={"north": "wood_grain"},
            rotation={"angle": 22.5, "axis": "x", "origin": (8, 6, 2.2727)}),  # pistol grip
        Box((7.3, 3.5, -1.7045), (8.7, 6, -0.0), "black", face_detail={"north": "witness_lines"},
            rotation={"angle": -22.5, "axis": "x", "origin": (8, 6, -1.7045)}),  # curved magazine, upper segment
        Box((7.3, 1.0, -2.6136), (8.7, 3.5, -0.9091), "black",
            rotation={"angle": -45, "axis": "x", "origin": (8, 3.5, -1.7614)}),  # curved magazine, lower segment
        Box((6.7, 6.4, 3.8636), (9.3, 9, 8.6364), "spruce", face_detail={"west": "wood_grain"}),  # wood stock, flush to the receiver's rear
        Box((6.7, 6.4, 8.6364), (9.3, 9, 9.0909), "spruce", role="stock"),  # buttplate
    ],
    "ruger_mini_14": [
        Box((6.5, 6, -4.9126), (9.5, 10, 3.6311), "gunmetal", face_detail={"north": "rivets"}),  # receiver
        Box((7.25, 7.25, -12.3883), (8.75, 8.75, -4.9126), "steel", role="muzzle"),  # long barrel, 1.5 thick
        Box((7.8, 8.75, -12.1748), (8.2, 9.15, -11.7476), "gunmetal"),  # front sight, flush on the barrel's new top
        Box((7.8, 10, 0.6408), (8.2, 10.4, 1.068), "gunmetal"),  # rear sight, flush on the receiver's top
        Box((6.7, 6.4, 3.6311), (9.3, 9, 9.1845), "spruce", face_detail={"west": "wood_grain"}),  # wood stock, flush to the receiver's rear
        Box((6.7, 6.4, 9.1845), (9.3, 9, 9.6117), "spruce", role="stock"),  # buttplate
        Box((7, 9, 8.1165), (9, 9.5, 8.7573), "spruce"),  # comb, flush on the stock's top
        Box((7, 5.6, -5.3398), (9, 6, -1.068), "spruce", face_detail={"west": "wood_grain"}),  # full stock running under the barrel
        Box((7, 5.3, 1.2816), (7.5, 6, 1.7087), "gunmetal"),  # trigger guard, front leg
        Box((7, 5.0, -0.2136), (7.5, 5.3, 1.7087), "gunmetal"),  # trigger guard, bottom
        Box((7.3, 3.5, -1.068), (8.7, 6, 0.534), "gunmetal", face_detail={"north": "witness_lines"}),  # small box magazine
    ],
    "awm": [
        Box((6.5, 6, -4.973), (9.5, 10, 3.6757), "gunmetal", face_detail={"north": "rivets"}),  # receiver
        Box((7.25, 7.25, -13.6216), (8.75, 8.75, -4.973), "steel", role="muzzle"),  # longest barrel, 1.5 thick
        Box((9.5, 8, -0.5405), (10.3, 8.6, 0.5405), "steel"),  # bolt handle, flush on the receiver's side
        Box((7.3, 10, -3.2432), (8.7, 10.4, 2.1622), "steel"),  # scope rail, flush on the receiver's top
        Box((6.6, 4.8, -13.1892), (7.25, 7.6, -12.5405), "steel"),  # bipod, left leg, flush to the 1.5-thick barrel
        Box((8.75, 4.8, -13.1892), (9.4, 7.6, -12.5405), "steel"),  # bipod, right leg, flush to the 1.5-thick barrel
        Box((6.8, 5.5, 3.6757), (9.2, 9, 9.9459), "polymer"),  # olive stock, flush to the receiver's rear
        Box((6.8, 5.5, 9.9459), (9.2, 9, 10.3784), "polymer", role="stock"),  # buttplate
        Box((7, 9, 5.4054), (9, 9.6, 7.5676), "polymer"),  # cheek riser, flush on the stock's top
        Box((7.3, 4.8, -1.0811), (8.7, 6, 0.5405), "gunmetal", face_detail={"north": "witness_lines"}),  # flush magazine
        Box((7, 5.3, 1.2973), (7.5, 6, 1.7297), "gunmetal"),  # trigger guard, front leg
        Box((7, 5.0, -0.2162), (7.5, 5.3, 1.7297), "gunmetal"),  # trigger guard, bottom
    ],
    "winchester_model_1897": [
        Box((6.5, 6, -4.4737), (9.5, 10, 4.4737), "steel", face_detail={"north": "rivets"}),  # receiver
        Box((7.25, 7.25, -11.0526), (8.75, 8.75, -4.4737), "steel", role="muzzle"),  # barrel, 1.5 thick
        Box((7.8, 8.75, -10.7895), (8.2, 9.15, -10.2632), "steel"),  # bead front sight, flush on the barrel's new top
        Box((7.7, 10, 4.3421), (8.3, 10.6, 4.8684), "gunmetal"),  # exposed hammer, flush on the receiver's top
        Box((7.7, 5.4, -10.5263), (8.3, 6.0, -0.0), "brass", face_detail={"north": "witness_lines"}),  # brass tube magazine
        Box((7.3, 4.6, -5.9211), (8.7, 5.4, -1.9737), "oak", face_detail={"west": "wood_grain"}),  # pump forend, flush to the tube
        Box((6.7, 6.4, 4.4737), (9.3, 9, 8.4211), "oak", face_detail={"west": "wood_grain"}),  # wood stock, flush to the receiver's rear
        Box((6.7, 6.4, 8.4211), (9.3, 9, 8.9474), "oak", role="stock"),  # buttplate
        Box((7, 5.3, 1.5789), (7.5, 6, 2.1053), "gunmetal"),  # trigger guard, front leg
        Box((7, 5.0, -0.2632), (7.5, 5.3, 2.1053), "gunmetal"),  # trigger guard, bottom
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
    # Round 3: mirrored on Z so the objective end (the longer, magnification-scaled end) still
    # reaches toward the muzzle, now -Z, instead of into the receiver.
    return [Box((-1, 0, -end_z), (1, 2, 2.5), "steel", face_material={"north": "glass"},
                face_detail={"north": "glint"})]


# Round 3: every part box mirrored on Z (`z' = -z`, angle negated for an x/y-axis rotation, kept for
# a z-axis one) to match the base geometry's own flip. Most of these were already Z-symmetric and so
# are unchanged in effect; `angled_grip`'s rotation and every muzzle/optic/stock box (all
# deliberately asymmetric on Z, per their own "extends toward the muzzle" / "extends backward"
# comments) are the ones that actually moved.
PART_BOXES: dict[str, dict[str, list[Box]]] = {
    "muzzle": {
        "suppressor": [Box((-0.6, -0.5, -5), (0.6, 0.5, 1), "gunmetal", face_detail={"north": "rivets"})],
        "flash_hider": [
            Box((-0.6, -0.5, -1), (0.6, 0.5, 1), "gunmetal"),
            Box((-0.3, -0.3, -2.5), (0.3, 0.3, -1), "gunmetal"),
            Box((-0.6, -0.3, -2.2), (-0.35, 0.3, -1), "gunmetal"),
            Box((0.35, -0.3, -2.2), (0.6, 0.3, -1), "gunmetal"),
        ],
        "compensator": [Box((-0.6, -0.5, -1.5), (0.6, 0.5, 1), "steel", face_material={"up": "copper"})],
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
                             rotation={"angle": 22.5, "axis": "x", "origin": (0, 0, 0)})],
        "thumb_grip": [Box((-1, -1.2, -1), (1, 0, 1), "dark_oak", face_detail={"north": "wood_grain"})],
    },
    "stock": {
        # local z starts at 0 -- flush against the base's rearmost fixed surface -- and extends
        # backward (positive z, now that -Z is the muzzle) from there, so the part sits cleanly
        # behind the fixed stock rather than overlapping into it.
        "tactical_stock": [Box((-1.5, -1.5, 0), (1.5, 1.5, 1), "black")],
        "cheek_pad": [Box((-1.2, 0, 0), (1.2, 1, 2), "dark_oak", face_detail={"north": "wood_grain"})],
        "bullet_loops": [Box((-1.5, -0.2, 0), (1.5, 0.2, 4), "dark_oak", face_material={"north": "brass"})],
    },
}

# class -> slot -> (x, y, z) translation placing that slot's part in the class's own weapon space,
# read directly off each class's own weapon geometry above so every anchor is provably flush with
# or overlapping it (verified by `tools/test_models.py::test_no_floating_elements`).
PART_ANCHOR: dict[str, dict[str, tuple[float, float, float]]] = {
    "pistol": {"muzzle": (8, 8.5, -2.6), "optic": (8, 10, 2.0), "magazine": (8, 1, 3.7)},
    "smg": {
        "muzzle": (8, 8.5, -4.0), "optic": (8, 10.5, 1.8), "magazine": (8, 1, 1.0),
        "grip": (8, 6.5, -0.5), "stock": (8, 9.8, 6.0),
    },
    "assault_rifle": {
        "muzzle": (8, 8, -10.9091), "optic": (8, 10, 0.9091), "magazine": (8, 1.0, -1.7614),
        "grip": (8, 6.5, -3.4091), "stock": (8, 7.7, 9.0909),
    },
    "dmr": {"muzzle": (8, 8, -12.3883), "optic": (8, 10, 0.8544), "magazine": (8, 3.5, -0.267), "stock": (8, 7.7, 9.6117)},
    "sniper_rifle": {
        "muzzle": (8, 8, -13.6216), "optic": (8, 10.4, -0.5405), "magazine": (8, 4.8, -0.2703), "stock": (8, 7.25, 10.3784),
    },
    "shotgun": {"muzzle": (8, 8, -11.0526), "magazine": (8, 5.4, -5.2632)},
}

# Round 3 display transforms (Kevin's round-2 verdict: "they point at the player instead of away
# from him... the rotation in the inventory/hotbar also looks weird"). Round 1/2 simply copied
# Create's own potato-cannon `display` block verbatim, cannon geometry and all -- it happened to
# read fine for a shape authored specifically against those numbers, and wrong for ours (see the
# module docstring's orientation section). Round 3 keeps only what generalises -- vanilla applies
# `rotation` as Euler X, Y, Z in that order around the model's own pivot (8, 8, 8), exactly what
# `_rotate_view` below already does, and what the preview renderer has used since round 1 -- and
# replaces every numeric value:
#
# - `gui`: a diagonal side-on read, the way vanilla draws a bow/trident/fishing rod across its own
#   16x16 canvas and the way the cannon's own icon reads (muzzle top-right, stock bottom-left) --
#   `[30, -135, 0]` tips the barrel toward the viewer and swings it across the slot on the diagonal;
#   `fixed` is the same idea held to a plain profile silhouette (`[0, 90, 0]`, a pure side view, no
#   tilt) the way an item frame shows a sword edge-on.
# - `ground`: no rotation at all -- the weapon already lies along its own Z axis, so identity *is*
#   "lying flat", unlike the cannon (modelled with its own long axis off Z) which needed a 90-degree
#   correction.
# - `thirdperson_righthand`: no rotation, translated down and back along the arm
#   (`[0, 1, -2]`) so the barrel (Z) continues the forearm's own direction instead of the cannon's
#   -15-degree swing, which pointed a Z-mirrored barrel at the player exactly as Kevin described.
# - `firstperson_righthand`: a small downward tilt (`[0, -5, 0]`) and a hip-carry translation
#   (`[1, 3, 1]`); `AIM_TRANSFORMATION` (unchanged, still additive) raises this to eye level.
#
# `gui`/`ground`/`fixed` get one `SHARED_SCALE` (below), not a per-class fit-to-slot scale, so real
# relative size survives into every context, in-hand included -- an AWM held at its own 24 units next
# to a pistol at 8 looks the way a real AWM next to a real M1911 looks, not two copies of the same
# rifle scaled to fill a box (Kevin, round 2: "all weapons kind of look like sniper rifles").
BASE_DISPLAY = {
    "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 1, -2]},
    "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 1, -2]},
    "firstperson_righthand": {"rotation": [0, -5, 0], "translation": [1, 3, 1]},
    "firstperson_lefthand": {"rotation": [0, -5, 0], "translation": [1, 3, 1]},
    "ground": {"rotation": [0, 0, 0]},
    "gui": {"rotation": [30, -135, 0]},
    "fixed": {"rotation": [0, 90, 0]},
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


def _rotated_bbox_center_and_extent(weapon_id: str, rotation: list[float]) -> tuple[tuple[float, float, float], float]:
    """The fully-loaded weapon's own bounding-box centre and largest (x or y) extent after a given
    display rotation (no scale, no translation applied) -- what `SHARED_SCALE` fits into the
    16-unit slot for the longest weapon, and what every class's own `gui`/`ground`/`fixed`
    translation centres for its own (shorter or longer) silhouette."""
    xs: list[float] = []
    ys: list[float] = []
    zs: list[float] = []
    for box in _all_elements(weapon_id, loaded=True):
        for corner in _element_corners_after_own_rotation(box):
            rx, ry, rz = _rotate_view(corner, rotation, PIVOT)
            xs.append(rx)
            ys.append(ry)
            zs.append(rz)
    center = ((max(xs) + min(xs)) / 2, (max(ys) + min(ys)) / 2, (max(zs) + min(zs)) / 2)
    extent = max(max(xs) - min(xs), max(ys) - min(ys))
    return center, extent


def _context_translation(weapon_id: str, rotation: list[float], scale: float) -> list[float]:
    """Vanilla scales and rotates an item's `elements` about the fixed pivot (8, 8, 8) and only then
    adds `translation` untouched -- so centring the model's own (rotated, scaled) bounding box in
    its slot/frame/ground tile is `scale * (pivot - bbox_center)` per axis (`WEAPON-REQ-016` round
    3, item 3: "translation to centre the model's own bounding box")."""
    (cx, cy, cz), _extent = _rotated_bbox_center_and_extent(weapon_id, rotation)
    return [round(scale * (PIVOT[0] - cx), 4), round(scale * (PIVOT[1] - cy), 4), round(scale * (PIVOT[2] - cz), 4)]


# One shared scale for every class (Kevin, round 3, item 2), derived from the AWM alone -- the
# longest weapon at its own real 24-unit length -- fit to the 16-unit slot under the `gui` rotation.
# Every shorter class uses this exact same number, so their own real (smaller) size shows through
# instead of being stretched back up to fill the slot.
_AWM_ID = WEAPON_FOR_CLASS["sniper_rifle"]
_, _AWM_GUI_EXTENT = _rotated_bbox_center_and_extent(_AWM_ID, BASE_DISPLAY["gui"]["rotation"])
SHARED_SCALE = round(0.94 * 16 / _AWM_GUI_EXTENT, 4)


def class_display(class_name: str) -> dict:
    weapon_id = WEAPON_FOR_CLASS[class_name]
    display = copy.deepcopy(BASE_DISPLAY)
    for context in ("ground", "gui", "fixed"):
        display[context]["scale"] = [SHARED_SCALE, SHARED_SCALE, SHARED_SCALE]
        display[context]["translation"] = _context_translation(weapon_id, display[context]["rotation"], SHARED_SCALE)
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
#
# FA-26 (Kevin, "the 3D models still show the missing-texture default"): 26.2's cuboid item-model
# format (`net.minecraft.client.resources.model.cuboid.*`, the package `FaceBakery.bakeQuad` in the
# crash lives in) has NO notion of a per-model `texture_size` at all -- confirmed by decompiling the
# shipped client: `CuboidModel$Deserializer` never reads a `texture_size` key, and both
# `CuboidFace.getU`/`getV` and `FaceBakery.computeMaterialTransparency` divide every raw `uv` number
# by a hardcoded 16.0F before multiplying by the *real* PNG's own pixel dimensions
# (`SpriteContents.computeTransparency`: `x0 = floor(u0 * this.width)` with `u0 = rawU / 16`). `uv`
# is always authored against a fixed nominal 16-unit face space, exactly like `from`/`to`, regardless
# of how large the backing atlas PNG actually is; a `"texture_size"` key some vanilla model files
# still carry (e.g. `block/heavy_core.json`, `[16, 16]`) is dead JSON kept only for
# human/Blockbench documentation, never consulted by any baking code.
#
# Round 1/2 of this atlas packer wrote `uv` directly in atlas-pixel units (one texel per model unit,
# packed into atlases as large as 64x64) under the old-BlockModel assumption that a declared
# `texture_size` would rescale them -- it doesn't, so any face packed past pixel 16 on either axis
# produced a `uv` value vanilla's fixed `/16` divide, then re-multiplied by the atlas's real pixel
# width, pushed outside the image (`Cannot compute translucency out of bounds: [52, 0, 60, 4] in
# 32x32 image`: a face whose packer rectangle started at raw pixel 26 read back as
# `floor((26/16)*32) = 52`), and every other face -- even ones that happened not to crash -- sampled
# the wrong region of its own atlas. The fix: convert every packer pixel rectangle into that fixed
# 16-unit space at generation time (`_PX_TO_UV16`), so vanilla's own `(uv/16)*realPixelWidth` recovers
# the exact original pixel rectangle. `texture_size` is still written (matches `docs/spec/decisions/
# DEC-018-art-direction.md`, and lets `ModelAssetsTest`/`test_models.py` cross-check the atlas PNG's
# real size against what the generator packed) but no longer plays any role in how `uv` is read.

def _px_to_uv16(px: float, atlas_size: int) -> float:
    """A packer pixel coordinate (0..`atlas_size`) as vanilla's fixed nominal 16-unit `uv` value,
    chosen so that vanilla's own `(uv / 16) * realPixelWidth` (with `realPixelWidth == atlas_size`,
    always true here since the PNG is saved at exactly the packed size) recovers `px` exactly."""
    return round(px * 16.0 / atlas_size, 6)


def _element(box: Box, uv_map, box_index: int, size: int, offset: tuple[float, float, float] = (0, 0, 0)) -> dict:
    ox, oy, oz = offset
    frm = [box.frm[0] + ox, box.frm[1] + oy, box.frm[2] + oz]
    to = [box.to[0] + ox, box.to[1] + oy, box.to[2] + oz]
    faces = {}
    for face_name in FACES:
        x0, y0, x1, y1 = uv_map[(box_index, face_name)]
        faces[face_name] = {
            "uv": [_px_to_uv16(x0, size), _px_to_uv16(y0, size), _px_to_uv16(x1, size), _px_to_uv16(y1, size)],
            "texture": "#0",
        }
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
        "elements": [_element(box, uv_map, i, size) for i, box in enumerate(boxes)],
    }


def part_model_json(slot: str, name: str, class_name: str, boxes: list[Box], anchor, size: int, uv_map) -> dict:
    texture_ref = f"{NS}:item/weapon/part/{slot}_{name}"
    return {
        "parent": f"{NS}:item/weapon/class/{class_name}",
        "texture_size": [size, size],
        "textures": {"0": texture_ref, "particle": texture_ref},
        "elements": [_element(box, uv_map, i, size, offset=anchor) for i, box in enumerate(boxes)],
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
            "on_true": {**base_model, "transformation": AIM_TRANSFORMATION},
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
# not a game asset): rotate every element (its own rotation, then a display pose's full
# rotation/scale/translation) around the model's own pivot (8, 8, 8), project orthographically,
# sample each face's own atlas pixels through an inverse affine map, shade by vanilla's directional
# table, and paint back-to-front.
#
# Round 3 (Kevin: "verify your preview renderer uses the same order and pivot"): round 1/2's
# renderer only ever applied a display context's `rotation`, then independently recentred whatever
# came out in its own bounding box -- it never actually exercised `scale` or `translation`, so it
# could not have caught round 2's wrong-orientation display block, and it cannot be calibrated
# against a real reference. `_apply_display` now applies vanilla's own composition -- rotate around
# the pivot, then scale around the pivot (uniform scale, so this commutes with the rotate step
# either order), then add `translation` untouched -- and `_display_origin` maps model-space (8, 8)
# to the caller's target point unconditionally, the same fixed camera framing vanilla itself uses,
# instead of re-centring per render. `_context_translation` (above) was derived against this exact
# composition, so a class's own computed `gui`/`ground`/`fixed` translation lands its bounding box
# exactly on the pivot -- and `render_cannon` below feeds Create's own real elements, atlas and
# `display` block through this same pipeline as a calibration check.

def _apply_display(p: tuple[float, float, float], display: dict) -> tuple[float, float, float]:
    rotation = display.get("rotation", [0, 0, 0])
    scale = display.get("scale", [1.0, 1.0, 1.0])[0]
    translation = display.get("translation", [0, 0, 0])
    p = _rotate_view(p, rotation, PIVOT)
    p = tuple(PIVOT[i] + (p[i] - PIVOT[i]) * scale for i in range(3))
    return (p[0] + translation[0], p[1] + translation[1], p[2] + translation[2])


def _display_origin(target_x: float, target_y: float, px_per_unit: float) -> tuple[float, float]:
    """Model-space (8, 8) -- the pivot's own x/y, screen-flipped the same way `_collect_faces`
    flips every point -- maps to `(target_x, target_y)` unconditionally: vanilla's own camera for
    every display context is fixed, not adapted per item, so a display transform's whole job is to
    place the model relative to that fixed point (`_context_translation` does exactly that)."""
    return target_x - PIVOT[0] * px_per_unit, target_y - (16 - PIVOT[1]) * px_per_unit


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


def _collect_faces(elements: list[Box], atlas_of: dict, uv_of: dict, display: dict,
                    wh_of: dict | None = None) -> list[RenderedFace]:
    """`display` is a full display-context dict (`rotation`, optional `scale`, optional
    `translation`), applied by `_apply_display`. `wh_of`, when given, overrides `_face_dims`'s own
    unit-per-texel assumption with each face's real atlas pixel size -- needed for `render_cannon`,
    whose real atlas is not packed at our own 1-texel-per-unit convention; every face our own
    generator packs keeps `_face_dims`'s answer, since we chose that convention ourselves."""
    faces: list[RenderedFace] = []
    for element_index, box in enumerate(elements):
        atlas = atlas_of[element_index]
        dims = _face_dims(box)
        for face_name in FACES:
            key = (element_index, face_name)
            if key not in uv_of:
                continue  # the cannon's own real model omits hidden faces; nothing else does.
            corners3d = _face_corner_points(box)[face_name]
            transformed = []
            for c in corners3d:
                if box.rotation:
                    c = _rotate_axis(c, box.rotation["angle"], box.rotation["axis"], box.rotation["origin"])
                c = _apply_display(c, display)
                transformed.append(c)
            screen = [(p[0], 16 - p[1]) for p in transformed]
            depth = sum(p[2] for p in transformed) / 4
            uv = uv_of[key][:2]
            wh = wh_of[key] if wh_of is not None else dims[face_name]
            faces.append(RenderedFace(screen, depth, atlas, uv, wh, DIRECTIONAL_SHADE[face_name]))
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


def render_view(canvas: Image.Image, weapon_id: str, loaded: bool, display: dict,
                 target_x: float, target_y: float, px_per_unit: float) -> None:
    elements = _all_elements(weapon_id, loaded)
    atlas_of, uv_of = {}, {}
    for i, box in enumerate(elements):
        size, atlas, uv_map = build_atlas([box])
        atlas_of[i] = atlas
        for face_name in FACES:
            uv_of[(i, face_name)] = uv_map[(0, face_name)]
    faces = _collect_faces(elements, atlas_of, uv_of, display)
    origin_x, origin_y = _display_origin(target_x, target_y, px_per_unit)
    _draw_faces(canvas, faces, origin_x, origin_y, px_per_unit)


# ---------------------------------------------------------------- Create's own potato cannon,
# decompiled straight from its jar, as the round-3 calibration row (Kevin, item 3): our renderer
# fed Create's own real elements, real atlas and real `display` block must reproduce Create's own
# known GUI icon (diagonal, muzzle top-right) and held pose (pointing forward) -- otherwise the
# renderer's transform math is wrong, not just our own numbers, and nothing else on the sheet can
# be trusted. A dev-machine aid only, never a build dependency: `main()` skips this row with a
# printed note when the jar isn't cached locally, so `ModelsCommandTest`'s `python3 tools/models.py`
# subprocess still exits 0 without it.

CANNON_JAR = Path(
    "/Users/kevin/.gradle/caches/modules-2/files-2.1/maven.modrinth/create-fly/"
    "26.2-rc-2-6.0.9-1/fe6f561de7d3b13e384dce6482760f766cb00cc0/"
    "create-fly-26.2-rc-2-6.0.9-1.jar"
)


def _load_cannon_reference():
    if not CANNON_JAR.is_file():
        return None
    try:
        with zipfile.ZipFile(CANNON_JAR) as jar:
            model = json.loads(jar.read("assets/create/models/item/potato_cannon/item.json"))
            png_bytes = jar.read("assets/create/textures/item/potato_cannon.png")
    except (KeyError, OSError, zipfile.BadZipFile):
        return None
    atlas = Image.open(io.BytesIO(png_bytes)).convert("RGBA")
    real_w, real_h = atlas.size
    elements: list[Box] = []
    uv_of: dict[tuple[int, str], tuple[int, int]] = {}
    wh_of: dict[tuple[int, str], tuple[int, int]] = {}
    for i, el in enumerate(model["elements"]):
        rot = el.get("rotation")
        rotation = None
        if rot and rot.get("angle"):
            rotation = {"angle": rot["angle"], "axis": rot["axis"], "origin": tuple(rot["origin"])}
        elements.append(Box(tuple(el["from"]), tuple(el["to"]), "steel", rotation=rotation))
        for face_name, face in el["faces"].items():
            u0, v0, u1, v1 = face["uv"]  # the cannon's own uv sometimes runs high-to-low (a mirror).
            x0, x1 = sorted((u0 / 16 * real_w, u1 / 16 * real_w))
            y0, y1 = sorted((v0 / 16 * real_h, v1 / 16 * real_h))
            uv_of[(i, face_name)] = (round(x0), round(y0))
            wh_of[(i, face_name)] = (max(1, round(x1 - x0)), max(1, round(y1 - y0)))
    return elements, atlas, uv_of, wh_of, model["display"]


def render_cannon(canvas: Image.Image, elements, atlas, uv_of, wh_of, display: dict,
                   target_x: float, target_y: float, px_per_unit: float) -> None:
    atlas_of = {i: atlas for i in range(len(elements))}
    faces = _collect_faces(elements, atlas_of, uv_of, display, wh_of)
    origin_x, origin_y = _display_origin(target_x, target_y, px_per_unit)
    _draw_faces(canvas, faces, origin_x, origin_y, px_per_unit)


def render_sheet(path: Path) -> None:
    px_per_unit = 8
    cell_w, cell_h = 280, 260
    header = 70
    label_h = 20
    columns = ("gui", "firstperson", "thirdperson", "ground")
    cannon_ref = _load_cannon_reference()
    rows: list[str] = (["potato_cannon (Create's own, calibration)"] if cannon_ref else []) + list(WEAPONS)
    sheet = Image.new("RGBA", (len(columns) * cell_w, header + len(rows) * cell_h + 340), (235, 235, 235, 255))
    draw = ImageDraw.Draw(sheet)
    draw.text((8, 6), "FA-22 round 3 -- real display transforms (rotation, scale, translation), 8x", fill=(20, 20, 20, 255))
    draw.text((8, 20), "top row: Create's own potato cannon through this renderer, its own real display block -- must match its known in-game look",
               fill=(120, 60, 20, 255))
    for col, title in enumerate(columns):
        draw.text((col * cell_w + 8, 40), title, fill=(90, 90, 90, 255))

    for row, row_id in enumerate(rows):
        cy = header + row * cell_h
        draw.text((8, cy + 2), row_id, fill=(20, 20, 20, 255))
        origin_x = cell_w // 2
        origin_y = cy + label_h + cell_h // 2
        if cannon_ref and row == 0:
            elements, atlas, uv_of, wh_of, cannon_display = cannon_ref
            for col, context in enumerate(("gui", "firstperson_righthand", "thirdperson_righthand", "ground")):
                render_cannon(sheet, elements, atlas, uv_of, wh_of, cannon_display.get(context, {}),
                               col * cell_w + origin_x, origin_y, px_per_unit)
            continue
        weapon_id = row_id
        display = class_display(WEAPONS[weapon_id][0])
        for col, context in enumerate(("gui", "firstperson_righthand", "thirdperson_righthand", "ground")):
            render_view(sheet, weapon_id, True, display.get(context, BASE_DISPLAY[context]),
                        col * cell_w + origin_x, origin_y, px_per_unit)

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

    # A mock 3x3 inventory grid at 2x, the real gui display transform -- the actual slot read.
    grid_y = atlas_y + 100
    draw.text((8, grid_y - 16), "3x3 inventory mock (2x, real gui display transform)", fill=(20, 20, 20, 255))
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
        display = class_display(WEAPONS[weapon_id][0])
        render_view(sheet, weapon_id, True, display["gui"], slot_x + slot_px / 2, slot_y + slot_px / 2, 2)

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

    calibration_note = ("" if _load_cannon_reference()
                         else f" (no potato-cannon calibration row -- {CANNON_JAR.name} not found locally)")
    print(f"wrote {len(written)} atlases; every weapon model, class display parent, part model "
          f"and items/weapon.json is under {ASSETS}; sheet at {SHEET_PATH}{calibration_note}")


if __name__ == "__main__":
    main()
