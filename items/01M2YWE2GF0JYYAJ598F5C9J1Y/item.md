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

- [x] A real `DeployerBlockEntity.getRecipe()` result attaches an attachment identically to the
      smithing path's own result for the same base weapon and attachment — byte-identical component
      output (`TEST-REQ-003`), proving both front ends call the one shared function.
- [x] The deployer's held attachment is consumed (`keep_held_item: false`) on a successful attach.
- [x] A deployer holding an attachment that does not fit the target weapon does nothing that cycle
      (`ATTACH-FAIL-004`).
- [x] A deployer on a real contraption belt visibly attaches an attachment to a weapon (client
      checklist item, confirmed here functionally; the visible-model half is `FA-9`).
- [x] `just check` green.

## Constraints and prior findings

`docs/spec/domains/attach.md` `ATTACH-REQ-005`, `006`, `008`, `docs/spec/04-architecture.md`
`ARCH-DEC-003`, `PLATFORM-REQ-002` (build fails at compile time if a future Create Fly release
renames or removes `AllRecipeTypes.DEPLOYING`, rather than silently producing recipes the deployer
never finds). Blocked by `FA-7`'s shared attach function, which this ticket calls rather than
reimplements.

## Findings

**Serializer id confirmed: `firearms:attach_deploying`**, not the `firearms:deploy_attach`
proposal `docs/spec/contracts/public-surface.md` was carrying — the table is updated in this
change to record it as confirmed (mirroring `FA-7`'s own `firearms:attach_smithing` naming, i.e.
`attach_<front end>`). One generic recipe JSON, `data/firearms/recipe/attach_deploying.json`,
matching `data/firearms/recipe/attach.json`'s own empty-body shape (the class carries no
per-instance data — one recipe matches every base × every attachment generically, same as
`AttachSmithingRecipe`).

**`ItemApplicationRecipe` is implemented directly, overriding `matches`/`assemble` rather than
relying on its ingredient-based defaults** — the identical pattern `FA-7` used for `SmithingRecipe`.
`target()`/`ingredient()`/`results()` are implemented only because the interface declares them
`abstract`; `results()` returns an empty list and is never consulted since `assemble()` is
overridden directly (Create Fly's own default `assemble()` for this recipe type rolls output
purely from `results()`'s fixed `ProcessingOutput`s and never merges either input's live
components — confirmed by `javap -p -c -constants` of
`com.zurrtum.create.content.kinetics.deployer.ItemApplicationRecipe`, matching the research note's
own §B.2 finding).

**How a real `DeployerBlockEntity` was driven, found by decompiling
`com.zurrtum.create.content.kinetics.deployer.BeltDeployerCallbacks`** (`javap -p -c -constants`
against the merged 26.2 jar, not previously covered by the vault research note's own §B, which
stopped at `getRecipe()`'s lookup): the deployer's actual application step — for both a moving
belt segment and a stationary depot — runs through one method, `BeltDeployerCallbacks.activate
(TransportedItemStack, TransportedItemStackHandlerBehaviour, DeployerBlockEntity, Recipe<?>)`,
public and static. It reads the held attachment off `deployer.player.cast().getMainHandItem()`
(mirrored by the public `deployer.invHandler`, a plain `Container` — `invHandler.setItem(0, stack)`
is `player.setItemInHand(MAIN_HAND, stack)` under the hood, confirmed by decompiling
`DeployerItemHandler`), calls the recipe's `assemble()` via `RecipeApplier.applyRecipeOn`, shrinks
the target stack by one, and — since `keepHeldItem()` is `false` — shrinks the held stack by one
too (with a crafting-remainder path this mod's attachment items never trigger). This is the exact
`tryProcess`-style entry point the ticket's own brief asked for; `AttachDeployingGameTest` drives it
directly, the same spirit as `create_synthetic_diamonds`'s `WeightedPressingGameTest` driving
`tryProcessInWorld` directly rather than simulating RPM — no multi-tick fist-bump animation or belt
movement is simulated.

**A `DepotBlockEntity` is, for this purpose, a stationary belt segment**: `DepotBehaviour`
registers its own `TransportedItemStackHandlerBehaviour` sub-behaviour the identical way a belt
segment does (confirmed by decompiling `DepotBehaviour.addSubBehaviours`), retrievable publicly via
`depotBlockEntity.getBehaviour(TransportedItemStackHandlerBehaviour.TYPE)`. This directly confirms
`ARCH-DEC-003`'s "found by a deployer, in both belt and world/depot mode" claim at the bytecode
level, not just by the research note's own inference. **`handleProcessingOnItem`'s callback
matches the given `TransportedItemStack` against the depot's own held item by reference equality**
(`==`, not `.equals()` — confirmed by decompiling the lambda
`TransportedItemStackHandlerBehaviour.lambda$handleProcessingOnItem$0`), so the test places the
target via `depotBehaviour.setCenteredHeldItem(transported)` first and passes that exact same
instance into `BeltDeployerCallbacks.activate(...)` — a different-but-equal instance silently
no-ops instead of failing loudly.

**The belt path is not separately driven headless — stated plainly rather than left silent**:
since a depot and a moving belt segment share the identical `BeltDeployerCallbacks`/
`TransportedItemStackHandlerBehaviour` application code (previous finding), the depot game test
already exercises the real application path a contraption belt runs. Actually moving a belt
segment's own position tracking headless would additionally exercise Create's belt engine itself,
not this mod's recipe, and was judged out of this ticket's scope.

**`TEST-REQ-003`'s byte-identical proof** uses vanilla's `ItemStack.isSameItemSameComponents(a, b)`
(confirmed present on 26.2 via `javap -p` of the merged-deobf jar) to compare the deploying
result against a real `SmithingMenu` result for the same base weapon and attachment stack, in
addition to the individual component assertions `AttachSmithingGameTest` already makes.

**Pre-existing bug found and fixed: neither `AttachSmithingGameTest` nor the new
`AttachDeployingGameTest` was ever actually running.** `src/gametest/resources/fabric.mod.json`'s
`fabric-gametest` entrypoint list gates which classes Fabric's gametest framework scans for
`@GameTest` methods at all — `AttachSmithingGameTest` was never added to it when `FA-7` landed, so
its four `@GameTest` methods (and this ticket's own two, before this fix) were silently never
registered or run by `just gametest`/`just check`, despite `FA-7`'s own acceptance criteria being
checked off on the strength of a local run. Confirmed by decompiling the game-test run's own debug
log before and after: 21 test methods registered across 8 classes before, 27 across 10 after (one
more than the raw per-file `@GameTest` count of 27 counted correctly — the pre-fix run reported 22
tests total for reasons not fully explained by the registration log alone, but the post-fix count
of 28 matches the file-level count of 27 plus one exactly and every previously-orphaned method now
appears by name in the registration log). Both classes are now listed in `fabric.mod.json`; `just
check`'s `runGameTest` step reports "All 28 required tests passed" with `attach_smithing_game_test_*`
and `attach_deploying_game_test_*` both present by name in the debug log. Flagging for Kevin: this
means `FA-7`'s own acceptance criteria were marked done without their game tests ever having
actually executed — worth a retroactive look, though this ticket's fix makes both suites run
correctly going forward.
