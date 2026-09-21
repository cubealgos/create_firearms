---
schema_version: 1
id: 01M30STYK35MNWQCGDZZ4V588C
key: FA-27
type: docs
title: "Modrinth icon: the AKM from the weapon model on the navy grid badge, lightened"
created_by: kevin
created_at: 2026-09-21T01:38:38Z
---

## Scope

Regenerate `docs/modrinth/icon.png` per Kevin's 2026-09-21 icon ruling: a real 3D render of the
AKM weapon item model on the cubealgos navy grid badge, lightened, replacing the current
placeholder (a hand-drawn 16x16 pixel-art cartridge, FA-15/FA-16) now that FA-9's real weapon item
models exist. This mod ships exactly one Modrinth icon.

Explicitly out of scope: the other five weapons' icons (M1911, Micro Uzi, Ruger Mini-14, AWM,
Winchester Model 1897) — the AKM is the sole chosen subject, not a rotation; any in-game
balance, model, or texture change (the lightening happens only in the render, never in the
shipped game asset); the Modrinth gallery screenshots (`docs/modrinth/gallery.md`, unrelated and
untouched).

## Approach

Port `create_metered_motor`'s inline 3D projector (`rot_axis`/`rotate_about`/
`display_transform`/`affine_from_points`/`invert_affine`/`build_faces`/`fit_scale`/
`render_model`) into this repo's `tools/icon.py`, reading `akm.json` and `akm.png` straight from
this mod's own `src/main/resources/assets/firearms/...` — no jar, nothing external to vendor or
credit. The AKM's `elements`/`faces[*].uv` are already raw 32x32 texture-pixel coordinates, so no
`texture_size/16` UV scale is applied (that factor is for a different, superseded model
convention). The tilt is `[30, -135, 0]`, the model's own default `gui` display transform
(confirmed by reading `firearms:item/weapon/class/assault_rifle`, which holds only display
transforms), at a 224px fit box (70% of the shared 320px box).

This repo's own existing `badge()` (the navy grid disc) and outline/shadow compositor
(renamed/adapted as `compose()`) are reused close to as-is, switched from nearest-neighbour
pixel-art scaling to LANCZOS "smooth" scaling, since a 3D projection is already anti-aliased art.

On top of the straight port, three lightening adjustments address "the render reads a bit dark"
on a navy badge: (1) the per-face `SHADE` table raised from vanilla's own item-render shading
(up 1.0, down 0.5, north/south 0.8, east/west 0.6) to (up 1.0, down 0.7, north/south 0.9,
east/west 0.8) — +0.1 on every face but `up`, same relative order; (2) an overall RGB-only
brightness lift `(1 + pct)` clamped to 255, applied to the assembled sprite before badge
compositing; (3) a soft warm top-left key light, masked to the sprite's own alpha so it never
touches the badge. `tools/icon.py --sheet` renders all three candidate lift levels (+15/+25/+35)
at 256px and 64px into `scratchpad/fa-icon-brightness.png` for the pick — see Constraints below.

## Acceptance criteria

- [x] `tools/icon.py` renders the AKM from `akm.json` + `akm.png` alone (no other file needed) at
      512x512.
- [x] The render reads visibly lighter than plain vanilla shading while keeping the wood/steel
      colour contrast and the badge's white outline.
- [x] A brightness-level comparison sheet exists at `scratchpad/fa-icon-brightness.png`.
- [x] `docs/modrinth/body.md`'s Icon row describes the new AKM render, not the placeholder
      cartridge.
- [x] `just map` and the available `tools/` tests still pass.

## Constraints and prior findings

- Tilt: `[30, -135, 0]` (the model's own inherited `gui` transform). Fit box: `224` (70% of the
  shared 320px box).
- Final `SHADE` table: `{"up": 1.0, "down": 0.7, "north": 0.9, "south": 0.9, "east": 0.8,
  "west": 0.8}`.
- Brightness level shipped: **+25%**. Evidence from `python3 tools/icon.py --sheet`: the
  blowout-fraction check (fraction of the sprite's own non-transparent pixels with any RGB
  channel >= 250) reads 0.00% at +15%, +25% *and* +35% — the AKM's texture is naturally dark
  (browns/greys/blacks, little near-white surface), so there is no measurable blowout at any of
  the three candidate levels, and no jump to weigh between them. With the objective check flat,
  the default rule applies cleanly: ship +25%. Visual read at both 256px and the 64px size
  Modrinth actually ships confirms the wood-coloured stock stays clearly distinct from the
  steel receiver/barrel at +25%; +15% read slightly duller and +35% no brighter in any way that
  mattered (headroom is real but unused, since the texture never approaches white). +15% and
  +35% were both rejected only for being off the picked default, not for any defect.
- Warm key light: colour `(255, 225, 185)` (warm off-white/light amber), peak alpha `40`,
  centred slightly inside the sprite's own bounding-box top-left corner
  (`x0 + bw*0.12, y0 + bh*0.12`), radius `span * 0.55`, Gaussian-blurred by `span * 0.35` (`span`
  = the sprite's own bounding-box longest side), then masked to the sprite's own alpha channel via
  `ImageChops.multiply` so it never spills onto the badge.
- `docs/modrinth/cartridge-badge-sprite.png` (the old placeholder cartridge's standalone export)
  is deleted: FA-27 retires `cartridge_sprite()`/`SPRITE_OUT` from `tools/icon.py` and nothing
  else in the repo referenced that file.
