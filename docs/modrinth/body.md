# Modrinth listing (paste-ready)

**Placeholder, filled in by FA-15.** Project settings below are fixed by the spec; the body,
gallery shot list and icon are FA-15's territory.

## Project settings

| Field | Value |
|---|---|
| Name | Create: Firearms |
| Slug | `firearms` |
| Summary | A roster of modern firearms under real-world designations, built from a base weapon plus attachments at the smithing table or by deployers, with dynamic stats, traded by weaponsmiths and fletchers. |
| Categories | Equipment, Technology (secondary: Adventure, Game-Mechanics, Magic) |
| Licence | MIT |
| Client side | Required |
| Server side | Required |
| Loaders | Fabric |
| Game versions | 26.2 |
| Dependencies | Create Fly (required), Fabric API (required) |
| Icon | `icon.png` in this folder: a navy badge rendered from an in-game asset of this mod's own — the AKM item model, or the M1911 sprite if a block-model render does not apply (`just icon` regenerates it, FA-15) |
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

To be written at FA-15, from `docs/spec/00-context.md` and the domain files
(`docs/spec/domains/weapon.md`, `domains/attach.md`, `domains/ammo.md`, `domains/combat.md`,
`domains/trade.md`). States plainly that every stat and price in the sheet is a proposed starting
point, not a balanced final number (`docs/spec/operations/release.md` `REL-REQ-003`), and that
every weapon uses only its real-world designation (`docs/spec/operations/compliance.md`).
