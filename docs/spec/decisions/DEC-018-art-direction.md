---
title: "create_firearms DEC-018 — Art direction: Create/vanilla pixel style, weapons as 3D models"
type: "spec"
category: "create_firearms"
---

# `DEC-018` — Art direction: Create/vanilla pixel style, weapons as 3D models

**Status:** decided by Kevin, 2026-09-20, on the FA-21 round-one sheet: "they should look more like
Create / vanilla Minecraft items, they currently don't have that vibe at all + they should be 3D
models I suppose."

## What "the vibe" means, so a round can be judged against it

Vanilla and Create item art share five habits, and the round-one sheet broke every one of them:

1. **16×16 canvas.** An item sprite is 16×16; nothing is drawn at 32×16. A long weapon is drawn
   diagonally across the square, as vanilla draws a bow, a trident or a fishing rod, and as Create
   draws the potato cannon's inventory icon.
2. **A one-pixel outline** in the darkest tone of the material, never pure black, never absent.
3. **Three to four tones per material**, lit from the top-left: highlight, base, shade, outline.
   No gradients, no anti-aliasing, no isolated single pixels.
4. **The game's own palette.** Steel is Create's industrial-iron greys, brass is Create's brass,
   wood is vanilla oak or dark oak, polymer is a desaturated olive; the reference swatches are in
   `tools/palette.py`, sampled from Create Fly's and vanilla's textures (CC0 and Mojang's; sampled
   colours, never copied pixels, `COMP-REQ-002`).
5. **Readable at 16 pixels**: one silhouette feature per weapon family that a player recognises in a
   hotbar (a curved magazine, a pump forend, a long scope tube).

## Weapons are 3D models, like Create's own tools

Create renders its held tools (wrench, potato cannon, extendo grip) as Blockbench-style cuboid
element models in every display context, with `display` transforms per context; the inventory icon
is the 3D model at a fixed rotation, not a sprite. Firearms follow that convention:

- **Each base weapon is a cuboid element model** (`models/item/weapon/<id>.json`: `elements`
  with `from`/`to`/`faces`, a per-weapon texture atlas at `textures/item/weapon/<id>.png`,
  `texture_size` declared), generated deterministically by `tools/models.py` from a box list per
  weapon, the way `tools/sprites.py` generated the sprites. Hand-made Blockbench files are
  acceptable later; the generator is the 1.0 path.
- **Each mounted attachment is a cuboid part model** (`models/item/weapon/part/<slot>_<name>.json`)
  positioned at the slot's anchor in the same 16-unit space, so the composite of base plus
  parts (`ARCH-DEC-006`, unchanged) shows the loadout on the weapon in hand, in third person, on
  the ground and in the inventory alike. Anchors move from sprite pixels to model units; a part is
  positioned per weapon class where a shared anchor cannot fit (a pistol's suppressor sits closer
  than a rifle's).
- **Display transforms** follow Create's potato cannon as the starting point (third person
  rotated 15° off the arm and pushed back, first person raised and tilted, ground rotated 90°,
  gui at roughly [64, 47, -47] scaled 0.86) and are tuned per class in the same rounds as the art.
  Aiming (the `minecraft:using_item` condition of FA-9) keeps its separate first-person
  translation.
- **Standalone attachment items and cartridges stay flat 16×16 sprites** in the style above, as
  vanilla keeps small parts flat (an iron nugget, a spyglass, an arrow). Their icon is the same art
  as the part's texture where that reads.

## Cost if wrong

Two generators and their asset trees under `tools/` and `assets/firearms/`; the composite item
definition (`items/weapon.json`) changes shape once, from sprite layers to model parts, and
`ModelAssetsTest` checks the new tree. No Java changes: the item, components and conditions are
untouched. If 3D weapons turn out to read worse than sprites in the hotbar, a `minecraft:select` on
`minecraft:display_context` can swap the gui context back to a sprite without touching the
in-hand models.
