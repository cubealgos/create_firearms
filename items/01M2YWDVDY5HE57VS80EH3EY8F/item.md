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

- [ ] A weapon with known components fires, decrements ammo and durability by one, starts the
      correct cooldown, and refuses to fire (empty-click only) at zero ammo (`WEAPON-REQ-007`,
      `008`).
- [ ] `auto` mode repeats at the fire-rate interval while held and stops the instant ammo reaches
      zero, control releases, or the weapon leaves the player's hand (`UC-008`).
- [ ] Reload consumes cartridges up to the derived magazine size from a matching-calibre stack and
      no-ops silently with none present (`WEAPON-REQ-010`, `011`).
- [ ] A suppressed weapon's fire sound is measurably quieter/shorter-range than an unsuppressed
      one's.
- [ ] The AWM's fire-mode reuse decision (`semi` with a longer fire-rate vs. a distinct third mode)
      is confirmed and recorded in `domains/weapon.md` §8.
- [ ] `just check` green.

## Constraints and prior findings

`docs/spec/domains/weapon.md` `WEAPON-REQ-003`–`013`, `WEAPON-FAIL-002`, `003`, §7,
`docs/spec/domains/combat.md` `COMBAT-REQ-001`, `009`, `COMBAT-DEC-003`,
`docs/spec/domains/ammo.md` `AMMO-REQ-004`. Blocked by `FA-5`'s bullet entity, which this loop
spawns on every successful shot.
