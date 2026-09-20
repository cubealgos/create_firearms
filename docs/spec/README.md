---
title: "create_firearms spec — index"
type: "spec"
category: "create_firearms"
repo: "create_firearms"
---

# Create: Firearms — specification

A Fabric mod on Create Fly for Minecraft 26.2: a roster of modern firearms under real-world
designations, built from a base weapon plus attachments permanently attached at the vanilla
smithing table by a player or by a Create deployer on a contraption, no new block anywhere and no
detaching — a weapon is crafted and, slot by slot, done (Kevin, 2026-09-20,
`decisions/DEC-017-no-detach-durability.md`). Combat runs under vanilla damage rules; stats are
derived at runtime, never baked; attachments are universal by slot; a weapon loses ordinary vanilla
durability per shot and is neither repairable at an anvil nor enchantable; weapons and attachments
sell through the existing weaponsmith, ammunition through the existing fletcher, with a master
buy-back trade wanting a fully-configured weapon for many emeralds. The fifth Create Fly add-on
built on the way to `create_civilization`; it is nonetheless a distributed product with real users
and is specified as one (`decisions/DEC-001-classification.md`).

This spec is the distributed-product spec sheet in the chunked format. The sheet's sections map to
files as follows; a section marked *out of scope* says why in the file that would have held it.

| Sheet section | File |
|---|---|
| §1 Document control | this file: identifiers, state, decisions |
| §2 Executive summary and business context | `00-context.md` |
| §3 Product architecture and runtime topology | `04-architecture.md` |
| §4 Domain-driven functional specifications | `01-actors.md`, `02-journeys.md`, `03-glossary.md`, `domains/weapon.md`, `domains/attach.md`, `domains/ammo.md`, `domains/combat.md`, `domains/trade.md`, `domains/ui.md` |
| §5 Interface contracts and integration | `contracts/platform-matrix.md`, `contracts/public-surface.md`, `contracts/data-contract.md` |
| §6 Compliance, security and governance | `operations/compliance.md` |
| §7 Release engineering, distribution and support | `operations/release.md`, `operations/testing.md` |
| §8 Migration, compatibility and out of scope | `00-context.md` §What it will not do, `contracts/data-contract.md` |
| Appendix: technical blueprints | `04-architecture.md` §Shape |

## Files and state

| File | Domain prefix | State |
|---|---|---|
| `00-context.md` | — | written |
| `01-actors.md` | `ACTORS` | written |
| `02-journeys.md` | `UC` | written |
| `03-glossary.md` | — | written |
| `04-architecture.md` | `ARCH` | written |
| `domains/weapon.md` | `WEAPON` | written |
| `domains/attach.md` | `ATTACH` | written |
| `domains/ammo.md` | `AMMO` | written |
| `domains/combat.md` | `COMBAT` | written |
| `domains/trade.md` | `TRADE` | written |
| `domains/ui.md` | `UI` | written |
| `contracts/platform-matrix.md` | `PLATFORM` | written |
| `contracts/public-surface.md` | `SURFACE` | written |
| `contracts/data-contract.md` | `DATA` | written |
| `operations/compliance.md` | `COMP` | written |
| `operations/release.md` | `REL` | written |
| `operations/testing.md` | `TEST` | written |

## Identifiers

`<DOMAIN>-<KIND>-<NNN>`: `WEAPON-REQ-004`, `TRADE-UC-001` (use cases are flat, see below),
`COMBAT-FAIL-001`, `ARCH-DEC-002`. Use cases themselves are `UC-NNN`, flat across the project.
Permanent; a withdrawn item keeps its number.

## Verifications

Every row is a claim in this sheet traced to `create-fly-potato-cannon-and-deploying-26-2.md`
("note 1") or `smithing-and-item-model-layers-26-2.md` ("note 2"), both 2026-09-20, both read via
`javap` against the Create Fly jar and the merged Minecraft jar — no sources jar existed for
either — or, for row 15, to a direct `javap` check of the same merged 26.2 jar run for
`decisions/DEC-017-no-detach-durability.md` after that ruling landed. A claim not in this table and
not in either note is marked "to verify at the first ticket" where it appears.

| # | Claim | Note, section |
|---|---|---|
| 1 | `ProjectileUtil.getHitResultOnMoveVector` is a swept-segment trace over the entire tick's movement, correct at arbitrary velocity by construction — no tunneling risk from speed alone | note 1 §A.3, §C.2 |
| 2 | `RecipeManager`/`RecipeMap` bucket recipes by the live `Recipe.getType()` object, not the JSON `"type"`/serializer field, confirmed independently for `create:deploying` and for vanilla `RecipeType.SMITHING`, matching `create_synthetic_diamonds`'s own finding for `create:pressing` | note 1 §B.3; note 2 §A.3 |
| 3 | `ItemApplicationRecipe`'s shared default `assemble()` never merges either input stack's live components; it builds output purely from `ProcessingOutput.rollOutput` over the recipe's own fixed JSON `components` patch | note 1 §B.2 |
| 4 | `SmithingRecipe` is an interface whose `getType()` is a default method returning the live `RecipeType.SMITHING` singleton; vanilla's own `SmithingTrimRecipe.assemble` already demonstrates the cross-slot merge pattern (read a component off `addition`, write it onto a copy of `base`) | note 2 §A.1, §A.2 |
| 5 | The smithing table's slot-filter/highlight system recognizes any `SmithingRecipe` implementor via a bare `instanceof` check, independent of `getType()` | note 2 §A.3 |
| 6 | `Player.isScoping()` = `isUsingItem() && getUseItem().is(Items.SPYGLASS)` — an exact identity check, never inspecting the use-animation; not overridden by `LocalPlayer`/`AbstractClientPlayer` | note 2 §C.1 |
| 7 | The FOV zoom (`0.1f`), the overlay texture, and held-item render suppression all gate on `isScoping()` alone, each a fresh hardcoded literal or field; no Fabric API FOV/zoom/camera hook exists anywhere in the cached modules | note 2 §C.2 |
| 8 | `minecraft:select`'s built-in `minecraft:component` property and `minecraft:condition`'s built-in `minecraft:has_component` property both branch directly on any normally-registered `DataComponentType`'s raw value or presence, with zero mixin, as long as the branch key is directly-valued rather than derived | note 2 §B.2 |
| 9 | `ItemCost` carries a real `DataComponentExactPredicate`; its `test()` matches sparse across component types (an unnamed type is never inspected) but whole-value equality within one named type — proven by the one real vanilla trade using `wants.components` | note 2 §D.3 |
| 10 | `create_metered_motor` already ships the exact `"replace": false` tag-merge mechanism this mod reuses for the weaponsmith/fletcher trade extension, end to end, with `gives.components` accepting a full `DataComponentPatch` | note 2 §D.2 |
| 11 | `create_villager_customers`'s own `StackShape` is `(itemId, count)` only; `TransactionExecutor.shapeOf` discards components when reading a live stack, confirmed by reading that mod's source directly | note 2 §D.3 |
| 12 | The potato cannon's ammo lookup is a registry-driven inventory scan via `ProjectileWeaponItem.getAllSupportedProjectiles()`; its fire-rate pacing rides on vanilla `ItemCooldowns`, applied automatically from `use()`/`finishUsingItem()` but not `releaseUsing()` | note 1 §A.1, §A.2; §C.1 |
| 13 | A `DamageSource` built with the projectile as the direct entity and the shooting player as the causing entity gets `pvp` gamerule and team-allegiance compliance for free from `ServerPlayer.hurtServer`/`canHarmPlayer`, with zero extra code | note 1 §A.6; note 2's own §C.4 reference is note 1's, restated |
| 14 | No charcoal-block-shaped concern applies here; `create:mixing` natively accepts multiple distinct ingredients, the natural automation shape for a cartridge's casing + propellant + projectile, deferred to a later release | note 2 §E.3, §E.4 |
| 15 | `ItemStack.isValidRepairItem` reads `DataComponents.REPAIRABLE` and returns false when absent (blocks the anvil's material-repair path); `ItemStack.isEnchantable()` returns false immediately when `DataComponents.ENCHANTABLE` is absent (blocks the enchanting table); `AnvilMenu.createResult()` also runs a *second*, independent same-item combine-repair path, gated only on `ItemStack.isDamageableItem()` and item identity, never on `REPAIRABLE` — a residual gap `REPAIRABLE`'s omission alone does not close | DEC-017 check, `decisions/DEC-017-no-detach-durability.md` |

## Divergences from heimathafen standards

| Standard | Divergence | Recorded in |
|---|---|---|
| `standards/legal/default-license-apache-2-cla.md` | MIT, no CLA | `decisions/DEC-003-licence.md` |
| "no remote unless justified later" | Public on Forgejo under `cubealgos` from the bootstrap, mirrored to GitHub with the issue tracker there, as all four siblings ended up | `decisions/DEC-003-licence.md` |
| The three earliest siblings' own "Create Fly: `<Name>`" Modrinth display-name pattern | This mod's listing is titled **"Create: Firearms"**, following `create_synthetic_diamonds`'s own precedent rather than the three earlier siblings' | `decisions/DEC-002-name.md` |

## Decisions

| ID | Decision | State |
|---|---|---|
| `DEC-001` | Distributed product, full spec sheet | written |
| `DEC-002` | `create_firearms`, mod id `firearms`, display "Create: Firearms" | written |
| `DEC-003` | MIT, no CLA; public under the cubealgos organisation from the first commit | written |
| `DEC-004` | Toolchain as the siblings; three client-only mixin targets, zero server-side mixin | written |
| `DEC-005` | One weapon per class, six calibres, none repeated, at 1.0; full attachment set ships regardless | written |
| `DEC-006` | No new block: the smithing table and the deployer replace the workstation | written |
| `DEC-007` | Universal attachments by slot, all 22 ship at 1.0 | written |
| `DEC-008` | Bullets are projectile entities, against the research's hitscan recommendation | written |
| `DEC-009` | Scopes reuse the spyglass mechanic via three accepted client mixins; red dot/holo never zoom | written |
| `DEC-010` | Stats are dynamic, a pure function, never baked | written |
| `DEC-011` | Per-slot data components, not one combined attachment map | written |
| `DEC-012` | Weaponsmith and fletcher trades, no new gunsmith profession | written |
| `DEC-013` | Master buy-back trades want a specific configuration for many emeralds | written |
| `DEC-014` | Combat scope is mobs and players under vanilla damage rules only | written |
| `DEC-015` | No JEI/EMI category at 1.0 | written |
| `DEC-016` | `create_villager_customers` needs a component-predicate match rule; recorded, not scoped here | written |
| `DEC-017` | Attachments never come off; ordinary durability; not repairable; not enchantable | written |

## Open questions gathered

Every ruling this sheet needed was already made by Kevin before the sheet was written
(`rulings-2026-09-20.md` §"Kevin's final rulings" and §"Kevin's ruling on detaching"). What remains
open is technical detail belonging to the first tickets, not design — with one exception, named
plainly:

- **This sheet's own reading of "a weapon is crafted and done"** — that it applies to a filled slot
  only, leaving a weapon's other, still-empty slots fillable at any later time — is Claude's
  interpretation, not a verified Kevin ruling on that specific point. Flagged for Kevin to confirm
  or correct (`decisions/DEC-017-no-detach-durability.md`; `domains/attach.md` §1, §7).
- The exact recipe serializer/type ids for the shared attach recipe on both front ends
  (`04-architecture.md` `ARCH-DEC-002`, `ARCH-DEC-003`).
- The exact JSON field names and codec shape for weapon, attachment, and cartridge data files
  (`domains/weapon.md`, `domains/attach.md`, `domains/ammo.md`).
- Exact crafting-table grids and materials for all six bases, 22 attachments, and 6 cartridges
  (`domains/weapon.md` `WEAPON-REQ-006`, `domains/attach.md` `ATTACH-REQ-007`,
  `domains/ammo.md` `AMMO-REQ-001`).
- Whether the anvil's same-item combine-repair path (`domains/weapon.md` `WEAPON-FAIL-006`) is an
  acceptable residual gap in "not repairable in an anvil," or needs a narrow `AnvilMenu` mixin to
  close fully — confirmed present in the 26.2 jar, independent of the `REPAIRABLE` component this
  sheet already omits (`decisions/DEC-017-no-detach-durability.md`).
- Bullet gravity multiplier and despawn-life tuning, and every stat and attachment modifier in
  `domains/weapon.md` and `domains/attach.md` — explicitly marked "proposed, retune at the sweep"
  throughout, not settled numbers.
- Trade prices across both professions and all levels (`domains/trade.md` §7).

One open item is **not** this project's to close: the `create_villager_customers` match-rule
extension `decisions/DEC-016-villager-customers-requirement.md` records is a requirement on that
sibling mod, tracked there, not a blocker on `create_firearms` 1.0 itself.
