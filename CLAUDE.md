# create_firearms

A Create Fly add-on for Minecraft 26.2 on Fabric: a roster of modern firearms under real-world
designations, built from a base weapon plus attachments permanently attached at the vanilla
smithing table or by a Create deployer — no new block, no detaching. Stats are derived at runtime;
combat runs under vanilla damage rules; weapons and attachments sell through the existing
weaponsmith, ammunition through the existing fletcher.

**This file routes. It does not hold content.** The specification is `docs/spec/`.

## Read this before you do that

| about to… | read first |
|---|---|
| anything at all | `docs/spec/README.md`, then the one domain file you need |
| find where something lives | `docs/map.md`; generated, never edited |
| touch `firearms.model` | it has no Minecraft imports; the build's `verifyPurePackage` enforces it |
| touch a base weapon, its stats, firing, reload or durability | `docs/spec/domains/weapon.md` |
| touch an attachment, a slot, or either attach recipe (smithing or deploying) | `docs/spec/domains/attach.md`, `docs/spec/04-architecture.md` `ARCH-DEC-002`, `ARCH-DEC-003` |
| touch a cartridge or a calibre | `docs/spec/domains/ammo.md` |
| touch the bullet entity, damage, spread, recoil or the scope mixins | `docs/spec/domains/combat.md`, `docs/spec/04-architecture.md` `ARCH-DEC-004` |
| touch a weaponsmith or fletcher trade | `docs/spec/domains/trade.md`, `docs/spec/decisions/DEC-012-villager-integration.md` |
| touch a tooltip, the debug command, or any player-facing surface | `docs/spec/domains/ui.md` |
| add or change a mixin | `docs/spec/04-architecture.md` `ARCH-DEC-001`; every mixin in this mod is client-only, on the shared `isScoping()` gate |
| name, texture or describe a weapon | the compliance rule below |
| add a dependency | `docs/spec/decisions/DEC-003-licence.md` (MIT) and heimathafen's dependency policy |
| commit | scope `firearms`, the ticket key (`FA-N`) in the subject |

## Compliance rule

Every weapon uses only its real-world designation (M1911, Micro Uzi, AKM, Ruger Mini-14, AWM,
Winchester Model 1897 at 1.0) — these are firearm model names, not any game's property. PUBG is a
design reference for the roster and slot layout only, never a naming or branding source: no PUBG
name, logo, splash art or texture appears anywhere in this mod, its listing, or its repository
(`docs/spec/operations/compliance.md` `COMP-REQ-002`). Every asset is drawn fresh for this mod's
own real-world-designated weapons; no Minecraft, Create Fly or third-party texture, model or sound
is copied.

## Working here

```
kontor claim FA-N
kontor branch new FA-N <slug>
just check
```

`just --list` shows the task surface; `just spec-sync` refreshes `docs/spec/` from the vault; `just map` regenerates the map.

## Standing rules

- The spec is authoritative; `docs/spec/` is a copy of heimathafen's vault.
- A design question the spec does not answer is asked, never decided inline.
- Nothing leaves the player's machine: no telemetry, no network calls (`docs/spec/operations/compliance.md`).
- Always keep a playable build: `just client` boots with Create Fly at every merge.
