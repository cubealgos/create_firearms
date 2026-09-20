---
schema_version: 1
id: 01M2YWDD4FDRDRVQRPWTQ8A6QK
key: FA-2
type: feat
title: The pure stat model and derivation function
created_by: kevin
created_at: 2026-09-20T07:45:11Z
---

## Scope

`firearms.model`: the six weapon classes and their fixed per-class slot sets
(`domains/weapon.md` §3, `WEAPON-REQ-001`); the six 1.0 weapons with their base stats
(damage, muzzle velocity, spread, fire rate, fire mode, magazine, reload, recoil, durability,
`WEAPON-REQ-002`); the six calibres; the 22 attachments and their multiplicative stat modifiers by
slot (`domains/attach.md` §3); the pure function `stats(base, present slot components) → final
stats`, applying each present attachment's modifier in the fixed slot order muzzle, optic,
magazine, grip, stock (`WEAPON-REQ-003`); the out-of-range clamp-and-log path for a datapack's own
weapon/attachment data (`contracts/public-surface.md` `SURFACE-REQ-002`). Not the data component
types themselves or the item registration (`FA-3`), not firing (`FA-6`), not the attach function's
own slot-match/merge logic, which is a related but separate pure function landing at `FA-7`.

## Approach

A plain Java model with no Minecraft, Fabric or Create imports, gated by the build's
`verifyPurePackage` task. Represent a class as an enum naming its slot set; a weapon and an
attachment as records carrying their own stat/modifier fields; the derivation function as a static
method folding present modifiers over a base-stat record in the fixed slot order
`WEAPON-REQ-003` names, so the same order is not re-decided per call site. Every 1.0 weapon and
attachment is a literal constant for now (`WEAPON-DEC-002`, `ATTACH-DEC-001`'s "universal by slot"
framing); the datapack-driven loader that turns a JSON file into one of these values is `FA-3`'s
own territory, not this ticket's.

## Acceptance criteria

- [x] `verifyPurePackage` passes with zero Minecraft, Fabric or Create imports in `firearms.model`.
- [x] Unit tests cover the derivation function for every one of the six 1.0 weapons bare, and for
      every one of the 22 attachments applied singly and in combination, confirming the fixed
      slot-order application (`WEAPON-REQ-003`).
- [x] A unit test proves an out-of-range modifier (a negative magazine size, a zero fire-rate
      divide) clamps to the nearest valid value and logs once rather than crashing
      (`SURFACE-REQ-002`).
- [x] `just check` green.

## Constraints and prior findings

`docs/spec/domains/weapon.md` §3, §5 (`WEAPON-REQ-001`–`003`), `docs/spec/domains/attach.md` §3,
`docs/spec/contracts/public-surface.md` `SURFACE-REQ-002`, `docs/spec/decisions/DEC-010-dynamic-stats.md`,
`docs/spec/decisions/DEC-011-per-slot-components.md`. All roster and attachment numbers are
proposed starting points, not balanced final numbers (`docs/spec/domains/weapon.md` §3 note); this
ticket ships them as-is and defers retuning to the balance sweep (`FA-14`). `FA-1`'s bootstrap
scaffold; `firearms.model.package-info` already states this package's purpose.
