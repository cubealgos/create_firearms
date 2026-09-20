---
title: "create_firearms DEC-008 — Bullets are projectile entities, against the research's hitscan recommendation"
type: "spec"
category: "create_firearms"
---

# `DEC-008` — Bullets are projectile entities, against the research's hitscan recommendation

**Status:** decided by Kevin, 2026-09-20, overruling the research's own recommendation.

The research pass recommended an instant hitscan raycast for anything firing faster than a few
shots per second, reserving a real projectile entity only for slow ordnance (research
`create-fly-potato-cannon-and-deploying-26-2.md` §A.3 verdict). **Kevin ruled against this**:
bullets are real entities with travel time and gravity drop, "a fast entity per shot." Per-tick hit
detection along the full movement vector, the same mechanism vanilla arrows and the potato cannon's
own projectile already use (`ProjectileUtil.getHitResultOnMoveVector`), means high velocity never
tunnels regardless — the correctness concern the research's recommendation was actually about does
not apply to this choice; only the performance/network-sync cost of an entity per shot at rifle
fire rates does, and Kevin accepted that cost explicitly. Shotguns spawn several bullet entities
per trigger pull (one per pellet); every bullet despawns after a short fixed life if it hits
nothing (`domains/combat.md`).

The full technical reasoning and the accepted fallback live in `04-architecture.md` `ARCH-DEC-004`;
recorded here as the top-level ruling this sheet's entire combat model is built on.

Alternative considered: the research's own hitscan recommendation. Not chosen — explicitly
overruled by Kevin for the travel-time and gravity-drop feel, not rejected for a technical flaw in
it. Cost if wrong: `ARCH-DEC-004` names the same hitscan design as the documented fallback if
automatic-weapon entity counts prove too expensive in practice — a same-shape replacement for the
flight step alone, not a redesign of hit detection, damage, or anything else in `domains/combat.md`.
