---
title: "create_firearms spec — platform matrix"
type: "spec"
category: "create_firearms"
---

# Platform matrix (`PLATFORM`)

| Row | Value | How it is checked |
|---|---|---|
| Minecraft | 26.2 (`~26.2` in `fabric.mod.json`) | `just doctor`, game tests on a dedicated server |
| Fabric Loader | ≥ 0.19.5 | `fabric.mod.json` |
| Fabric API | ≥ 0.160.0 (development tooling only — no Fabric event carries this mod's own recipe lookup or its scope mixins) | `fabric.mod.json` |
| Create Fly | `26.2-rc-2-6.0.9-1`, mod id `create`, declared version `6.0.9-1`, `implementation` coordinate `maven.modrinth:create-fly`, pinned | `fabric.mod.json` depends `create`; `NOTICE` |
| Java | 25 | `just doctor` |
| Gradle / Loom | 9.5.1 wrapper / 1.17 | wrapper properties |
| Operating systems | macOS, Linux, Windows: the JVM's | not tested separately; nothing native |
| Client and server | Combat resolution, recipe assembly, stat derivation, and trade checks are entirely server-side. The three scope mixins, item-model rendering, tooltip text, and the cosmetic recoil/tracer/sound layer are client-side; the smithing/deploying recipe JSON is loaded both sides, as any vanilla recipe is | game tests (server), `just client` (client checklist, `operations/testing.md`) |
| Mixin targets, client (`create_firearms`, client-side sourceset) | `Player.isScoping()` (widened for a firearm with a zoom-bearing optic); `AbstractClientPlayer.getFieldOfViewModifier` (per-optic zoom factor); the HUD's spyglass-overlay draw (per-optic texture) — three `@Mixin` targets on three vanilla classes, all client-only, all gated on the same widened `isScoping()` (research `smithing-and-item-model-layers-26-2.md` §C.3) | mixin config, `just check` |
| Mixin targets, server | **None.** The smithing and deploying recipe classes are found through ordinary `Recipe.getType()`/`RecipeType` registry mechanics; the villager-trade extension is a tag-merge, not a mixin — the same zero-server-mixin shape `create_synthetic_diamonds` confirmed for its own recipe class (`04-architecture.md` `ARCH-DEC-002`, `ARCH-DEC-003`) | mixin config, `just check` |
| Entities | One new entity type, the bullet (`domains/combat.md`), client- and server-registered the ordinary way; no mixin needed for its own hit detection, which reuses vanilla's public `ProjectileUtil.getHitResultOnMoveVector` | game tests |

`PLATFORM-REQ-001`: **If** any row moves, **then** `just doctor` fails naming the row.
`PLATFORM-REQ-002`: **If** a Create Fly release renames or removes `AllRecipeTypes.DEPLOYING` or
changes `DeployerBlockEntity.getRecipe()`'s lookup mechanism, **then** the build fails at compile
time (the field reference no longer resolves) rather than silently producing recipes the deployer
never finds.
`PLATFORM-REQ-003`: **If** a Minecraft release moves `Player.isScoping()`'s signature or its call
sites in `AbstractClientPlayer`/the HUD/`ItemInHandRenderer`, **then** the build fails at compile
time for the affected mixin, the same bounded exposure `create_villager_customers` already accepts
for its own villager-brain mixin.
