---
title: "create_firearms spec — architecture: two recipe front ends, one attach function, three client mixins"
type: "spec"
category: "create_firearms"
---

# 04 — Architecture

Sheet §3. Everything here follows `create-fly-potato-cannon-and-deploying-26-2.md` and
`smithing-and-item-model-layers-26-2.md`; where the research left a proposal rather than a settled
fact, or a Kevin ruling overrode the research's own recommendation, both are named plainly.

## Shape

```
 crafting table          smithing table             deployer (contraption)        server combat
 ┌────────────────┐     ┌──────────────────────┐   ┌───────────────────────┐    ┌──────────────────┐
 │ base weapons,    │     │ Recipe<SmithingRecipe │   │ Recipe<ItemApplication │    │ bullet entity      │
 │ attachments,      │     │  Input> implements    │   │  Input>, getType() ==  │    │  (custom, gravity, │
 │ cartridges — all  │     │  SmithingRecipe        │   │  AllRecipeTypes         │    │  swept-segment hit │
 │ plain shaped/      │     │  (getType() default =  │   │  .DEPLOYING             │    │  test every tick)  │
 │ shapeless recipes  │     │  RecipeType.SMITHING)  │   │                         │    │                    │
 └────────────────┘     └──────────┬───────────┘   └───────────┬───────────┘    └────────┬──────────┘
                                    │                             │                          │
                                    └──────────────┬──────────────┘                          │
                                                    ▼                                         │
                                     shared attach function (`domains/attach.md`)              │
                                     reads/writes firearms:base, firearms:attachment_<slot>     │
                                                    │                                          │
                                                    ▼                                          ▼
                                     stat derivation function (`domains/weapon.md`) ◄───────────┘
                                     pure: (base, present slot components) → final stats
                                                    │
                        ┌───────────────────────────┼───────────────────────────┐
                        ▼                            ▼                            ▼
              item model (composite +      tooltip (`domains/ui.md`)   villager trades (`domains/trade.md`)
              condition/select, zero                                    tag-merge into weaponsmith/fletcher
              mixin for direct-valued
              components)

 client only: three mixins on Player.isScoping(), the FOV constant, and the overlay texture read
 (`domains/combat.md` COMBAT-REQ-006–008) — nothing else in this mod needs a mixin anywhere.
```

**No new block anywhere.** The only new server-side objects are two recipe classes (one per attach
front end), one entity class (the bullet), eight data component types, and the JSON/data files that
parametrize weapons, attachments, cartridges and trades. The only client-side code is the three
scope mixins and ordinary item-model/tooltip rendering.

## `ARCH-DEC-001` — a Fabric mod on Create Fly, one jar, Java 25, three client mixin targets

Same toolchain as the four siblings (`decisions/DEC-004-toolchain.md`): Loom 1.17, Gradle 9.5.1,
Kotlin DSL with a version catalog, one Gradle project — the pure surface (the stat derivation
function, the attach function, the spread/weighted-pick math) is small enough that a
package-purity check gives the same guarantee a second module would (`operations/testing.md`).

**Every mixin this mod carries is client-side and scoped to one shared gate**: `Player.isScoping()`
(widened to also cover an aiming firearm with a zoom-bearing optic), the FOV zoom constant inside
`AbstractClientPlayer.getFieldOfViewModifier` (read per-optic instead of the spyglass's hardcoded
`0.1f`), and the overlay texture read inside the HUD's spyglass-overlay draw (swapped per-optic).
All three exist only because `isScoping()` is an exact `Items.SPYGLASS` identity check that never
inspects the use-animation (research `smithing-and-item-model-layers-26-2.md` §C.1–C.2): reporting
a spyglass-like use animation from a custom weapon reproduces the arm pose but none of the zoom,
the overlay, or the held-item suppression, since all three gate on that one method. **No
server-side mixin exists anywhere in this mod** — both recipe front ends and the villager-trade
extension are found through ordinary registry mechanics (`ARCH-DEC-002`–`ARCH-DEC-005` below).
**Cost if wrong:** a Minecraft client-rendering change moving `isScoping()`'s call sites is the one
upstream this mod is exposed to that none of the four siblings are.

## `ARCH-DEC-002` — the smithing path: a custom `SmithingRecipe`, zero mixin

A Java class implementing `SmithingRecipe` (an interface, not an abstract class) inherits
`getType() == RecipeType.SMITHING` as a default method, returning the same live singleton every
vanilla smithing recipe reports. Since `RecipeMap` buckets recipes by that live `getType()` object,
not by the JSON `"type"` string, this mod's own recipe lands in the same bucket as every vanilla
smithing recipe and is found by `SmithingMenu.createResult()` with zero mixin — the identical
mechanism `create_synthetic_diamonds` confirmed for `create:pressing`
(`create-fly-pressing-recipes-26-2.md`, cited by `smithing-and-item-model-layers-26-2.md` §A.3).
The smithing table's own slot-filter/highlight system recognizes any `SmithingRecipe` implementor
via a bare `instanceof` check, independent of `getType()`, so this mod's recipe is also
slot-highlighted correctly for free.

`assemble()` follows the pattern vanilla's own `SmithingTrimRecipe` already demonstrates: read one
component off one input slot (`addition`, the attachment), write it onto a copy of another (`base`,
the weapon) — the shared attach function (`domains/attach.md` `ATTACH-DEC-001`) is exactly this,
called from both recipe front ends. There is no reverse direction: neither recipe front end ever
clears a slot component once set (`decisions/DEC-017-no-detach-durability.md`).

**Rejected alternative: a dedicated workstation block.** Kevin's own opening asked for the argument
against it; accepted, because the smithing table already does everything a bespoke workstation
would, at zero new-block cost (`decisions/DEC-006-no-new-block.md`).

## `ARCH-DEC-003` — the deploying path: a custom `Recipe<ItemApplicationInput>`, zero mixin

`DeployerBlockEntity.getRecipe()` streams `AllRecipeTypes.DEPLOYER_RECIPES` (exactly `DEPLOYING`
and the by-hand `ITEM_APPLICATION`) through `RecipeMap.getRecipesFor`, the same live-`getType()`
bucketing as pressing and smithing. A custom class implementing `Recipe<ItemApplicationInput>`
(directly, or via the public `ItemApplicationRecipe` interface for its free `matches()` default)
whose `getType()` returns the literal `AllRecipeTypes.DEPLOYING` field is found by a deployer, in
both belt and world/depot mode, with zero mixin (research
`create-fly-potato-cannon-and-deploying-26-2.md` §B.3). **Create Fly's own default `assemble()`
for this recipe type never merges either input stack's live components** — it only rolls
`ProcessingOutput`s from the recipe's own fixed JSON `components` patch — so a deploying recipe
that actually attaches an item's identity onto another's components needs this mod's own
`assemble()`, calling the identical shared attach function `ARCH-DEC-002` uses.

**Target/ingredient order**: `ItemApplicationInput` stores `target` (the weapon, sitting on the
belt or depot) at index 0 and `ingredient` (the deployer's held attachment) at index 1; the
belt-deployer callback always shrinks the target by one and replaces it with the result. This
mod's deploying recipe always consumes the held attachment (`keepHeldItem()` false); there is no
reverse recipe for a deployer to run against an already-equipped slot.

## `ARCH-DEC-004` — bullets are projectile entities, against the research's own recommendation

The research pass recommended an instant hitscan raycast (`Level.clip`/`ProjectileUtil
.getEntityHitResult`, no spawned entity) for anything firing faster than a few shots per second,
reserving a real entity for slow ordnance only (`create-fly-potato-cannon-and-deploying-26-2.md`
§A.3 verdict). **Kevin overruled this**: bullets are real, gravity-affected projectile entities
with visible travel time, "a fast entity per shot." The entity still uses the same tunneling-safe
mechanism the research verified for the potato cannon and for vanilla arrows —
`ProjectileUtil.getHitResultOnMoveVector`, a swept-segment trace over the entire tick's movement
regardless of speed, not a point sample — so the chosen architecture carries none of the
correctness risk the research's warning was actually about; it accepts only the performance and
network-sync cost of a real entity per shot, at rifle fire rates, which the research flagged but
did not forbid (`domains/combat.md` `COMBAT-DEC-001`). **Cost if wrong:** if automatic-weapon entity
counts prove too expensive in practice, the fallback the research already worked out — an instant
hitscan with no spawned entity — is a same-shape replacement for the bullet's flight step alone;
nothing else in this mod's design depends on the entity existing.

## `ARCH-DEC-005` — per-slot data components, nothing baked, nothing else persisted

Eight component types: `firearms:base` (`{ version, weapon_id }`), `firearms:ammo` (`{ caliber,
loaded }`), and one `firearms:attachment_<slot>` per slot (muzzle, optic, magazine, grip, stock),
holding an attachment id or absent. Registered the same `BuiltInRegistries.DATA_COMPONENT_TYPE`
way `create_metered_motor`'s own `MeteredMotor.STATS` already is (research
`create-fly-potato-cannon-and-deploying-26-2.md` §C.6) — no mixin needed for registration itself.

**Split per slot, not one combined map, for two independent engine reasons** (research
`smithing-and-item-model-layers-26-2.md` §B.2, §D.3): (1) the zero-mixin item-model path
(`minecraft:select`/`minecraft:condition` on `minecraft:component`/`minecraft:has_component`) reads
one component's raw value or presence per layer — a combined map cannot drive "show the muzzle
layer if a muzzle attachment is present" without a derived value, which would force the same
accessor-mixin fallback `create_metered_motor`'s MM-15 needed for its own *derived* tier property;
(2) a villager `wants` predicate (`DataComponentExactPredicate`) matches a named component's value
as a whole, with no partial/sub-path match inside it — only a per-slot shape lets a master buy-back
trade name just the slot(s) it cares about and leave the rest free (`domains/trade.md`
`TRADE-REQ-005`).

**Nothing is baked**: final stats are never written to the item; every read (firing, tooltip,
trade display) re-runs the stat derivation function over whichever components are present at that
moment (`domains/weapon.md` `WEAPON-REQ-003`). Filling a further, still-empty slot later is
therefore free of any recomputation step beyond the function itself running again — the direct
mechanism behind Kevin's "easy upgrade paths," now that a filled slot itself is permanent
(`decisions/DEC-017-no-detach-durability.md`).

## `ARCH-DEC-006` — item models: composite layers, zero mixin for direct-valued slot components

A base weapon's item model is `minecraft:composite` over a base layer plus one
`minecraft:condition` layer per slot, each gated on the built-in `minecraft:has_component` property
against that slot's own `firearms:attachment_<slot>` type — zero mixin, since both `minecraft
:component`/`minecraft:has_component` read any normally-registered `DataComponentType`'s raw value
or presence directly (research `smithing-and-item-model-layers-26-2.md` §B.2, §B.3). This only
holds because each slot is its own directly-valued component (`ARCH-DEC-005`); a combined map or
any derived branch key would need the same accessor-mixin fallback `create_metered_motor`'s MM-15
already used once. One model file per base weapon, not one per base×attachment combination
(`domains/weapon.md` `WEAPON-REQ-013`).

## Runtime topology (sheet §3.1)

Combat resolution, recipe assembly, stat derivation and trade checks are entirely server-side. The
three scope mixins, item-model rendering, tooltip text and the cosmetic recoil/tracer/sound layer
are client-side only and carry no authority over any hit, damage amount, or ammo count
(`domains/combat.md` `COMBAT-DEC-003`). No process, daemon or file of this mod's own exists outside
the Minecraft client and server processes.

## Failure modes with no single owner (sheet §3.6)

| ID | Failure | Response |
|---|---|---|
| `ARCH-FAIL-001` | Create Fly missing or an incompatible version | Fabric Loader refuses to start with its dependency message; the mod adds nothing. |
| `ARCH-FAIL-002` | `RecipeManager`/`RecipeMap`'s live-`getType()` bucketing does not behave as the research's disassembly shows for `SMITHING` or `DEPLOYING`, at the first ticket | Falls back to a narrowly-scoped mixin on this mod's own recipe lookup only, never on the shared smithing or deploying machinery every other recipe of that type relies on. |
| `ARCH-FAIL-003` | Another mod also registers a `SmithingRecipe` or a `DEPLOYING`-typed recipe matching the same input pair | Both are candidates in the same bucket; ordinary vanilla recipe-conflict resolution applies, no special handling by this mod. |
| `ARCH-FAIL-004` | Automatic-weapon fire rates produce more bullet entities per second than the server comfortably ticks and syncs | Accepted risk of `ARCH-DEC-004`; the documented fallback is replacing the flight step with the research's own hitscan design, not a redesign of hit detection. |
| `ARCH-FAIL-005` | A future Minecraft client-rendering change moves `Player.isScoping()`'s call sites or signature | Breaks the build at compile time for the mixin target, the same bounded exposure every add-on already accepts for its own mixins (`create_villager_customers` `PLATFORM-REQ-002`). |
