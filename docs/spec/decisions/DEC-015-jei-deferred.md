---
title: "create_firearms DEC-015 — No JEI/EMI category at 1.0"
type: "spec"
category: "create_firearms"
---

# `DEC-015` — No JEI/EMI category at 1.0

**Status:** this sheet's own scope call, mirroring `create_synthetic_diamonds` `DEC-009`; not a
Kevin ruling naming JEI specifically, but consistent with every other "no screen of this mod's own"
choice in `rulings-2026-09-20.md`.

The smithing and deploying attach recipes, and every crafting recipe for bases, attachments and
cartridges, work regardless of whether a recipe viewer displays them. Create Fly's own JEI category
for pressing was already found unable to render a custom recipe class without crashing
(`create_synthetic_diamonds` research); this mod's own custom smithing/deploying recipe classes
would need the identical bespoke category work before they could safely appear in JEI, and nothing
in the rulings asks for that at 1.0. `domains/attach.md` and `domains/ammo.md`'s own tables are the
documented reference in the meantime (`domains/ui.md` `UI-DEC-001`).

Alternative considered: shipping a JEI category at 1.0 alongside the two recipe classes. Not
chosen: keeps 1.0's surface to the mechanism itself, the same call `create_synthetic_diamonds`
made for its own pressing recipe. Cost if wrong: none — additive future work, touching neither the
attach nor the combat domains when eventually built.
