---
title: "create_firearms DEC-009 — Scopes reuse the vanilla spyglass mechanic via three accepted client mixins"
type: "spec"
category: "create_firearms"
---

# `DEC-009` — Scopes reuse the vanilla spyglass mechanic via three accepted client mixins

**Status:** decided by Kevin, 2026-09-20, accepting the mixin cost the research surfaced.

Kevin, verbatim: "scopes should actually be able to be used like the telescope in game is
already." The research found this less free than hoped: `Player.isScoping()` is an exact
`Items.SPYGLASS` identity check, never keyed on the use-animation, so reporting a spyglass-like
animation from a custom weapon reproduces only the arm pose, none of the zoom, overlay, or
held-item suppression (research `smithing-and-item-model-layers-26-2.md` §C.1–C.3). **Kevin
accepted the three-mixin cost this implies**: one widening `isScoping()` itself, one reading a
per-optic zoom factor in place of the spyglass's hardcoded `0.1f`, one swapping the overlay texture
per optic. All three gate on the same widened check, so this is "one mixin family," not three
independent design decisions (`04-architecture.md` `ARCH-DEC-001`; `domains/combat.md`
`COMBAT-REQ-006`–`008`).

**Red dot and holo do not zoom** — Kevin, 2026-09-20: they tighten spread and aim only, never
satisfying the widened `isScoping()` gate. 2x through 15x zoom by their own factor
(`domains/attach.md` enumerations; `domains/combat.md` `COMBAT-DEC-002`).

Alternative considered: a bespoke camera/zoom system built from scratch, avoiding
`Player.isScoping()` entirely. Rejected: Kevin's own framing asks specifically to reuse the
spyglass mechanic, and a from-scratch system would need to reimplement the FOV modifier, the
overlay draw, and the held-item suppression independently — a larger surface than three small
mixins on the one shared gate all three already go through. Cost if wrong: if a future Minecraft
version moves `isScoping()`'s call sites, all three mixins break at compile time together, the same
bounded, single-point exposure `04-architecture.md` `ARCH-FAIL-005` already names.
