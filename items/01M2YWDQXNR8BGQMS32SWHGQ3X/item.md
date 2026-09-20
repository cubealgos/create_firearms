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

## Findings

- **`FA-3` blocker sidestepped by construction, not by waiting**: `BulletEntity`/`BulletSpawner`
  take damage, muzzle velocity, spread and pellet count as plain parameters (`BulletSpawner
  .spawnBullet`/`spawnPellets`), never reading `firearms:base`/`firearms:attachment_*` components
  or calling the stat derivation function themselves. `FA-6`'s firing loop is the one that will read
  `FA-3`'s components and `FA-2`'s derived stats, then hand the numbers to `BulletSpawner`. This
  ticket built and tested entirely against `firearms.combat`/`firearms.client.combat`, touching
  neither `firearms.model` (parallel `FA-2`) nor any `FA-3` file — so the formal `blocked_by`
  relation never actually gated this ticket's own work, and can be closed out as soon as `FA-3`
  lands without any rework here.
- **Gravity and life decided, not just proposed**: `GRAVITY_PER_TICK = 0.02` blocks/tick² (below
  vanilla arrow's `0.05`, per the sheet's own "cartridge rounds drop less" framing) and
  `LIFE_TICKS = 40` (the sheet's own proposed number). Both are still flagged `proposed, retune at
  the balance sweep` in `BulletEntity`'s Javadoc, matching every other 1.0 stat's own tone — this
  ticket answered the "first ticket" half of `domains/combat.md` §7's "first ticket, balance sweep"
  resolution path, but did not edit `docs/spec/domains/combat.md` §7's open-questions table itself
  (out of this ticket's assigned scope: `docs/spec/` is a copy of heimathafen's vault, and no
  instruction here extended to updating vault source). Recommend a follow-up ticket or spec-sync
  pass formally closes that open question in the vault, quoting `GRAVITY_PER_TICK`/`LIFE_TICKS` as
  the answer.
- **Water is unaffected, not "slows or stops"**: implemented literally per `domains/combat.md` §3's
  own proposal ("unaffected flight, retune at the sweep") — no water special-casing in
  `BulletEntity.tick()` at all. A separate paraphrase of this ticket's brief said "water slows or
  stops per spec"; the spec itself says the opposite is the current proposal, so the spec's literal
  text won out.
- **`death.attack.firearms.bullet.player` may be unreachable at 1.0**: read against the 26.2 jar's
  own `DamageSource.getLocalizedDeathMessage()`, the `.player`-suffixed message key convention
  (confirmed live for `cactus`/`fall`/`drown`/etc.) is produced by `CombatTracker`'s own "died from
  an indirect cause while fighting someone" path, not by `DamageSource`'s own resolution — which,
  whenever both a direct and a causing entity are present (true for every bullet hit), only ever
  emits the base key or a `.item`-suffixed one (never `.player`). Both keys are shipped and checked
  by `SourceSurfaceTest` per this ticket's literal instruction; whether `.player` is ever actually
  selected for a bullet kill is worth confirming on the client checklist rather than assumed.
- **`@GameTest`'s `padding` attribute does not move the invisible test-boundary barrier** in 26.2:
  `TestInstanceBlockEntity.processStructureBoundary()` (which both places and removes the barrier
  walls) builds from `getStructureBounds()` — the raw, unpadded structure box — never
  `getTestBounds()`/`getBoundsWithPadding()`. `padding` only widens what `GameTestHelper
  .getBoundsWithPadding()` reports to a test's own assertions; it does nothing to let a fast-moving
  entity actually travel further before clipping the barrier. Four of this ticket's six game tests
  needed real distance (20–48 blocks) and all silently failed against the default
  `fabric-gametest-api-v1:empty` (8×8×8) structure regardless of `padding` value, until switched to
  a bundled larger structure (`firearms_gametest:open_range`,
  `src/gametest/resources/data/firearms_gametest/gametest/structure/open_range.snbt`, 4×30×60, all
  air). Worth a heimathafen vault note for any future ticket writing a long-range game test in this
  Minecraft version.
- **Knockback strength (`0.35f`) is this ticket's own pick**, not derived from any vanilla constant
  — `domains/combat.md` only says "small base value, comparable to an arrow's"; a plain (unenchanted)
  vanilla arrow's own `AbstractArrow.doKnockback` applies zero knockback (the value comes entirely
  from the Punch enchantment), so "comparable to an arrow's" has no zero-enchant baseline to copy
  numerically. Flagged in `BulletEntity`'s own Javadoc as a proposed constant.
