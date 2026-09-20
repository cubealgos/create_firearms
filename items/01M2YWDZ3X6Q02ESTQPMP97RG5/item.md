---
schema_version: 1
id: 01M2YWDZ3X6Q02ESTQPMP97RG5
key: FA-7
type: feat
title: The shared attach function and the smithing recipe class
created_by: kevin
created_at: 2026-09-20T07:45:29Z
---

## Scope

The shared attach function: given a base weapon and an attachment, matches only when the weapon's
class has that attachment's slot and the slot's component is currently absent, and returns a copy
of the base weapon with the named slot's component set to the attachment's id, every other
component unchanged (`ATTACH-REQ-001`–`003`); never matches an occupied slot, and no other recipe
or path ever changes or clears a filled slot (`ATTACH-REQ-002`, `decisions/DEC-017-no-detach-durability.md`).
A `SmithingRecipe` implementor calling this function: base slot = weapon, addition slot =
attachment, template slot empty; found by the vanilla smithing table with zero mixin, inheriting
`getType() == RecipeType.SMITHING` as a default method (`04-architecture.md` `ARCH-DEC-002`). Not
the deploying front end (`FA-8`, which calls this same function) and not the item models that
render a filled slot (`FA-9`).

## Approach

One pure(ish) attach function in `firearms.model` or a thin server-side wrapper around it — confirm
at this ticket whether the slot-match/merge logic itself can stay in the pure package (it reads and
writes `DataComponentType` values, which are Minecraft types, so it likely cannot be fully pure;
`verifyPurePackage` settles this by simply failing if it is placed there wrongly) — called from both
the smithing recipe's `assemble()` and, at `FA-8`, the deploying recipe's own `assemble()`, never
duplicating the match/merge logic between the two (`ATTACH-REQ-008`). Follows the pattern vanilla's
own `SmithingTrimRecipe` already demonstrates: read one component off one input slot, write it onto
a copy of another (`ARCH-DEC-002`). The smithing table's own slot-filter/highlight system recognizes
any `SmithingRecipe` implementor via a bare `instanceof` check, independent of `getType()`, so no
extra registration step is needed for correct highlighting.

## Acceptance criteria

- [ ] A real `SmithingMenu` result attaches an attachment into the named empty slot and changes no
      other component (`ATTACH-REQ-001`, `003`).
- [ ] The recipe never matches an already-occupied slot (`ATTACH-REQ-002`) — a game test proves
      re-offering a second attachment for an already-filled slot produces no result.
- [ ] The recipe never matches a class/slot mismatch (e.g. a scope onto a shotgun) —
      `ATTACH-FAIL-001`.
- [ ] The shared attach function has no duplicated copy in the deploying recipe class once `FA-8`
      lands (proven together at `FA-8`'s own acceptance, `TEST-REQ-003`).
- [ ] `just check` green.

## Constraints and prior findings

`docs/spec/domains/attach.md` `ATTACH-REQ-001`–`003`, `008`, `ATTACH-DEC-001`, `ATTACH-DEC-003`,
`docs/spec/04-architecture.md` `ARCH-DEC-002`, `ARCH-FAIL-002` (fallback: a narrowly-scoped mixin on
this mod's own recipe lookup only, if `RecipeMap`'s live-`getType()` bucketing does not behave as
the research's disassembly predicts). Blocked by `FA-3`'s per-slot attachment components. `ATTACH-REQ-004`
and `ATTACH-DEC-002` (detach tools) are withdrawn by `decisions/DEC-017-no-detach-durability.md` —
not implemented, ids not reused.
