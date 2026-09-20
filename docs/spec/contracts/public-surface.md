---
title: "create_firearms spec — public surface: what a datapack, resource pack or add-on may rely on"
type: "spec"
category: "create_firearms"
---

# Public surface (`SURFACE`)

| Surface | Stable from | What it is |
|---|---|---|
| Mod id `firearms` | 1.0 | Fabric mod id |
| The seven data component type ids: `firearms:base`, `firearms:ammo`, `firearms:attachment_muzzle`, `_optic`, `_magazine`, `_grip`, `_stock` | 1.0 | `04-architecture.md` `ARCH-DEC-005`; the shape a datapack's item-model or trade predicate may branch on |
| Weapon data files (one per base weapon, keyed by class, calibre and base stats) | 1.0 | `domains/weapon.md` `WEAPON-DEC-003`; datapack-addable without new Java |
| Attachment data files (one per attachment, keyed by slot and stat modifiers) | 1.0 | `domains/attach.md`; datapack-addable without new Java |
| Cartridge items, one per calibre | 1.0 | `domains/ammo.md` |
| Recipe serializer/type id for the shared attach recipe: `firearms:attach_smithing` (smithing, confirmed `FA-7`), `firearms:attach_deploying` (deploying, confirmed `FA-8`; supersedes the `firearms:deploy_attach` proposal) | 1.0 | `04-architecture.md` `ARCH-DEC-002`, `ARCH-DEC-003` |
| Damage type id `firearms:bullet` | 1.0 | `domains/combat.md` |
| The tag-merge files at `data/minecraft/tags/villager_trade/{weaponsmith,fletcher}/level_N.json` and the trade JSONs under `data/firearms/villager_trade/` | 1.0 | `domains/trade.md`; datapack-overridable the same way vanilla trade tags are |
| The per-class slot table (which classes have which slots) | 1.0 | `domains/weapon.md` §3; a new weapon must use an existing class, or a new class is a code change, not data |
| Translation keys for weapon/attachment/cartridge names, tooltip labels | 1.0 | `domains/ui.md` |
| JEI/EMI category (none at 1.0) | N/A | `domains/ui.md` `UI-DEC-001` |

Not public: the stat derivation function's internal implementation beyond the guarantee it makes
(`domains/weapon.md` `WEAPON-REQ-003`); the bullet entity's internal class shape; the shared attach
function's internal shape beyond the recipe JSON contracts above; the three client mixins'
injection points (`04-architecture.md`). Versioned by SemVer over the surface above
(`operations/release.md`).

`SURFACE-REQ-001`: a change to a stable surface is a major version.
`SURFACE-REQ-002`: **where** a datapack's weapon or attachment data file carries an out-of-range
stat modifier (e.g. a negative magazine size, a zero fire-rate divide), the system shall clamp it
to the nearest valid value and log once, rather than fail to start — mirroring
`create_villager_customers`'s `SURFACE-REQ-002` and `create_synthetic_diamonds`'s `ROLL-REQ-003`
clamp-and-log pattern.
`SURFACE-REQ-003`: a new weapon or attachment added purely as a data file, using an existing class
and an existing slot, is never a breaking change to the public surface.
