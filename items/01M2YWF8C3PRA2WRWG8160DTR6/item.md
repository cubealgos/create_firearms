---
schema_version: 1
id: 01M2YWF8C3PRA2WRWG8160DTR6
key: FA-17
type: feat
title: The remaining roster as data files
created_by: kevin
created_at: 2026-09-20T07:46:11Z
---

## Scope

The rest of `rulings-2026-09-20.md`'s full 18-weapon, PUBG-shaped roster proposal, beyond the six
1.0 weapons, shipped as base weapon data files in an existing class — zero new Java per weapon
(`WEAPON-DEC-003`, `SURFACE-REQ-003`). Each addition is a minor version, never a major one, since it
breaks no stable surface (`REL-REQ-004`). Every weapon uses only its real-world designation, per the
same compliance rule the 1.0 roster follows (`COMP-REQ-002`).

## Approach

Confirm the specific weapons and their stats against real-world reference data at this ticket, the
same way the 1.0 roster's own numbers were proposed; each is a data file naming an existing class,
calibre and base stats, loaded by `FA-3`'s existing loader with no code change. No new class, no new
slot, no new attachment — the full 22-attachment set already covers every 1.0 slot.

## Acceptance criteria

- [ ] Each added weapon crafts, fires, and takes every attachment its class's slot set allows,
      exactly as a 1.0 weapon does.
- [ ] No new Java is touched to add any weapon in this ticket (`WEAPON-DEC-003`).
- [ ] Every added weapon uses only its real-world designation (`COMP-REQ-002`, enforced by the
      existing `SourceSurfaceTest` scan).
- [ ] The release is tagged as a minor version bump over `1.0.0+26.2`.
- [ ] `just check` green.

## Constraints and prior findings

`docs/spec/decisions/DEC-005-roster.md`, `docs/spec/domains/weapon.md` `WEAPON-DEC-002`, `003`,
`docs/spec/contracts/public-surface.md` `SURFACE-REQ-003`, `docs/spec/operations/release.md`
`REL-REQ-004`. Backlog, `M7`; not scoped for 1.0. Blocked by `FA-16` (ships only after 1.0 stabilizes).
