---
title: "create_firearms spec — journeys: the use cases end to end"
type: "spec"
category: "create_firearms"
---

# 02 — Journeys

Every step names who acts. `UC` ids are flat across the project; domain files reference them.

### `UC-001` — Crafting a base weapon and an attachment

Actor: player (`ACTORS-001`) · Goal: get an M1911 and a suppressor onto the crafting table

| Step | Actor | Action |
|---|---|---|
| 1 | player | Gathers the M1911's crafting materials and crafts it at a plain crafting table (`domains/weapon.md` `WEAPON-REQ-006`). |
| 2 | player | Separately crafts a suppressor at a plain crafting table (`domains/attach.md` `ATTACH-REQ-007`). |
| 3 | player | Now holds two items: a base weapon carrying only `firearms:base`, and an attachment item carrying no components of its own beyond its identity. |

### `UC-002` — Attaching at the smithing table (also the upgrade path)

Actor: player · Goal: put the suppressor onto the M1911, now or at any later time

| Step | Actor | Action |
|---|---|---|
| 1 | player | Opens a vanilla smithing table; places the M1911 in the base slot and the suppressor in the addition slot; the template slot stays empty (`domains/attach.md` `ATTACH-REQ-001`). |
| 2 | server | Matches this mod's custom smithing recipe (`Recipe<SmithingRecipeInput>`, `getType() == RecipeType.SMITHING`) against the pair, with zero mixin (`04-architecture.md` `ARCH-DEC-002`). |
| 3 | server | Assembles the result: a copy of the base weapon with `firearms:attachment_muzzle` set to the suppressor's id, every other component untouched (`domains/attach.md` `ATTACH-REQ-003`). |
| 4 | player | Takes the result; the suppressor item is consumed, the same way a template or addition is consumed by any vanilla smithing recipe. |

This is the whole attach mechanism, and — read this way for Kevin to confirm or correct
(`domains/attach.md` §1) — also the entire upgrade path: a player may run this same step again
later against any of the weapon's *other*, still-empty slots. The muzzle slot filled here never
changes again once set (`decisions/DEC-017-no-detach-durability.md`); there is no step that swaps
or clears it (withdrawn: `UC-003`, `UC-004`).

### `UC-005` — A deployer attaches an attachment (the contraption path)

Actor: deployer (`ACTORS-003`) · Goal: apply a scope to a weapon riding a contraption belt

| Step | Actor | Action |
|---|---|---|
| 1 | player | Builds a contraption with a deployer holding a 4x scope, positioned over a belt carrying M1911s (a class with an optic slot; the M1911 itself has no optic slot, so this journey assumes an AKM or similar). |
| 2 | server | The deployer's `getRecipe()` finds this mod's custom `Recipe<ItemApplicationInput>` reporting `getType() == AllRecipeTypes.DEPLOYING`, with zero mixin (`04-architecture.md` `ARCH-DEC-003`; research `create-fly-potato-cannon-and-deploying-26-2.md` §B.3). |
| 3 | server | Assembles the result exactly as `UC-002`'s smithing recipe does, through the same shared attach function (`domains/attach.md` `ATTACH-DEC-001`); the belt-deployer callback shrinks the target weapon by one and replaces it with the result. |
| 4 | server | The deployer's held scope is consumed (`keep_held_item` false), mirroring a consumable deploying recipe such as planks-to-cogwheel. |

`UC-006` is withdrawn: it described a deployer detaching an attachment, which no longer exists
(Kevin, 2026-09-20: "they don't come off"; `decisions/DEC-017-no-detach-durability.md`).

### `UC-007` — Firing a semi-automatic weapon

Actor: player · Goal: fire one round from a loaded M1911 at a mob

| Step | Actor | Action |
|---|---|---|
| 1 | player | Holds the loaded weapon and presses the fire control once. |
| 2 | server | Checks the weapon's cooldown group is clear and `firearms:ammo.loaded > 0` (`domains/weapon.md` `WEAPON-REQ-008`). |
| 3 | server | Derives final stats from `firearms:base` plus every present slot component (`domains/weapon.md` `WEAPON-REQ-003`); rolls spread within the derived cone; spawns one bullet entity along the resulting vector (`domains/combat.md` `COMBAT-REQ-001`). |
| 4 | server | Decrements `firearms:ammo.loaded` by one, reduces the weapon's durability by one shot, and starts the fire-rate cooldown (`domains/weapon.md` `WEAPON-REQ-009`, `010`). |
| 5 | server | The bullet travels under gravity, is hit-tested every tick along its full movement vector, and on a hit applies vanilla damage rules (`domains/combat.md` `COMBAT-REQ-002`–`004`). |
| 6 | client | Plays the muzzle flash, sound, tracer, and a cosmetic recoil camera kick, decoupled from the already server-authoritative shot (`domains/combat.md` `COMBAT-DEC-003`). |

### `UC-008` — Firing an automatic weapon

Actor: player · Goal: hold the trigger on a loaded AKM

| Step | Actor | Action |
|---|---|---|
| 1 | player | Holds the fire control down. |
| 2 | server | Repeats `UC-007` steps 2–6 once per the weapon's fire-rate interval for as long as the control stays held and ammo remains (`domains/weapon.md` `WEAPON-REQ-011`, fire mode `auto`). |
| 3 | server | Stops the loop the instant `firearms:ammo.loaded` reaches zero, control is released, or the weapon leaves the player's hand. |
| 4 | player | Hears the empty-click sound rather than a shot once ammo runs out mid-hold (`domains/weapon.md` `WEAPON-FAIL-002`). |

### `UC-009` — Firing a pump-action shotgun

Actor: player · Goal: fire the Winchester Model 1897 at close range

| Step | Actor | Action |
|---|---|---|
| 1 | player | Fires once; fire mode `pump` behaves as `semi` for cooldown purposes but the assemble step spawns several pellet bullet entities instead of one, each independently spread within a wider cone (`domains/combat.md` `COMBAT-REQ-005`). |
| 2 | server | Each pellet is hit-tested and damage-applied independently; a target within the cone's densest part can be hit by several pellets from one trigger pull. |

### `UC-010` — Reloading

Actor: player · Goal: refill a partially-spent magazine

| Step | Actor | Action |
|---|---|---|
| 1 | player | Triggers reload (empty click, or a manual reload control) while holding the weapon. |
| 2 | server | Scans the player's inventory for a cartridge item matching the weapon's caliber (`domains/ammo.md` `AMMO-REQ-004`), the same inventory-scan shape the potato cannon's ammo lookup uses (research §A.1). |
| 3 | server | Consumes cartridges up to the derived magazine size, sets `firearms:ammo.loaded` accordingly, and starts the reload-duration cooldown (`domains/weapon.md` `WEAPON-REQ-012`). |
| 4 | server | Where no matching cartridge exists anywhere in the inventory, the reload fails silently; the weapon keeps whatever ammo it had (`domains/weapon.md` `WEAPON-FAIL-003`). |

### `UC-011` — Aiming down a magnified scope

Actor: player · Goal: use a 4x-scoped Ruger Mini-14 like the vanilla spyglass

| Step | Actor | Action |
|---|---|---|
| 1 | player | Holds the aim control (the same press-and-hold shape as the vanilla spyglass). |
| 2 | client | The widened `Player.isScoping()` mixin returns true for this weapon, since its optic component names a zoom-bearing scope and the player is using the item (`domains/combat.md` `COMBAT-REQ-006`; research `smithing-and-item-model-layers-26-2.md` §C.1). |
| 3 | client | The FOV mixin reads the attached optic's zoom factor (4.0) instead of the vanilla spyglass's hardcoded `0.1f`; the overlay mixin draws this optic's own reticle texture instead of the spyglass's (`domains/combat.md` `COMBAT-REQ-007`, `008`). |
| 4 | server | While aiming, the derived spread cone is narrowed by the optic's own spread modifier (`domains/weapon.md` `WEAPON-REQ-005`). |
| 5 | player | Releases the control; the view, overlay and held-item render return to normal, exactly as releasing a spyglass does. |

### `UC-012` — Aiming with a red dot or holo sight

Actor: player · Goal: use a non-magnifying optic

| Step | Actor | Action |
|---|---|---|
| 1 | player | Holds the aim control on a weapon with a red dot or holo attached. |
| 2 | client | The widened `isScoping()` does **not** trigger for these two optics: no FOV change, no overlay, no held-item suppression (`domains/combat.md` `COMBAT-REQ-006`, "zoom factor ≤ 1"). |
| 3 | server | The derived spread cone still narrows by the optic's own modifier, exactly as `UC-011` step 4 — the aiming benefit is spread and aim assist only, never zoom (`decisions/DEC-009-scope-mechanic.md`). |

### `UC-013` — A bullet misses and despawns

Actor: server · Goal: clean up a shot that never lands

| Step | Actor | Action |
|---|---|---|
| 1 | server | A bullet entity travels its full life (`domains/combat.md` `COMBAT-REQ-003`) without an entity or block hit resolving its flight early. |
| 2 | server | At its despawn age, the entity is removed with no damage applied and no drop. |

### `UC-014` — Crafting a cartridge

Actor: player · Goal: make 7.62mm ammunition for an AKM

| Step | Actor | Action |
|---|---|---|
| 1 | player | Gathers a brass sheet, gunpowder, and the calibre's projectile material at a plain crafting table (`domains/ammo.md` `AMMO-REQ-001`). |
| 2 | player | Crafts a batch of cartridges (`domains/ammo.md` §3, stack sizes). |

### `UC-015` — Buying a weapon or attachment from the weaponsmith

Actor: player · Goal: buy a compensator instead of crafting one

| Step | Actor | Action |
|---|---|---|
| 1 | player | Trades emeralds for the compensator at the weaponsmith's tagged trade level (`domains/trade.md` `TRADE-REQ-001`). |
| 2 | player | Receives a plain, unattached attachment item, usable exactly as a crafted one. |

### `UC-016` — Buying ammunition from the fletcher

Actor: player · Goal: buy 9mm cartridges

| Step | Actor | Action |
|---|---|---|
| 1 | player | Trades emeralds (or the fletcher's usual cost item) for a stack of 9mm cartridges (`domains/trade.md` `TRADE-REQ-002`). |

### `UC-017` — The master buy-back trade

Actor: weaponsmith villager (`ACTORS-004`) · Goal: buy one fully-configured AKM for many emeralds

| Step | Actor | Action |
|---|---|---|
| 1 | player | Reaches the weaponsmith's top trade level, having sold or worked toward it as with any villager. |
| 2 | player | Offers an AKM carrying `firearms:attachment_muzzle = suppressor` and `firearms:attachment_optic = scope_4x`, any other slot free (`domains/trade.md` `TRADE-REQ-005`). |
| 3 | server | `ItemCost.test()` checks item identity plus the named components only, via `DataComponentExactPredicate`; unconstrained slots (magazine, grip, stock) are never inspected (research `smithing-and-item-model-layers-26-2.md` §D.3). |
| 4 | player | Receives the trade's emerald payout; the weapon is gone, exactly as any sold item is. |

**Not yet closed by this mod alone**: pairing this trade with `create_villager_customers` so a
villager walks the emeralds home from a table-cloth shop needs that sibling's match rule extended
to honour component predicates — `decisions/DEC-016-villager-customers-requirement.md`.

### `UC-018` — Reading a weapon's tooltip

Actor: player · Goal: understand what a weapon in hand actually does

| Step | Actor | Action |
|---|---|---|
| 1 | player | Hovers the weapon in an inventory or hotbar slot. |
| 2 | client | The tooltip lists the derived final stats and names every attached slot's occupant, computed the same way `domains/weapon.md`'s stat function computes them for firing (`domains/ui.md` `UI-REQ-001`). |

### `UC-019` — A worn weapon is refused at the anvil and the enchanting table

Actor: player · Goal: try to repair or enchant a worn M1911 the way any vanilla tool would allow

| Step | Actor | Action |
|---|---|---|
| 1 | player | Places a damaged M1911 in the enchanting table; the table offers no enchantments for it, since it carries no `ENCHANTABLE` component (`domains/weapon.md` `WEAPON-REQ-015`). |
| 2 | player | Places the same M1911 in an anvil alongside a repair material (e.g. iron ingots); the anvil produces no result, since `ItemStack.isValidRepairItem` reads the absent `REPAIRABLE` component and returns false (`domains/weapon.md` `WEAPON-REQ-014`). |
| 3 | player | Places the same M1911 in an anvil alongside an enchanted book; no enchantment transfers, since none of vanilla's enchantments list this weapon among their supported items, so `Enchantment.canEnchant` fails for all of them and the anvil's result stays empty (`domains/weapon.md` `WEAPON-REQ-015`). |
| 4 | player | Places two damaged M1911s in an anvil together; **this partially repairs the first from the second** under vanilla's own same-item combine-repair behaviour, which does not check `REPAIRABLE` at all — a residual gap this sheet names rather than silently closes (`domains/weapon.md` §7). |
| 5 | player | Concludes the only way a weapon regains durability is a fresh craft; a broken weapon is retired, exactly like a broken vanilla tool with no repair material defined. |
