---
schema_version: 1
id: 01M2YWDVDY5HE57VS80EH3EY8F
key: FA-6
type: feat
title: "Firing: use, cooldown, fire modes, ammo, reload, durability, sounds, server-authoritative spread, recoil packet"
created_by: kevin
created_at: 2026-09-20T07:45:25Z
---

## Scope

The fire control loop: refuse to fire with no error beyond an empty-click cue when
`firearms:ammo` is absent or zero (`WEAPON-REQ-007`); on a successful shot, derive final stats
(`WEAPON-REQ-003`), roll spread server-side within the derived cone (`COMBAT-REQ-001`), spawn the
bullet(s) `FA-5` provides, decrement `firearms:ammo.loaded`, reduce `DataComponents.DAMAGE` by one,
and start the fire-rate cooldown on the weapon's own cooldown group via vanilla `ItemCooldowns`
(`WEAPON-REQ-008`, `009`); fire mode behaviour — `semi` once per press, `auto` repeating while held
and ammo remains, `pump` as `semi` for cooldown but multi-pellet for assembly
(`WEAPON-REQ-004`); reload scanning inventory for a matching-calibre cartridge, consuming up to the
derived magazine size, silently no-op when none match (`WEAPON-REQ-010`, `011`, `AMMO-REQ-004`);
distinct fire and empty-click sounds per weapon, with a suppressor reducing effective fire-sound
volume/range (`WEAPON-REQ-013`, `COMBAT-REQ-009`); the cosmetic client-side recoil packet
(`COMBAT-DEC-003`) — the visual camera kick, decoupled from the server-authoritative spread roll.
Not the bullet entity itself (`FA-5`, already landed) and not the tooltip or the visible recoil
animation rendering (`FA-11`).

## Approach

Pace fire rate through vanilla `ItemCooldowns`, keyed to the weapon's own cooldown group, the same
mechanism the potato cannon uses (`WEAPON-REQ-009`, research `create-fly-potato-cannon-and-deploying-26-2.md`
§A.2, §C.1); confirm at this ticket whether `useCooldown` from `use()`/`finishUsingItem()` covers
`auto` mode's repeated firing or a hand-rolled per-shot `ItemCooldowns` call is needed
(`domains/weapon.md` §7 open question). The AWM's `semi (bolt-cycle)` fire mode reuses `semi` with
only a longer fire-rate stat, per this sheet's own proposed reading (`domains/weapon.md` §7),
confirmed rather than re-litigated at this ticket unless testing shows the reuse is observably
wrong. Spread is rolled entirely server-side; the client-side recoil kick is a plausible independent
cosmetic animation with no round trip, mirroring the potato cannon's own cosmetic-packet split
(`COMBAT-DEC-003`).

## Acceptance criteria

- [x] A weapon with known components fires, decrements ammo and durability by one, starts the
      correct cooldown, and refuses to fire (empty-click only) at zero ammo (`WEAPON-REQ-007`,
      `008`).
- [x] `auto` mode repeats at the fire-rate interval while held and stops the instant ammo reaches
      zero, control releases, or the weapon leaves the player's hand (`UC-008`).
- [x] Reload consumes cartridges up to the derived magazine size from a matching-calibre stack and
      no-ops silently with none present (`WEAPON-REQ-010`, `011`).
- [x] A suppressed weapon's fire sound is measurably quieter/shorter-range than an unsuppressed
      one's.
- [x] The AWM's fire-mode reuse decision (`semi` with a longer fire-rate vs. a distinct third mode)
      is confirmed and recorded in `domains/weapon.md` §8.
- [x] `just check` green.

## Constraints and prior findings

`docs/spec/domains/weapon.md` `WEAPON-REQ-003`–`013`, `WEAPON-FAIL-002`, `003`, §7,
`docs/spec/domains/combat.md` `COMBAT-REQ-001`, `009`, `COMBAT-DEC-003`,
`docs/spec/domains/ammo.md` `AMMO-REQ-004`. Blocked by `FA-5`'s bullet entity, which this loop
spawns on every successful shot.

## Findings

- **Cooldown mechanism** (`WEAPON-REQ-009`'s open question, now `weapon.md` `WEAPON-DEC-007`):
  vanilla `ItemCooldowns`, called by hand from a server-side `onUseTick`, not the automatic
  `useCooldown` property — `UseCooldown.apply()` only fires from `use()`/`finishUsingItem()`, once
  per interaction, which cannot pace `auto`'s repeated per-tick shots and would double-apply for
  `semi`/`pump`. `WeaponItem.use()`/`useOn()` start the vanilla "using item" session uniformly, for
  every fire mode, on both logical sides (the fire mode itself lives in server-only weapon data a
  remote client never has loaded, so deciding per-mode before that data is reachable would be
  wrong); `onUseTick` dispatches every fire-or-reload attempt through `FiringLogic#attempt`, which
  calls `player.getCooldowns().addCooldown(...)` itself with the derived `fireRateTicks`/
  `reloadTicks`. Since every base weapon shares the one `firearms:weapon` item, `ItemCooldowns`'s
  default cooldown-group-by-item-id would collide across different base weapons (firing an M1911
  would block an unrelated AKM); `FiringLogic` keys every check/start to a throwaway stack copy
  carrying a `minecraft:use_cooldown` component whose `cooldownGroup` is the base weapon's own id
  (`firearms:m1911`, ...), never persisted back onto the real stack (`DATA-REQ-005`).
- **Fire-mode dispatch, uniformly**: `semi`/`pump` take exactly one `attempt()` evaluation per press
  (`onUseTick` calls `player.stopUsingItem()` right after, regardless of outcome — `pump`'s "delay"
  is the same `fireRateTicks` cooldown `semi` uses, per `weapon.md`'s own "pump behaves as semi for
  cooldown purposes", not the reload-ticks value; a literal reading of the ticket's "the reload-style
  cooldown" phrase was resolved in favor of the domain spec's explicit statement). `auto` keeps
  attempting every tick the cooldown allows, and — per `UC-008` step 3 ("stops the loop the instant
  `firearms:ammo.loaded` reaches zero") — stops itself the instant a `RELOADED` or `EMPTY_CLICK`
  outcome occurs rather than auto-reloading and continuing mid-hold; a fresh press starts the next
  reload attempt. Reload itself only triggers when the magazine is already at zero (`ammo == null ||
  loaded() <= 0`), matching the ticket's own "if empty → reload attempt ... else if loaded → fire"
  Build description — a partial/manual top-up reload before the magazine empties is out of this
  ticket's scope (not named by any acceptance criterion).
- **Aiming rule** (`WEAPON-REQ-005`): no aim-down-sights control exists yet (the three scope mixins,
  `COMBAT-REQ-006`–`008`, land at a later ticket). Per this ticket's own instruction, `FiringLogic
  .isAiming(Player)` reads `player.isShiftKeyDown()` (sneaking) as the aiming signal in the
  meantime — swap this one method's body for the real control later; nothing else needs to change.
- **Sound granularity** (`WEAPON-REQ-013`): the ticket's own Build section names exactly
  `firearms:fire.<class>`, `firearms:fire.suppressed`, `firearms:reload`, `firearms:empty` — a
  class-grained reading of WEAPON-REQ-013's "a distinct fire sound ... per weapon", not a literal
  per-base-weapon one. `firearms.fire.FireSounds` follows that reading: one fire `SoundEvent` per
  `WeaponClass` (6), one universal suppressed variant, one universal reload and one universal
  empty-click sound (9 total). A suppressor swaps in the universal suppressed event (a shorter fixed
  8-block range, `COMBAT-REQ-009`) and the shot is played at half volume on top of that. All nine
  `.ogg` files are near-silent generated placeholders (`ffmpeg`'s experimental `vorbis` encoder,
  `-strict -2`, ~0.25s, volume≈0.02) — a later ticket replaces the actual recordings.
- **Test infrastructure fix, incidental**: `SourceSurfaceTest.noSourceOrResourceFileNamesPubgOrItsBranding`
  walked every file under `src/main/resources` with `Files.readString` (UTF-8), which threw
  `MalformedInputException` the moment a binary asset existed there — this ticket's `.ogg` files are
  the first ones. Fixed by excluding known binary extensions (`.ogg`, `.png`, `.jpg`, `.jpeg`,
  `.ttf`, `.otf`) from that scan; `FA-9`'s item textures would have hit the identical failure
  otherwise.
- **Spec sync scope**: `weapon.md`'s two resolved open questions (AWM fire-mode reuse, cooldown
  wiring) are recorded as `WEAPON-DEC-006`/`007` in both the vault and this branch's `docs/spec/`
  copy, added by hand rather than via `just spec-sync` — the vault has already advanced ahead of
  `development` with `FA-4`'s own `WEAPON-DEC-005` (the anvil mixin), and a full sync would have
  pulled that unmerged, unrelated content into this diff. `just doctor`'s `check_spec_copy` will
  still report a (pre-existing, one-line) divergence from the vault until `FA-4` merges; `just
  check` does not run `doctor` and is unaffected.
