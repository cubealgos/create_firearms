---
title: "create_firearms DEC-014 — Combat scope is mobs and players under vanilla damage rules only"
type: "spec"
category: "create_firearms"
---

# `DEC-014` — Combat scope is mobs and players under vanilla damage rules only

**Status:** decided by Kevin, 2026-09-20.

Combat is scoped to mobs and players, on the same footing as arrows: no bleeding, no limb hits, no
penetration, at 1.0. Every bullet hit resolves through ordinary vanilla `DamageSource`/
`LivingEntity.hurt`, exactly as an arrow's does, with the causing entity set to the shooting player
so the `pvp` gamerule and team-allegiance checks apply automatically (research
`create-fly-potato-cannon-and-deploying-26-2.md` §A.6, §C.4; `domains/combat.md` `COMBAT-REQ-004`,
`010`). This is the scope boundary that keeps this a weapons mod rather than a wound-simulation mod
— realism (Kevin's other stated goal) is bounded to what vanilla damage rules and the component
model can express, not extended into a new combat subsystem.

Alternative considered: a richer combat model with hit locations, armor penetration by calibre, or
bleed-over-time. Not chosen for 1.0 — explicitly named as kept out by Kevin's own rulings, not
merely deferred by omission. Cost if wrong: none identified at 1.0; any of these would be a
significant new subsystem (a hit-location model, a damage-over-time effect, an armor-bypass rule)
layered on top of the vanilla `DamageSource` this mod already builds correctly, not a rework of it.
