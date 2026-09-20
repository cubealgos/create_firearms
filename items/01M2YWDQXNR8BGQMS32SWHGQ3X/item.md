---
schema_version: 1
id: 01M2YWDQXNR8BGQMS32SWHGQ3X
key: FA-5
type: feat
title: "The bullet entity: velocity, gravity, per-tick hit ray, damage type, knockback, life, water, pellets"
created_by: kevin
created_at: 2026-09-20T07:45:22Z
---

## Scope

The bullet entity class: spawn at the weapon's muzzle offset with the derived muzzle velocity along
the aim vector, server-rolled spread applied to the initial vector (`COMBAT-REQ-001`); gravity-affected
flight with no drag beyond gravity (`domains/combat.md` §3); per-tick hit testing via
`ProjectileUtil.getHitResultOnMoveVector` over that tick's full movement vector, never a fixed-length
or point probe (`COMBAT-REQ-002`); despawn with no damage/drop after a fixed life
(`COMBAT-REQ-003`, `UC-013`); on an entity hit, a `DamageSource` of type `firearms:bullet` with the
bullet as direct entity and the shooting player as causing entity (`COMBAT-REQ-004`, `010`); a
small base knockback; block hits remove the bullet with no penetration or ricochet
(`COMBAT-FAIL-002`); water behaviour (proposed: unaffected flight, retune at the sweep); the
shotgun's 8-pellet spawn per trigger pull, independently spread and hit-tested
(`COMBAT-REQ-005`). Not firing itself — the cooldown/ammo/fire-mode loop that spawns a bullet
(`FA-6`) — and not the damage-type tag/registration files beyond what this entity needs to exist.

## Approach

One entity class, registered the ordinary Fabric way, client- and server-side; damage type
`firearms:bullet` as a data-driven `DamageType` JSON, not in `bypasses_armor` or `bypasses_shield`
(`domains/combat.md` §3 table). Hit testing and the tunneling-safety argument follow the identical
mechanism vanilla arrows and the potato cannon's own projectile already use — no new algorithm to
invent, just the entity class wiring it up (research `create-fly-potato-cannon-and-deploying-26-2.md`
§A.3). Shotgun pellets are eight independent bullet entities spawned from one trigger pull; the
per-pellet spread cone is wider than a rifle's own (`domains/weapon.md` roster table: 8.0° vs.
2–4.5°).

## Acceptance criteria

- [ ] A game test proves hit detection holds at both low and very high muzzle velocity, with no
      tunneling regardless of speed (`COMBAT-REQ-002`).
- [ ] A game test fires at a target beyond 40 blocks and confirms a hit still registers
      (`TEST-REQ-004`).
- [ ] A bullet that hits nothing despawns cleanly after its fixed life, no damage, no drop
      (`UC-013`).
- [ ] A shotgun-shaped trigger pull spawns 8 independently-spread pellet entities, each
      independently hit-tested (`COMBAT-REQ-005`).
- [ ] Every constructed `DamageSource` follows the direct/causing-entity convention so `pvp` and
      team-allegiance checks apply with no extra code (`COMBAT-REQ-010`).
- [ ] `just check` green, including the new game tests.

## Constraints and prior findings

`docs/spec/domains/combat.md` `COMBAT-REQ-001`–`005`, `010`, `COMBAT-FAIL-001`–`002`,
`docs/spec/04-architecture.md` `ARCH-DEC-004` (bullets are real entities, against the research's own
hitscan recommendation — Kevin's explicit ruling, with the hitscan design as the documented
fallback if automatic-fire entity counts prove too expensive, `ARCH-FAIL-004`). Exact bullet gravity
multiplier and despawn-life tick counts (proposed 40 ticks) are open questions confirmed at this
ticket (`domains/combat.md` §7). Blocked by `FA-3`'s item/component surface (a bullet's damage and
velocity read the derived stats `FA-2` computes from the components `FA-3` registers).
