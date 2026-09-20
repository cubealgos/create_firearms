---
schema_version: 1
id: 01M2YWE2GF0JYYAJ598F5C9J1Y
key: FA-8
type: feat
title: The deploying recipe class
created_by: kevin
created_at: 2026-09-20T07:45:33Z
---

## Scope

A custom `Recipe<ItemApplicationInput>` (directly, or via the public `ItemApplicationRecipe`
interface for its free `matches()` default) whose `getType()` returns the literal
`AllRecipeTypes.DEPLOYING` field, found by a Create deployer with zero mixin in both belt and
world/depot mode (`ARCH-DEC-003`, `ATTACH-REQ-005`); `target` (the weapon) at input index 0,
`ingredient` (the deployer's held attachment) at index 1; `assemble()` calls `FA-7`'s shared attach
function directly, since Create Fly's own default `assemble()` for this recipe type never merges
either input's live components; `keep_held_item: false`, consuming the held attachment
(`ATTACH-REQ-006`). Not the smithing front end (`FA-7`, already landed) and not the item models
(`FA-9`).

## Approach

Reuses `FA-7`'s shared attach function verbatim for the match/merge logic — this ticket's own
`assemble()` is a thin adapter reading `target`/`ingredient` off `ItemApplicationInput` and calling
that function, never re-implementing slot-match or component-merge (`ATTACH-REQ-008`). The
belt-deployer callback always shrinks the target weapon by one and replaces it with the result,
exactly as any consumable deploying recipe (e.g. planks into a cogwheel) does.

## Acceptance criteria

- [ ] A real `DeployerBlockEntity.getRecipe()` result attaches an attachment identically to the
      smithing path's own result for the same base weapon and attachment — byte-identical component
      output (`TEST-REQ-003`), proving both front ends call the one shared function.
- [ ] The deployer's held attachment is consumed (`keep_held_item: false`) on a successful attach.
- [ ] A deployer holding an attachment that does not fit the target weapon does nothing that cycle
      (`ATTACH-FAIL-004`).
- [ ] A deployer on a real contraption belt visibly attaches an attachment to a weapon (client
      checklist item, confirmed here functionally; the visible-model half is `FA-9`).
- [ ] `just check` green.

## Constraints and prior findings

`docs/spec/domains/attach.md` `ATTACH-REQ-005`, `006`, `008`, `docs/spec/04-architecture.md`
`ARCH-DEC-003`, `PLATFORM-REQ-002` (build fails at compile time if a future Create Fly release
renames or removes `AllRecipeTypes.DEPLOYING`, rather than silently producing recipes the deployer
never finds). Blocked by `FA-7`'s shared attach function, which this ticket calls rather than
reimplements.
