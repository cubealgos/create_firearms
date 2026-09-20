---
schema_version: 1
id: 01M2Z369VZ1VJ9MWZNATTKV3NN
key: FA-21
type: feat
title: Art pass on the weapon and attachment sprites
created_by: kevin
created_at: 2026-09-20T09:43:38Z
---

## Scope

An art pass on the item sprites FA-9 generated (Kevin, 2026-09-20: the weapons read as flat sticks, the attachments as grey blocks). Recognisable pixel-art silhouettes per base weapon (stock, grip, magazine, barrel, sights, distinct per class: the M1911's slide, the Micro Uzi's stubby magazine and wire stock, the AKM's curved magazine and wooden furniture, the Mini-14's long barrel and wooden stock, the AWM's bolt and scope rail, the Winchester's pump and tube), shaded with three to four tones per material in Create's flat style; attachment glyphs that read as the part (a suppressor tube, a scope with lens, a red dot's small window, an extended magazine, a vertical grip, a stock pad), positioned at each slot's anchor; cartridges kept. Our own art only (`COMP-REQ-002`). Iterated with Kevin from rendered sheets; the models and anchors of FA-9 stay.

## Approach

Extend `tools/sprites.py` (deterministic, committed PNGs) rather than hand-painting; render a sheet at 8x per round to the scratchpad for Kevin's approval; up to three rounds.

## Acceptance criteria

- [ ] A sheet round approved by Kevin (round five merged for the in-game look on 2026-09-20; not yet approved).
- [x] `ModelAssetsTest` and `just check` green; every anchor still lines up (the layered composite renders each attachment on its weapon: prove with a composited preview per weapon in the sheet).
- [x] Merged through a Forgejo pull request into `development`.

## Constraints and prior findings

FA-9's Findings: the layer anchors per slot, the 32x16 base size, the `minecraft:using_item` condition.
