---
title: "create_firearms spec — actors: who touches a weapon and what each may do"
type: "spec"
category: "create_firearms"
---

# 01 — Actors

| ID | Actor | May | May not |
|---|---|---|---|
| `ACTORS-001` | **Player** | Craft bases, attachments and ammunition at a crafting table; attach an attachment into any of a weapon's still-empty slots at a smithing table, at any time; load ammunition by caliber; aim, fire and reload; buy from the weaponsmith and fletcher; sell a fully-configured weapon to a master buy-back trade | Fire without the server confirming ammo, cooldown and fire mode (`domains/combat.md`); bypass the `pvp` gamerule against another player (`domains/combat.md` `COMBAT-REQ-010`); attach into an already-occupied slot, or detach, repair, or enchant a weapon by any means: none of those exist (`domains/attach.md` `ATTACH-REQ-002`; `decisions/DEC-017-no-detach-durability.md`) |
| `ACTORS-002` | **Server** | Resolve every smithing and deploying recipe; derive final stats from a weapon's components; spawn and tick bullet entities; apply damage, knockback and PvP gating; run every villager trade check | Trust a client with any hit result, damage amount, or ammo count: every one of those is computed and applied server-side only (`04-architecture.md` `ARCH-DEC-005`) |
| `ACTORS-003` | **Deployer** (a Create Fly block, driven by a contraption) | Apply an attachment held in its hand to a weapon sitting on a belt or a depot, via the deploying recipe, into any still-empty slot (`domains/attach.md` `ATTACH-REQ-005`, `006`) | Fire a weapon, load ammunition, or act as a combat participant of any kind: combat scope is players and mobs only (`00-context.md`); remove an attachment by any means: there is no reverse deploying recipe (`decisions/DEC-017-no-detach-durability.md`) |
| `ACTORS-004` | **Weaponsmith villager** | Sell weapon bases and attachments at its existing trade levels; at its top level, buy back one fully-configured weapon per trade for many emeralds (`domains/trade.md` `TRADE-REQ-004`) | Gain a new profession, job site, or POI of its own: it is the existing vanilla weaponsmith, unchanged apart from its trade pool (`decisions/DEC-012-villager-integration.md`) |
| `ACTORS-005` | **Fletcher villager** | Sell ammunition at its existing trade levels | Sell a weapon or an attachment: those stay on the weaponsmith (`domains/trade.md` `TRADE-DEC-001`) |
| `ACTORS-006` | **Target (mob or player)** | Take damage from a bullet under ordinary vanilla `DamageSource`/`LivingEntity.hurt` rules, exactly as from an arrow | Be bled, limb-hit, staggered, or penetrated: none of those exist at 1.0 (`domains/combat.md` `COMBAT-DEC-001`) |
| `ACTORS-007` | **Create Fly** (dependency) | Supply the deployer block, the `create:deploying`/`AllRecipeTypes.DEPLOYING` recipe machinery this mod's own recipe class reports itself under, and the brass material items (`create:brass_sheet`) a cartridge casing crafts from | Be modified: this mod adds recipe classes, components and JSON data only, and touches nothing of Create Fly's own code |
| `ACTORS-008` | **Datapack or resource pack author** | Add a new weapon (a base data file naming an existing class) or a new attachment (a data file naming a slot and its stat modifiers) with zero new Java, once 1.0's slot and component shape exists (`domains/weapon.md` `WEAPON-DEC-003`) | Change the attach function, the stat derivation function, or the per-slot component shape: those are code (`04-architecture.md`) |
| `ACTORS-009` | **`create_villager_customers`** (sibling mod, not a dependency of this one) | Nothing yet: its own match rule (`StackShape`) does not honour component predicates, so a shop cannot distinguish two differently-attached weapons of the same base item | Be changed by this mod: the required extension is recorded as a cross-project requirement on that mod, out of this mod's own scope (`decisions/DEC-016-villager-customers-requirement.md`) |
| `ACTORS-010` | **Contributor** | Build, test and change the mod under MIT | Add telemetry or network calls (`operations/compliance.md`) |

## Findings from writing this

- **`FINDING-1`** Two actors carry the attach mechanism (`ACTORS-001` at a smithing table,
  `ACTORS-003` through a deployer) against one shared rule, mirroring
  `create_villager_customers`'s `DEC-005` "both directions are one act": the front end differs, the
  merge function underneath does not (`domains/attach.md` `ATTACH-DEC-001`).
- **`FINDING-2`** No actor here ever opens a screen this mod draws for configuration — the smithing
  table's own screen, the deployer, and the two villagers' own trade screens are every interaction
  surface this mod uses (`domains/ui.md`), the same "no screen of its own" shape
  `create_villager_customers` and `create_synthetic_diamonds` both landed on.
- **`FINDING-3`** `ACTORS-009` is listed even though it is not a dependency, because Kevin's own
  framing ("ties in with metered motor and villager customers") makes the master buy-back trade's
  full value depend on a change this mod cannot make itself. Recording it here, rather than
  silently assuming it will happen, is the point of `decisions/DEC-016-villager-customers-requirement.md`.
- **`FINDING-4`** `ACTORS-006` (the target) is the only actor that never touches this mod's own
  code path directly — it is hit by a bullet the same way it is hit by an arrow, which is exactly
  why `domains/combat.md` `COMBAT-DEC-001` treats vanilla damage rules as a scope boundary, not a
  placeholder for something richer later.
