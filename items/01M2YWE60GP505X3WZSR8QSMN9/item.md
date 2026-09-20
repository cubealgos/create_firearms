---
schema_version: 1
id: 01M2YWE60GP505X3WZSR8QSMN9
key: FA-9
type: feat
title: "Item models: composite layers, condition on slot components, display transforms"
created_by: kevin
created_at: 2026-09-20T07:45:36Z
---

## Scope

One composite item model per base weapon: a base layer plus one `minecraft:condition` layer per
slot the class has, each gated on `minecraft:has_component` against that slot's own
`firearms:attachment_<slot>` type — zero mixin (`04-architecture.md` `ARCH-DEC-006`,
`WEAPON-REQ-012`). A display transform on the base layer distinguishing aiming vs. hip-fire, keyed
on whether the player is currently using the item (`ItemDisplayContext`). One model file per base
weapon, never one per base×attachment combination. Not the scope mixins (`FA-10`) and not the
tooltip (`FA-11`).

## Approach

Confirm the exact `minecraft:select`/`minecraft:condition` JSON shape against the 26.2 item-model
schema at this ticket (research `smithing-and-item-model-layers-26-2.md` §B.2, §B.3, cited by
`ARCH-DEC-006`); this only holds because each slot is its own directly-valued component
(`ARCH-DEC-005`) — a combined map or any derived branch key would need the accessor-mixin fallback
`create_metered_motor`'s `MM-15` already used once, which this ticket does not need. Textures and
layer art are drawn fresh for each weapon's own real-world designation; no Minecraft, Create Fly or
third-party asset is copied (`operations/compliance.md`).

## Acceptance criteria

- [ ] Attaching visibly changes the composite item model's layers in a real client render, and a
      filled slot's layer never reverts (client checklist item).
- [ ] A shotgun (no optic/grip/stock slots) never renders a layer for a slot its class lacks.
- [ ] The aiming vs. hip-fire display transform is visibly distinct on at least one weapon.
- [ ] No texture, model file, or string anywhere in this ticket's assets references PUBG or its
      branding (`COMP-REQ-002`, enforced by `SourceSurfaceTest`'s existing scan).
- [ ] `just check` green.

## Constraints and prior findings

`docs/spec/04-architecture.md` `ARCH-DEC-005`, `ARCH-DEC-006`, `docs/spec/domains/weapon.md`
`WEAPON-REQ-012`, `013` (item model), `docs/spec/operations/compliance.md` `COMP-REQ-002`. Blocked
by `FA-7` (needs a real filled slot to test the condition layer against, from the smithing attach
recipe).
