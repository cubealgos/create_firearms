---
title: "create_firearms spec — context: why, for whom, and what it will not do"
type: "spec"
category: "create_firearms"
---

# 00 — Context

## Why this exists

Kevin's idea, verbatim: "I actually want to build a weapons style mod now: it should be possible
to use it with Create contraptions (deployers applying attachments to bases), but it should also
be able to be used by the player; that workstation should also add another villager profession,
the gunsmith; I want the weapon mod to be somewhat realistic and stats to be dynamic; the
workstation should be used to build unique weapon combinations from bases and attachments; all
bullets + attachments + bases should have normal crafting recipes; I'd also let you argue against
the new workstation ... + trades could be added to existing villagers; we want easy upgrade paths,
as little new blocks as possible." (`rulings-2026-09-20.md`)

The argument against the workstation and the gunsmith profession was made and accepted before this
sheet was written: the vanilla smithing table already does everything a dedicated workstation
would (`04-architecture.md` `ARCH-DEC-002`), and a new profession needs a new job-site block and
POI registration — the exact new-block cost the workstation decision was written to avoid
(`domains/trade.md` `TRADE-DEC-001`). What survives is a mod built entirely from recipes,
components and one villager-trade extension, on top of two vanilla/Create systems that already
exist: the smithing table and the deploying recipe.

It is the fifth Create Fly add-on built this way. Unlike its four siblings, it is not primarily a
resource-loop or automation piece: it is `create_civilization`'s answer to combat and defense,
scoped narrowly to vanilla damage rules so it stays a weapon mod, not a wound-simulation mod. Like
every sibling, it ships to real users on Modrinth and is specified as a distributed product
(`decisions/DEC-001-classification.md`).

## Who it is for

- Players on Minecraft 26.2 with Fabric and Create Fly who want a modern firearm, built up from a
  base and attachments, usable in survival combat under ordinary vanilla damage and PvP rules.
- Players who also run Create contraptions and want a deployer to assemble or reconfigure a weapon
  automatically, the same way a deployer already scrapes wax off copper.
- Server operators who install it alongside Create Fly and expect nothing to configure beyond the
  data files (`contracts/public-surface.md`).
- Modpack and datapack authors who want to add weapons or attachments to the roster as data files,
  with zero new Java, once the 1.0 roster's slot and component shape exists
  (`domains/weapon.md` `WEAPON-DEC-003`).
- Kevin, chasing "easy upgrade paths, as little new blocks as possible" and villager-driven
  late-game emerald automation tied to `create_metered_motor` and `create_villager_customers`.

## Business context

No business model, no revenue, no telemetry. Published on Modrinth under MIT, source on the
cubealgos Forgejo with a GitHub mirror and tracker, public from the first commit
(`decisions/DEC-003-licence.md`) — the same place all four siblings ended up.

## What it will not do

- No new block, anywhere, for any purpose: no workstation, no job-site, no dedicated storage
  (`decisions/DEC-006-no-new-block.md`). The vanilla smithing table and Create's deployer carry the
  entire attach mechanic.
- No detaching, no repair, no enchanting: an attachment, once on, is permanent; a weapon loses
  durability like any other vanilla tool and simply wears out (Kevin, 2026-09-20,
  `decisions/DEC-017-no-detach-durability.md`).
- No new villager profession: weapons and attachments sell through the existing weaponsmith,
  ammunition through the existing fletcher (`domains/trade.md`).
- No bleeding, no limb hits, no penetration, no stagger, no armor-piercing mechanic of its own: all
  combat resolves through vanilla `DamageSource`/`LivingEntity.hurt` exactly as an arrow does
  (`domains/combat.md` `COMBAT-DEC-001`).
- No recipe-viewer integration at 1.0 (JEI or otherwise), deferred like `create_synthetic_diamonds`
  chose for its own recipes (`domains/ui.md`).
- No Create automation path for ammunition at 1.0: cartridges craft only at a crafting table;
  press/mixer automation is a later ticket (`domains/ammo.md` `AMMO-REQ-005`).
- No PUBG name, branding, logo, splash art, or texture anywhere in the mod, its listing, or its
  repository: every weapon uses only its real-world designation
  (`operations/compliance.md` `COMP-REQ-002`).
- No suppressor noise-radius mechanic at 1.0: no vanilla hook was confirmed for it in the research
  pass (`domains/combat.md` §7).
- No extension of `create_villager_customers` itself: that sibling's match rule needs teaching to
  honour component predicates before the master buy-back trade's full automation loop closes, and
  that change is out of this mod's scope (`decisions/DEC-016-villager-customers-requirement.md`).

## Success

Kevin crafts an AKM and a 4x scope, walks to a smithing table, comes out with one weapon that
zooms when aimed; a deployer on a belt strips the scope back off the same weapon without touching
a menu; a weaponsmith sells the same scope at level 3 and, at level 5, buys a fully kitted-out AKM
back for a stack of emeralds. Nothing about the smithing table, the deployer, or the two villagers
looks any different from vanilla or Create Fly, other than what they now trade in.
