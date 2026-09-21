# Modrinth listing (paste-ready)

## Project settings

| Field | Value |
|---|---|
| Name | Create: Firearms |
| Slug | `firearms` |
| Summary | A roster of modern firearms under real-world designations, built from a base weapon plus attachments at the smithing table or by deployers, with dynamic stats, traded by weaponsmiths and fletchers. |
| Categories | Equipment, Technology, Utility (secondary: Adventure, Game-Mechanics, Management, Optimization) |
| Licence | MIT |
| Client side | Required |
| Server side | Required |
| Loaders | Fabric |
| Game versions | 26.2 |
| Dependencies | Create Fly (required), Fabric API (required) |
| Icon | `icon.png` in this folder: a real 3D render of the AKM, this mod's own weapon item model (`akm.json` + its own 32x32 texture atlas), tilted to the model's own `gui` display transform, on the cubealgos navy grid badge (`just icon` regenerates it; FA-27, Kevin's 2026-09-21 ruling, replaces the earlier 16x16 pixel-art cartridge placeholder from FA-15) |
| Links | Source `https://github.com/cubealgos/create_firearms` · Issues `https://github.com/cubealgos/create_firearms/issues` · Origin `https://git.cubealgos.de/cubealgos/create_firearms` |

## Version settings

| Field | Value |
|---|---|
| Version number | `1.0.0+26.2` |
| Version title | Firearms 1.0.0 for Minecraft 26.2 |
| Channel | Release |
| File | `dist/create_firearms-1.0.0+26.2.jar` |
| Changelog | paste `dist/release-notes-1.0.0+26.2.md` |

## Body

Six modern firearms, each built up from a bare base weapon and a set of attachments you choose,
assembled by hand at a smithing table or automatically by a Create deployer. Every stat you see —
damage, spread, recoil, reload speed, magazine size — is derived live from whatever is actually
attached, never baked into the item. Nothing about the smithing table or the deployer looks any
different from vanilla or Create Fly; they simply now build something new.

### What it does

- **Six base weapons, one per class.** M1911 (pistol), Micro Uzi (SMG), AKM (assault rifle), Ruger
  Mini-14 (DMR), AWM (sniper rifle), and Winchester Model 1897 (shotgun) — each its own calibre,
  each with a plain crafting-table recipe.
- **22 attachments across five slots.** Muzzle, optic, magazine, grip and stock, gated by class (a
  shotgun only takes a muzzle and a magazine; a rifle's muzzle attachment still fits a shotgun,
  since fit is by slot, not by weapon). Every attachment has its own plain crafting-table recipe.
- **Two ways to build a weapon.** Combine a weapon and an attachment at a smithing table by hand,
  or let a Create deployer do the identical thing on a belt or contraption — both call the same
  attach logic, so a weapon built by a deployer is no different from one built by hand.
- **Attachments are permanent.** Once a slot is filled it stays filled for the life of the weapon;
  there is no detach recipe. A weapon's other, still-empty slots stay fillable at any later time.
- **Dynamic stats.** A weapon's damage, muzzle velocity, spread, fire rate, recoil, magazine size
  and reload time are all derived fresh from its base weapon plus whatever is currently attached,
  every time they're needed — never written back onto the item.
- **Bullets are real projectiles.** Every shot spawns a bullet entity with real gravity drop and
  travel time, hit-tested every tick the same way a vanilla arrow is, so a shot connects or misses
  by the same mechanism the game already uses for one. The shotgun fires 8 independently-spread
  pellets per trigger pull from a single shell.
- **Scopes zoom like the vanilla spyglass.** Attach a 2x through 15x optic and aiming down it
  actually narrows your field of view and swaps in that optic's own overlay, the same widened
  mechanism the vanilla spyglass already uses — a red dot or holo sight tightens your aim without
  ever zooming.
- **Sold by the weaponsmith and the fletcher, no new villager profession.** Weapons and attachments
  join the existing weaponsmith's trade levels; cartridges join the existing fletcher's. At the
  weaponsmith's top level, six master trades buy back one specific, fully-kitted configuration per
  weapon class — a suppressed AWM with an 8x scope, for instance — for a stack-sized emerald
  payout, while every other slot on the traded weapon stays unconstrained.
- **Ordinary durability, no repair, no enchanting.** A weapon wears out like any other vanilla tool
  as it fires and simply breaks when it runs out — it can't be repaired at an anvil and can't be
  enchanted, matching the rest of its "crafted and done" permanence.

### Setting it up

Craft a base weapon and an attachment at a crafting table, then combine them at a smithing table
exactly as you would a netherite upgrade — the weapon goes in the base slot, the attachment in the
addition slot, the template slot stays empty. Repeat for each open slot the weapon's class has.
Prefer automation? A deployer holding an attachment does the identical combine against a weapon
sitting on a belt or a depot, no menu involved. Load a weapon by holding the matching cartridge in
your inventory and reloading; fire it like any other held item, aim down a zoom-capable optic to
scope in.

### Made for Create

No new block, anywhere — no workstation, no job-site, no dedicated storage. The vanilla smithing
table and Create's own deployer carry the entire attach mechanic, and the deployer applies an
attachment exactly the way it already scrapes wax off copper: one ingredient, one target, one
result.

### Privacy

Nothing leaves your machine. No telemetry, no update checks, no network call of any kind — combat
resolution, recipe assembly, stat derivation and every trade check run entirely server-side.

### Requirements

Minecraft 26.2, Fabric, Fabric API, and Create Fly 6.0.9-1 (the build this version was tested
with; the mod declares exactly that version).

### Support

Through the issue tracker only (https://github.com/cubealgos/create_firearms/issues), as time
allows. Source on GitHub, mirrored from the cubealgos Forgejo. Include your Minecraft, Fabric and
Create Fly versions, the mod version from the jar name, and the steps that show the problem. MIT
licensed.

### A note on the roster

Every weapon here uses only its real-world firearm designation (M1911, Micro Uzi, AKM, Ruger
Mini-14, AWM, Winchester Model 1897) — these are firearm model names. Every texture, model and
sound in this mod is drawn and recorded fresh for it; none of it is copied from any other game.
Every stat and price in this listing is a proposed starting point, balance-swept before 1.0, not a
promise that these exact numbers ship unchanged in a future update.
