---
schema_version: 1
id: 01M3037WG3K0ZK8X0ZRKRSQBCN
key: FA-26
type: bug
title: "Weapon item model fails to bake: a part atlas uv lies outside its 32x32 image; and the AWM cannot fire"
created_by: kevin
created_at: 2026-09-20T19:03:44Z
---

## Scope

<fill this in before committing>

## Approach

<fill this in before committing>

## Acceptance criteria

- [x] <fill this in before committing>

## Constraints and prior findings

<fill this in before committing>

## Scope

Kevin, 2026-09-20, client on development 3b3d54a: "the 3D models and their items are still showing the missing texture default, and I can't shoot the AWM; shooting on the AKM works fine." Client log: `Unable to bake item model: 'firearms:weapon' — IllegalArgumentException: Cannot compute translucency out of bounds: [52, 0, 60, 4] in 32x32 image` from `FaceBakery.bakeQuad` via `CuboidItemModelWrapper` inside the `SelectItemModel`. One face's uv rectangle in a generated cuboid model (FA-22's `tools/models.py`) lies outside its atlas PNG, so the whole `firearms:weapon` model fails to bake and every weapon shows the missing model (FA-23 fixed the earlier parse failure; this is the next layer). Second: the AWM refuses to fire with the new attack payload while the AKM fires.

## Acceptance criteria

- [x] The root cause of the uv overflow is found (texture_size vs. the PNG's real size, or a packer rectangle past the atlas edge) and fixed in `tools/models.py`; assets regenerated.
- [x] `ModelAssetsTest` loads every referenced atlas PNG and asserts every face uv, scaled by `texture_size` to pixels, lies inside the image's real dimensions; and a test that the packer never emits a rectangle past the atlas.
- [x] The AWM fires with the attack payload; a game test covers firing each of the six bases through the payload path once (not only the AKM), and the cause of the AWM refusal is recorded in the ticket's Findings.
- [ ] `just check` green; merged through a Forgejo pull request into `development`; Kevin sees the models and the AWM fires.
