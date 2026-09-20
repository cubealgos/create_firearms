---
schema_version: 1
id: 01M2YWEQM3NYN10116V54M9F7F
key: FA-14
type: test
title: Requirement-to-test table, three check runs, client checklist
created_by: kevin
created_at: 2026-09-20T07:45:54Z
---

## Scope

A requirement-to-test table mapping every `WEAPON-REQ`, `ATTACH-REQ`, `AMMO-REQ`, `COMBAT-REQ`,
`TRADE-REQ` and `UI-REQ` id to the test that proves it, or a recorded ruling where no automated test
applies (`TEST-REQ-001`); three consecutive green `just check` runs on a clean checkout; Kevin's own
client checklist covering firing feel across all six weapons and fire modes, the scope mixins'
zoom/overlay/suppression behaviour, visible attach on both front ends, and confirming no JEI/EMI
entry appears anywhere (`operations/testing.md` client-checklist row). Balance-sweep pass over every
proposed stat and price, since every number in `domains/weapon.md`, `domains/attach.md`,
`domains/ammo.md` and `domains/trade.md` is marked a proposed starting point, not a balanced final
figure. Not the release build itself (`FA-16`) and not the Modrinth assets (`FA-15`).

## Approach

Walk each domain file's requirements table, add or confirm the owning test, and where a requirement
genuinely cannot be pure-tested (the two recipe classes actually being found by a real
`SmithingMenu`/`DeployerBlockEntity`, the scope mixins producing a visible effect against a real
client renderer, automatic-fire bullet-entity volume at real server tick rates —
`operations/testing.md`'s own "what is genuinely hard" section), name the game test or the client
checklist item that stands in for it instead of leaving the id unmapped.

## Acceptance criteria

- [ ] Every `WEAPON-REQ`, `ATTACH-REQ`, `AMMO-REQ`, `COMBAT-REQ`, `TRADE-REQ` and `UI-REQ` id in the
      six domain files is mapped to a test or a recorded ruling (`TEST-REQ-001`).
- [ ] `just check` is green three consecutive times on a clean checkout.
- [ ] Kevin's client checklist is completed and any findings are folded back into the relevant
      domain file or a follow-up ticket.
- [ ] The balance sweep either confirms the proposed stats/prices as final or records specific
      adjustments in the relevant domain files' own tables.

## Constraints and prior findings

`docs/spec/operations/testing.md` `TEST-REQ-001`–`004` and its own "what is genuinely hard" section,
`docs/spec/operations/release.md` `REL-REQ-003` (release notes must state every number is a proposed
starting point regardless of what this sweep confirms or changes). Blocked by `FA-11` (a complete
client experience to check) and `FA-13` (the trades the sweep also verifies).
