---
schema_version: 1
id: 01M2YWECS01YV0BEMP7XH77ZWP
key: FA-11
type: feat
title: Tooltips and the recoil kick
created_by: kevin
created_at: 2026-09-20T07:45:43Z
---

## Scope

The weapon tooltip: name and class (from `firearms:base.weapon_id`), the live-derived final stats
(damage, muzzle velocity, spread, fire rate, fire mode, magazine size, reload ticks, recoil,
durability remaining), every present slot's occupant with an empty-but-present-in-class slot shown
as unequipped, never hidden, and loaded ammo vs. derived magazine size, recomputed on every hover
rather than cached (`UI-REQ-001`, `domains/ui.md` §3). The cosmetic client-side recoil camera kick
that visibly renders on firing, reading `FA-6`'s server-authoritative recoil packet
(`COMBAT-REQ-011`). Not the scope overlay (`FA-10`, already landed) and not the debug command
(`FA-12`).

## Approach

The tooltip calls the exact same live stat function firing does (`FA-2`), never a cached or
duplicated computation — this is what `UI-DEC` and `WEAPON-REQ-003` are protecting against
structurally. The recoil kick is client-only, purely cosmetic, and carries no authority over any
hit, damage, or ammo state (`COMBAT-REQ-011`); it reads the packet `FA-6` already sends rather than
re-deriving anything server-side.

## Acceptance criteria

- [x] A tooltip on any 1.0 weapon lists its derived stats and every slot's occupant or "empty,"
      correct immediately after gaining an attachment with no stale read (`UI-REQ-001`).
- [x] The recoil kick visibly renders on every shot, at each of the six weapons' own fire modes
      (client checklist item).
- [x] The recoil kick carries no server authority — a game test or code-review check confirms
      nothing server-side reads the client recoil packet as an input to damage, ammo, or hit
      resolution.
- [x] `just check` green.

## Constraints and prior findings

`docs/spec/domains/ui.md` `UI-REQ-001`, §3, `docs/spec/domains/combat.md` `COMBAT-REQ-011`,
`COMBAT-DEC-003`. Blocked by `FA-6` (the firing loop and the recoil packet this ticket renders).
