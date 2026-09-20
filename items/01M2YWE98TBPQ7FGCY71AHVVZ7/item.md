---
schema_version: 1
id: 01M2YWE98TBPQ7FGCY71AHVVZ7
key: FA-10
type: feat
title: "The scope mixins: isScoping widening, per-optic zoom, overlay"
created_by: kevin
created_at: 2026-09-20T07:45:40Z
---

## Scope

The three client-only mixins this mod's `04-architecture.md` `ARCH-DEC-001` names as its entire
mixin surface: widen `Player.isScoping()` to also return true while a player uses a firearm whose
attached optic's zoom factor is greater than 1.0 (`COMBAT-REQ-006`); read that optic's own zoom
factor in `AbstractClientPlayer.getFieldOfViewModifier`, in place of the spyglass's hardcoded
`0.1f` (`COMBAT-REQ-007`); swap the HUD's spyglass-overlay draw to the optic's own texture, in
place of `SPYGLASS_SCOPE_LOCATION` (`COMBAT-REQ-008`). Red dot and holo never satisfy the widened
gate — no FOV change, no overlay, no held-item suppression for them (`COMBAT-DEC-004`). Populates
`firearms.mixins.json`'s `client` array for the first time. Not the item model layers (`FA-9`,
already landed) and not the spread-while-aiming numeric effect, which is `WEAPON-REQ-005`'s own
territory in the stat function (`FA-2`, already landed).

## Approach

All three mixins gate on the same widened boolean check, so this is "one mixin family," not three
independent decisions (`decisions/DEC-009-scope-mechanic.md`). `Player.isScoping()` is an exact
`Items.SPYGLASS` identity check today, never keyed on use-animation (research
`smithing-and-item-model-layers-26-2.md` §C.1) — the widening mixin adds the firearm+zoom-optic
condition alongside the existing spyglass check, not in place of it. `compatibilityLevel: JAVA_25`
is already set in `firearms.mixins.json` from `FA-1`'s bootstrap.

## Acceptance criteria

- [ ] A firearm with a magnified optic (2x–15x) attached zooms the FOV by that optic's own factor
      while the aim control is held, and shows that optic's own overlay texture.
- [ ] A firearm with red dot or holo attached shows no FOV change, no overlay, and does not suppress
      the held-item render while aiming (`COMBAT-DEC-004`).
- [ ] Releasing the aim control returns the view, overlay and held-item render to normal exactly as
      releasing a vanilla spyglass does.
- [ ] `just check` green, including a `just client` manual confirmation on the checklist.

## Constraints and prior findings

`docs/spec/04-architecture.md` `ARCH-DEC-001`, `docs/spec/decisions/DEC-009-scope-mechanic.md`,
`docs/spec/domains/combat.md` `COMBAT-REQ-006`–`008`, `COMBAT-DEC-004`, `COMBAT-FAIL-003` (a second
mod also widening `isScoping()` coexists only if both widen additively — a mixin-ordering problem,
not this mod's to solve alone), `docs/spec/contracts/platform-matrix.md` `PLATFORM-REQ-003` (build
fails at compile time if a future Minecraft release moves `isScoping()`'s call sites). Blocked by
`FA-7` (needs a real optic-bearing weapon to test against).
