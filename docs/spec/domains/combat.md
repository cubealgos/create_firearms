---
title: "create_firearms spec — COMBAT: the bullet, damage, spread/recoil, and scoping"
type: "spec"
category: "create_firearms"
---

# `COMBAT` — what happens once a bullet leaves the barrel

## 1. Purpose

The bullet entity and shotgun pellets; damage source, type and PvP gating; tracers and impact
cosmetics; the suppressor's sound effect; how spread and recoil are decided and where each is
applied (server or client); the scope mechanic and its three client mixins. Not firing itself
(cooldown, ammo, fire mode — `domains/weapon.md`) and not what an attachment's modifier values are
(`domains/attach.md`).

## 2. Dimensions

| Dimension | Answer |
|---|---|
| **Actors** | The server (`ACTORS-002`) owns every hit result, damage amount, and the spread roll; the shooting player's client renders the cosmetic recoil kick, tracer, and (while scoped) the zoom and overlay; the target (`ACTORS-006`) takes damage under vanilla rules with no code of its own. |
| **Over time** | Shot fired → bullet entity spawned along a server-rolled vector → travels under gravity, hit-tested every tick along its full movement → hits an entity (damage applied, entity removed) or a block (removed, no damage) or lives out its fixed life and despawns. |
| **Multiplicity** | One bullet per shot for every 1.0 weapon except the shotgun, which spawns 8 pellets per trigger pull from one ammo unit (`WEAPON-REQ-008`'s "one round" is still consumed once, not per pellet). |
| **Unwanted** | A bullet that would tunnel through a target at high velocity (prevented structurally by the swept-segment hit test, not by a speed cap); a scoped weapon's zoom leaking onto an unrelated vanilla interaction; two mods both widening `isScoping()` (`COMBAT-FAIL-003`). |
| **Not-you** | A player who never fires a weapon never sees a tracer, a scope overlay, or a bullet entity in their world. A player hit by another player's bullet experiences it exactly as being hit by an arrow: the same knockback, invulnerability-tick, and `pvp` gamerule behaviour. |

## 3. Enumerations

### Bullet entity lifecycle

| Stage | Behaviour |
|---|---|
| Spawn | Position at the weapon's muzzle offset; velocity = derived muzzle velocity along the aim vector, offset by a spread roll drawn server-side within the derived cone (`WEAPON-REQ-003`, `005`). |
| Flight | Gravity-affected (a multiplier below vanilla arrow's own, since real cartridge rounds drop less over typical engagement ranges — proposed, retune at the sweep); no drag beyond gravity at 1.0. |
| Hit test | Every tick, `ProjectileUtil.getHitResultOnMoveVector` over that tick's full movement vector — the identical mechanism vanilla arrows and the potato cannon's own projectile use, correct at any velocity by construction (research `create-fly-potato-cannon-and-deploying-26-2.md` §A.3). |
| Entity hit | Damage applied (`COMBAT-REQ-004`); the bullet is removed. |
| Block hit | The bullet is removed; no penetration, no ricochet at 1.0. |
| Despawn | Removed with no effect after a fixed life, proposed 40 ticks (2 real seconds) — enough range for every 1.0 weapon's muzzle velocity to cover a plausible engagement distance. |

### Shotgun pellets

The Winchester Model 1897's `pump` fire mode spawns 8 pellet bullet entities per trigger pull, each
independently rolled within a wider per-pellet spread cone (`domains/weapon.md` roster table: 8.0°
vs. a rifle's 2–4.5°) and independently hit-tested; one ammo round is consumed for the whole pull,
mirroring the potato cannon's own "consume once per split loop, not once per pellet" convention
(research §A.2 step 5).

### Damage source and type

| Field | Value |
|---|---|
| Damage type id | `firearms:bullet` |
| Tag membership | Not in `bypasses_armor` or `bypasses_shield`, matching vanilla's own projectile types (`arrow`, `trident`, `mob_projectile`) — armor and shields work against a bullet exactly as they do against an arrow, unless a later ticket adds a tag-file change (research `create-fly-potato-cannon-and-deploying-26-2.md` §C.3). |
| `DamageSource` construction | Direct entity = the bullet (or the pellet); causing entity = the shooting player — the same convention the potato cannon uses, which gets `pvp` gamerule compliance for free from `ServerPlayer.hurtServer`/`canHarmPlayer` with zero extra code (research §A.6, §C.4). |
| Knockback | A small base value, comparable to an arrow's, not amplified per calibre at 1.0. |

## 4. Use cases

`UC-007`–`UC-013` in `02-journeys.md`.

## 5. Requirements

| ID | Requirement | Priority | From |
|---|---|---|---|
| `COMBAT-REQ-001` | On a successful fire, the system shall roll the shot's spread server-side, within the derived cone, and apply it to the spawned bullet's (or pellets') initial vector; the client shall never supply or influence this roll. | Must | `04-architecture.md` `ARCH-DEC-005` (server-authoritative) |
| `COMBAT-REQ-002` | The bullet entity shall be hit-tested every tick along that tick's full movement vector via `ProjectileUtil.getHitResultOnMoveVector`, never a fixed-length or single-point probe, so no velocity this mod ships causes a tunneling miss. | Must | Kevin, 2026-09-20; research §A.3 |
| `COMBAT-REQ-003` | A bullet entity that has not hit anything by its fixed life shall despawn with no damage and no drop. | Must | `UC-013` |
| `COMBAT-REQ-004` | On an entity hit, the system shall apply the weapon's derived damage (or, for a pellet, the shotgun's per-pellet damage) via a `DamageSource` of type `firearms:bullet`, built with the bullet as the direct entity and the shooting player as the causing entity. | Must | Research §A.6, §C.4 |
| `COMBAT-REQ-005` | The shotgun's `pump` fire mode shall spawn 8 independently-spread pellet entities per trigger pull while consuming exactly one round of ammunition. | Must | `UC-009` |
| `COMBAT-REQ-006` | The system shall widen `Player.isScoping()`, via one client mixin, to also return true while a player is using a firearm whose attached optic's zoom factor is greater than 1.0 — covering the FOV modifier, the overlay draw, and held-item render suppression from the single widened gate. | Must | Kevin, 2026-09-20: "scopes should actually be able to be used like the telescope"; research `smithing-and-item-model-layers-26-2.md` §C.1–C.3 |
| `COMBAT-REQ-007` | The system shall provide a second client mixin reading the attached optic's own zoom factor for the FOV calculation, in place of the vanilla spyglass's hardcoded `0.1f` literal, whenever the widened `isScoping()` gate is satisfied by a firearm. | Must | Research §C.2, §C.3(b) |
| `COMBAT-REQ-008` | The system shall provide a third client mixin swapping the scope overlay texture to the attached optic's own texture, in place of the vanilla spyglass's fixed `SPYGLASS_SCOPE_LOCATION`, whenever the widened gate is satisfied by a firearm. | Must | Research §C.2, §C.3(c) |
| `COMBAT-REQ-009` | A suppressor's only effect at 1.0 shall be a reduced effective volume/range on the weapon's fire sound; no aggro-radius or mob-perception effect is implemented, since no vanilla hook was confirmed for one. | Must, scoped | `rulings-2026-09-20.md` "suppressor noise-radius effects (no vanilla hook confirmed yet)" |
| `COMBAT-REQ-010` | Every `DamageSource` this mod constructs shall follow the direct/causing-entity convention in `COMBAT-REQ-004`, so the vanilla `pvp` gamerule and team-allegiance check apply automatically with no code of this mod's own duplicating them. | Must | Research §C.4 |
| `COMBAT-REQ-011` | The recoil camera kick, the tracer, the muzzle flash, and the impact particle shall be rendered client-side only and shall carry no authority over any hit, damage, or ammo state. | Must | `COMBAT-DEC-003` |

## 6. Failure modes

| ID | Failure | Response |
|---|---|---|
| `COMBAT-FAIL-001` | A bullet hits nothing before its life ends | `COMBAT-REQ-003`: despawns cleanly, `UC-013`. |
| `COMBAT-FAIL-002` | A bullet hits a block | Removed immediately, no penetration, no ricochet; matches the non-piercing single-raycast-per-tick model research §C.2 confirms is already tunneling-safe without a pierce loop. |
| `COMBAT-FAIL-003` | Another mod also mixins `Player.isScoping()` | Coexists only if both widen the same boolean expression additively; a genuine collision is a mixin-ordering problem shared by that pair of mods, the same shape `create_villager_customers` `ARCH-FAIL-004` already accepts for its own brain mixin. |
| `COMBAT-FAIL-004` | Target is invulnerable (creative, spectator, or another vanilla immunity) | Vanilla's own `hurtServer` invulnerability check runs before this mod's damage is ever applied; no special case needed. |

## 7. Open questions

| Question | Blocks | Decided by |
|---|---|---|
| The exact bullet gravity multiplier and despawn-life tick counts per weapon (uniform at 1.0, proposed 40 ticks) | `COMBAT-REQ-002`, `003` | first ticket, balance sweep |
| Whether the suppressor's volume/range reduction is a second `SoundEvent` variant per weapon or a runtime attenuation parameter on the same event | `COMBAT-REQ-009` | first ticket |

## 8. Decisions

- `COMBAT-DEC-001` — **Vanilla damage rules only: no bleeding, no limb hits, no penetration**
  (Kevin, 2026-09-20, confirming the proposal's own scoping). A bullet hits or misses exactly as an
  arrow does; nothing about where on a target's model it lands changes anything. **Cost if wrong:**
  none identified — this is the scope boundary that keeps this a weapons mod rather than a
  wound-simulation mod, and every stat and requirement in this sheet assumes it.
- `COMBAT-DEC-002` — **Bullets are projectile entities, against the research's own hitscan
  recommendation** (Kevin, 2026-09-20). Restated here as the mechanism this domain implements; the
  full reasoning and the accepted cost lives in `04-architecture.md` `ARCH-DEC-004`.
- `COMBAT-DEC-003` — **Spread is server-authoritative on the bullet's real trajectory; recoil's
  camera kick is a client-only cosmetic animation** (this sheet's resolution of the ticket's open
  "server-authoritative aim punch sent to the client or applied client-side: decide"). The hit
  outcome must never depend on anything the client reports, so the actual spread roll and the
  bullet's actual vector are entirely server-side (`COMBAT-REQ-001`); the visual camera nudge a
  player feels when firing has no bearing on that roll and needs no round trip, mirroring the
  potato cannon's own cosmetic-packet/server-authoritative-shot split (research
  `create-fly-potato-cannon-and-deploying-26-2.md` §A.2 step 9, §A.5). **Cost if wrong:** if a
  future ticket wants the client's camera kick to visually match the server's actual roll exactly
  (rather than a plausible independent animation), a small S2C packet carrying the roll seed is an
  additive change, not a redesign.
- `COMBAT-DEC-004` — **Red dot and holo tighten spread and aim but never zoom** (Kevin, 2026-09-20).
  `COMBAT-REQ-006`'s widened `isScoping()` gate checks the attached optic's zoom factor strictly
  greater than 1.0, so these two optics never satisfy it — no FOV change, no overlay, no held-item
  suppression — while still contributing their own spread-while-aiming modifier through the
  ordinary stat function (`domains/attach.md` enumerations).
