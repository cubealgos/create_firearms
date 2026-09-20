---
schema_version: 1
id: 01M2YWFBXTT5YB7HD6EBBHBZMN
key: FA-18
type: feat
title: JEI recipe-viewer category
created_by: kevin
created_at: 2026-09-20T07:46:15Z
---

## Scope

A self-contained JEI/EMI category for the smithing and deploying attach recipes, deferred at 1.0 by
`UI-DEC-001` since `create_synthetic_diamonds`'s own JEI category work already found Create Fly's
default category unable to render a custom recipe class without crashing, and this mod's two custom
recipe classes need the identical bespoke category work before they can safely appear
(`decisions/DEC-015-jei-deferred.md`).

## Approach

Confirm at this ticket whether `create_synthetic_diamonds`'s own eventual JEI ticket (if and when
built) established a reusable pattern for a custom-recipe-class category; if so, follow it here
rather than re-deriving the crash workaround independently.

## Acceptance criteria

- [ ] The smithing attach recipe and the deploying attach recipe both render correctly in JEI (or
      EMI) with no crash.
- [ ] `just check` green.

## Constraints and prior findings

`docs/spec/decisions/DEC-015-jei-deferred.md`, `docs/spec/domains/ui.md` `UI-REQ-005`,
`UI-DEC-001`. Backlog, `M7`; not scoped for 1.0. Blocked by `FA-16` (ships only after 1.0 stabilizes).
