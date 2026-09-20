---
title: "create_firearms spec — AMMO: the six cartridges"
type: "spec"
category: "create_firearms"
---

# `AMMO` — cartridges, their recipes, and loading

## 1. Purpose

The six calibre-matched cartridge items, their crafting recipes and materials, stack sizes, and how
a weapon draws them from inventory on reload. Not the weapon doing the loading
(`domains/weapon.md` `WEAPON-REQ-010`) and not what a fired round does once it leaves the barrel
(`domains/combat.md`).

## 2. Dimensions

| Dimension | Answer |
|---|---|
| **Actors** | The player (`ACTORS-001`) crafts or buys cartridges and carries them in inventory; the server (`ACTORS-002`) matches them by calibre during a reload; the fletcher villager (`ACTORS-005`) sells them. |
| **Over time** | Crafted or bought → sits in inventory → drawn into a weapon's `firearms:ammo.loaded` count on reload → consumed one at a time as the weapon fires (`domains/weapon.md` `WEAPON-REQ-008`). A cartridge item itself is never partially consumed — it either becomes one loaded round or stays a whole item in inventory. |
| **Multiplicity** | Six cartridge items at 1.0, one per calibre, no shared item across calibres. A datapack may add a cartridge for a new calibre alongside a new weapon, with zero new Java. |
| **Unwanted** | A cartridge of the wrong calibre for the weapon being reloaded (simply never matched, `AMMO-FAIL-001`); crafting materials substituted from a different mod's equivalent item (allowed only where the recipe's own ingredient tag already covers it, not a special case this mod adds). |
| **Not-you** | A player who never builds a firearm never crafts or sees a cartridge differently from any other inert crafting item. |

## 3. Enumerations

### The six cartridges (proposed, retune at the sweep)

Materials per research `smithing-and-item-model-layers-26-2.md` §E: `create:brass_sheet` (Create
Fly's own casing-shaped material, reused as-is rather than inventing a new casing item,
`AMMO-DEC-001`), `minecraft:gunpowder` (propellant), `minecraft:iron_nugget` (projectile material,
scaled by count for a calibre's own size). All recipes are shapeless crafting-table recipes,
output 4 cartridges per craft, stack size 64 (matching vanilla arrows).

| Cartridge | Calibre | Weapon | Materials (per 4 crafted) |
|---|---|---|---|
| `.45 ACP` cartridge | `.45 ACP` | M1911 | 1 brass sheet, 1 gunpowder, 1 iron nugget |
| `9mm` cartridge | `9mm` | Micro Uzi | 1 brass sheet, 1 gunpowder, 1 iron nugget |
| `7.62mm` cartridge | `7.62mm` | AKM | 1 brass sheet, 1 gunpowder, 2 iron nuggets |
| `5.56mm` cartridge | `5.56mm` | Ruger Mini-14 | 1 brass sheet, 1 gunpowder, 2 iron nuggets |
| `.300 Magnum` cartridge | `.300 Magnum` | AWM | 1 brass sheet, 1 gunpowder, 3 iron nuggets |
| `12 gauge` shell | `12 gauge` | Winchester Model 1897 | 1 brass sheet, 1 gunpowder, 4 iron nuggets (buckshot) |

Pistol/SMG calibres cost the least material; rifle calibres scale up by projectile material only,
mirroring the roster's own damage/velocity scaling without inventing a second material type.

### Deferred: Create automation path

Not built at 1.0 (`00-context.md`). The research identifies a two-step shape that needs no new
Java when it is built: `create:pressing` (brass sheet from a brass ingot, already Create Fly's own
recipe) feeding `create:mixing` (gunpowder + iron nuggets + the pressed casing, combined in one
basin step, optionally `heat_requirement: heated` for a "propellant needs heat to prime" flavour).
The plain crafting-table recipe above remains the non-automated baseline regardless of whether this
path is later added, since Create's processing recipe types and vanilla crafting recipes are
independent, non-exclusive registrations on the same output item (research §E.4).

## 4. Use cases

`UC-010`, `UC-014`, `UC-016` in `02-journeys.md`.

## 5. Requirements

| ID | Requirement | Priority | From |
|---|---|---|---|
| `AMMO-REQ-001` | The system shall provide a plain shapeless crafting-table recipe for each of the six cartridges, materials as the table above, output 4 per craft. | Must | `rulings-2026-09-20.md` |
| `AMMO-REQ-002` | Each cartridge item shall stack to 64. | Must | Enumerations |
| `AMMO-REQ-003` | Each cartridge item shall carry no data component of this mod's own: it is identified purely by its own item id, one per calibre. | Must | Simplicity; nothing downstream needs a cartridge-level component |
| `AMMO-REQ-004` | On reload, the system shall match a cartridge to a weapon only when the cartridge's calibre equals the weapon's own base calibre (`domains/weapon.md` `WEAPON-REQ-010`), scanning the player's inventory the same way the potato cannon's ammo lookup does. | Must | Research `create-fly-potato-cannon-and-deploying-26-2.md` §A.1 |
| `AMMO-REQ-005` | The system shall not provide a Create automation recipe (pressing/mixing) for cartridges at 1.0; the crafting-table recipe is the only path. | Must, scoped out | `00-context.md` |

## 6. Failure modes

| ID | Failure | Response |
|---|---|---|
| `AMMO-FAIL-001` | Wrong-calibre cartridge present during reload | Never matched; the reload behaves as if no cartridge exists, per `domains/weapon.md` `WEAPON-FAIL-003`. |
| `AMMO-FAIL-002` | Player crafts a cartridge with no weapon of that calibre yet | No error — the item is inert crafting output like any other, held until a matching weapon exists. |

## 7. Open questions

| Question | Blocks | Decided by |
|---|---|---|
| Whether `12 gauge` needs a distinct pellet-count field on the cartridge itself, or whether pellet count is purely a `domains/combat.md` firing-time constant independent of the cartridge item | `domains/combat.md` `COMBAT-REQ-005` | first ticket; this sheet proposes the latter, keeping cartridges free of any component |

## 8. Decisions

- `AMMO-DEC-001` — **The casing is `create:brass_sheet`, not a new item** (this sheet's reading of
  research §E.2's brass-chain precedent): Create Fly's own ingot→sheet pressing step already
  produces exactly the flat metal-sheet shape a cartridge case needs; inventing a
  `firearms:cartridge_casing` item would duplicate an existing material for no gameplay reason.
  **Cost if wrong:** a dedicated casing item is a straightforward substitution in the recipe table
  above, not a redesign.
