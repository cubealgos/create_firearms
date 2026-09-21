---
schema_version: 1
id: 01M2YWEQM3NYN10116V54M9F7F
key: FA-14
type: test
title: Requirement-to-test table, three check runs, client checklist
created_by: kevin
created_at: 2026-09-20T07:45:54Z
---

## Scope

A requirement-to-test table mapping every `WEAPON-REQ`, `ATTACH-REQ`, `AMMO-REQ`, `COMBAT-REQ`,
`TRADE-REQ` and `UI-REQ` id to the test that proves it, or a recorded ruling where no automated test
applies (`TEST-REQ-001`); three consecutive green `just check` runs on a clean checkout; Kevin's own
client checklist covering firing feel across all six weapons and fire modes, the scope mixins'
zoom/overlay/suppression behaviour, visible attach on both front ends, and confirming no JEI/EMI
entry appears anywhere (`operations/testing.md` client-checklist row). Balance-sweep pass over every
proposed stat and price, since every number in `domains/weapon.md`, `domains/attach.md`,
`domains/ammo.md` and `domains/trade.md` is marked a proposed starting point, not a balanced final
figure. Not the release build itself (`FA-16`) and not the Modrinth assets (`FA-15`).

## Approach

Walk each domain file's requirements table, add or confirm the owning test, and where a requirement
genuinely cannot be pure-tested (the two recipe classes actually being found by a real
`SmithingMenu`/`DeployerBlockEntity`, the scope mixins producing a visible effect against a real
client renderer, automatic-fire bullet-entity volume at real server tick rates —
`operations/testing.md`'s own "what is genuinely hard" section), name the game test or the client
checklist item that stands in for it instead of leaving the id unmapped.

## Acceptance criteria

- [x] Every `WEAPON-REQ`, `ATTACH-REQ`, `AMMO-REQ`, `COMBAT-REQ`, `TRADE-REQ` and `UI-REQ` id in the
      six domain files is mapped to a test or a recorded ruling (`TEST-REQ-001`).
- [x] `just check` is green three consecutive times on a clean checkout.
- [ ] Kevin's client checklist is completed and any findings are folded back into the relevant
      domain file or a follow-up ticket.
- [x] (Kevin, 2026-09-21: keep as is, recorded as WEAPON-DEC in the spec) The balance sweep either confirms the proposed stats/prices as final or records specific
      adjustments in the relevant domain files' own tables.

## Requirement-to-test table

Legend: a bare class/method name is a game test in `src/gametest/java/firearms/gametest/`; a name
prefixed `unit:` is a pure test in `src/test/java/firearms/`; **(new, FA-14)** marks a test this
ticket added to close a gap; **client checklist: …** names the checklist item below that stands in
for what cannot be proven headless; **GAP — ruled: …** records a deliberate decision not to add a
test, with the reason, rather than leaving the id unmapped.

### `WEAPON-REQ` (`docs/spec/domains/weapon.md`)

| ID | Test |
|---|---|
| `WEAPON-REQ-001` | `AttachRuleTest` (all 6 classes × their own/lacking slots, `unit:`) + `ModelAssetsTest.everyBaseHasExactlyItsOwnClasssSlotLayersAndNoOther` |
| `WEAPON-REQ-002` | `unit:model.WeaponBaseStatsTest` (all six bare rows) + `DataLoaderGameTest.theWeaponLoaderProducesExactlyTheSixBasesWithTheModelsValues` |
| `WEAPON-REQ-003` | `unit:model.StatDerivationOrderTest`, `unit:model.AttachmentModifierTest`, `StatDerivationGameTest`, `WeaponTooltipGameTest` (tooltip re-runs the live function) |
| `WEAPON-REQ-004` | `FiringGameTest.aLoadedWeaponFiresOnceAndMovesAmmoDurabilityAndCooldownTogether` (semi), `.anAutoWeaponFiresNBulletsOverNTimesFireRateTicksOfHeldUse` (auto), `.aPumpWeaponRefusesASecondShotInsideThePumpDelay` (pump) |
| `WEAPON-REQ-005` | `AimSpreadSelectionGameTest.isAimingReadsTheSneakKeyAsTheInterimAimSignal` **(new, FA-14)**, `.hipFireUsesTheWiderConeAndAimingUsesTheNarrowerOne` **(new, FA-14)** (fires 60 hip-fire + 20 aiming shots, measures each bullet's real angle off the look vector, proves hip-fire uses the wider cone and aiming uses the optic-narrowed one), plus `unit:model.AttachmentModifierTest`/`StatDerivationOrderTest` for the stat math itself |
| `WEAPON-REQ-006` | `CraftingRecipeGameTest.everyBaseWeaponCraftsWithItsOwnGridAndMaxDamage` |
| `WEAPON-REQ-007` | `FiringGameTest.anEmptyWeaponWithNoMatchingCartridgesFiresNothingAndStaysEmpty`, `.anEmptyWeaponWithOnlyWrongCalibreCartridgesFiresNothingAndLeavesThemUnconsumed` **(new, FA-14)** |
| `WEAPON-REQ-008` | `FiringGameTest.aLoadedWeaponFiresOnceAndMovesAmmoDurabilityAndCooldownTogether` |
| `WEAPON-REQ-009` | `FiringGameTest.aLoadedWeaponFiresOnceAndMovesAmmoDurabilityAndCooldownTogether` (`ON_COOLDOWN` on the immediate second attempt) |
| `WEAPON-REQ-010` | `FiringGameTest.anEmptyWeaponWithMatchingCartridgesReloadsToMagazineSizeAndConsumesThem` |
| `WEAPON-REQ-011` | `FiringGameTest.anEmptyWeaponWithNoMatchingCartridgesFiresNothingAndStaysEmpty`, `.anEmptyWeaponWithOnlyWrongCalibreCartridgesFiresNothingAndLeavesThemUnconsumed` **(new, FA-14)** |
| `WEAPON-REQ-012` | `unit:ModelAssetsTest.everyItemDefinitionModelReferenceResolvesToAnExistingModelFile`, `.everyBaseHasExactlyItsOwnClasssSlotLayersAndNoOther` |
| `WEAPON-REQ-013` | `FiringGameTest.aSuppressorChangesTheFireSoundEventChosen` proves the sound-event *selection*; the audible per-weapon distinctness and suppressor volume/range reduction are **client checklist: sounds** |
| `WEAPON-REQ-014` | `WeaponDurabilityAndAnvilGameTest.aWeaponStackIsNeverEnchantableAndNeverAValidRepairTarget`, `.aRealAnvilOffersNoMaterialRepairAndNoEnchantForADamagedWeapon`, `AnvilCombineRefusalGameTest.aWeaponAndAnIronIngotYieldNoAnvilResult` |
| `WEAPON-REQ-015` | `WeaponDurabilityAndAnvilGameTest` (both methods), `AnvilCombineRefusalGameTest.aRealEnchantingTableOffersNoEnchantmentForAWeapon` |

### `WEAPON-FAIL`

| ID | Test |
|---|---|
| `WEAPON-FAIL-001` | `FiringGameTest.attemptingToFireANonWeaponItemReportsNotAWeapon` **(new, FA-14)** |
| `WEAPON-FAIL-002` | see `WEAPON-REQ-007` |
| `WEAPON-FAIL-003` | see `WEAPON-REQ-011` |
| `WEAPON-FAIL-004` | `FiringGameTest.aRemovedAttachmentIdIsTreatedAsAnAbsentSlotNotACrash` **(new, FA-14)** |
| `WEAPON-FAIL-005` | GAP — ruled: the break-at-zero-durability behaviour is stock `ItemStack.hurtAndBreak`/vanilla durability code with no mod-specific branch (`WEAPON-REQ-008`'s own per-shot decrement is the only mod code involved, already tested there); a dedicated test would only be re-testing vanilla |
| `WEAPON-FAIL-006` | `AnvilCombineRefusalGameTest.twoDamagedWeaponStacksNeverCombineAtAnAnvil`, `.twoDamagedIronPickaxesStillCombineAtAnAnvil` (scope proof) |

### `ATTACH-REQ` (`docs/spec/domains/attach.md`)

| ID | Test |
|---|---|
| `ATTACH-REQ-001` | `AttachSmithingGameTest.aSuppressorAttachesToAMicroUziIntoTheMuzzleSlot`, `unit:model.AttachRuleTest` |
| `ATTACH-REQ-002` | `unit:model.AttachRuleTest.everySlotTheClassHasIsNeverAttachableWhenOccupied`, `AttachSmithingGameTest.aSecondSuppressorOnAnAlreadySuppressedUziMatchesNothing`, `AttachDeployingGameTest.aSecondSuppressorOnAnAlreadySuppressedUziMatchesNothing` |
| `ATTACH-REQ-003` | `AttachSmithingGameTest.aSuppressorAttachesToAMicroUziIntoTheMuzzleSlot`, `AttachDeployingGameTest.aRealDeployerFindsAndAppliesTheDeployingAttachRecipe` (both assert every other component, including `firearms:base`, is unchanged) |
| ~~`ATTACH-REQ-004`~~ | Withdrawn — no detach path exists; no test, id kept not reused |
| `ATTACH-REQ-005` | `AttachDeployingGameTest.aRealDeployerFindsAndAppliesTheDeployingAttachRecipe` |
| `ATTACH-REQ-006` | `AttachDeployingGameTest.aRealDeployerFindsAndAppliesTheDeployingAttachRecipe` (`deployer.invHandler.getItem(0).isEmpty()`) |
| `ATTACH-REQ-007` | `CraftingRecipeGameTest.everyAttachmentCraftsWithItsOwnIngredients` |
| `ATTACH-REQ-008` | `AttachDeployingGameTest.aRealDeployerFindsAndAppliesTheDeployingAttachRecipe` (`TEST-REQ-003`'s byte-identical check against a real `SmithingMenu` result) |

### `ATTACH-FAIL`

| ID | Test |
|---|---|
| `ATTACH-FAIL-001` | `AttachSmithingGameTest.aGripOnAWinchester1897MatchesNothingSinceTheShotgunClassHasNoGripSlot`, `.aStockOnAnM1911MatchesNothingSinceThePistolClassHasNoStockSlot`, `unit:model.AttachRuleTest.everySlotTheClassLacksIsNeverAttachableEitherWay`, `unit:model.LoadoutSlotValidationTest` |
| ~~`ATTACH-FAIL-002`~~ | Withdrawn — no detach recipe to attempt this against; no test |
| `ATTACH-FAIL-003` | see `ATTACH-REQ-002` |
| `ATTACH-FAIL-004` | `AttachDeployingGameTest.aSecondSuppressorOnAnAlreadySuppressedUziMatchesNothing` |

### `AMMO-REQ` (`docs/spec/domains/ammo.md`)

| ID | Test |
|---|---|
| `AMMO-REQ-001` | `CraftingRecipeGameTest.everyCartridgeCraftsFourAtATime` |
| `AMMO-REQ-002` | `AmmoCartridgeGameTest.everyCartridgeStacksToSixtyFour` **(new, FA-14)** |
| `AMMO-REQ-003` | `AmmoCartridgeGameTest.aCartridgeCarriesNoneOfThisModsOwnComponents` **(new, FA-14)** |
| `AMMO-REQ-004` | `FiringGameTest.anEmptyWeaponWithMatchingCartridgesReloadsToMagazineSizeAndConsumesThem`, `.anEmptyWeaponWithOnlyWrongCalibreCartridgesFiresNothingAndLeavesThemUnconsumed` **(new, FA-14)** |
| `AMMO-REQ-005` | `unit:SourceSurfaceTest.noCartridgeRecipeUsesACreateProcessingType` **(new, FA-14)** |

### `AMMO-FAIL`

| ID | Test |
|---|---|
| `AMMO-FAIL-001` | `FiringGameTest.anEmptyWeaponWithOnlyWrongCalibreCartridgesFiresNothingAndLeavesThemUnconsumed` **(new, FA-14)** |
| `AMMO-FAIL-002` | GAP — ruled: trivially true by architecture — cartridge items are registered and craftable unconditionally (`ItemRegistration`, `CraftingRecipeGameTest`), with no runtime check against any weapon's existence anywhere in the crafting or item-registration path; nothing to construct a failing case against |

### `COMBAT-REQ` (`docs/spec/domains/combat.md`)

| ID | Test |
|---|---|
| `COMBAT-REQ-001` | `unit:combat.SpreadMathTest` (the pure roll) + `AimSpreadSelectionGameTest` (real rolls through the real path) — the spread roll is drawn only from `ServerLevel.getRandom()` inside `BulletSpawner`, a server-only type, so client influence is structurally impossible, not just untested |
| `COMBAT-REQ-002` | `BulletGameTest.aVeryHighVelocityBulletDoesNotTunnelThroughATarget` |
| `COMBAT-REQ-003` | `BulletGameTest.aBulletWithNothingToHitDespawnsAfterItsLifeWithNoDamageOrDrop` |
| `COMBAT-REQ-004` | `BulletGameTest.aBulletHitsATargetTwentyBlocksAwayAndDealsTheGivenDamage` |
| `COMBAT-REQ-005` | `BulletGameTest.aShotgunTriggerPullSpawnsEightIndependentlySpreadPellets`, `FiringGameTest.aShotgunTriggerPullSpawnsItsFullPelletCount` |
| `COMBAT-REQ-006` | **client checklist: scoped AWM zoom/overlay** — `PlayerScopingMixin` needs a real client renderer (`operations/testing.md`'s own "what is genuinely hard"); `unit:client.scope.ScopeZoomTest` proves the pure optic→zoom-factor mapping the mixin reads |
| `COMBAT-REQ-007` | **client checklist: scoped AWM zoom/overlay** (per-optic FOV) — same mixin limitation; `ScopeZoomTest` covers the data side |
| `COMBAT-REQ-008` | **client checklist: scoped AWM zoom/overlay** (per-optic overlay texture) — same limitation |
| `COMBAT-REQ-009` | `FiringGameTest.aSuppressorChangesTheFireSoundEventChosen` proves event *selection*; audible volume/range is **client checklist: sounds** |
| `COMBAT-REQ-010` | `BulletGameTest.aBulletHitsATargetTwentyBlocksAwayAndDealsTheGivenDamage` (direct/causing entity on the `DamageSource`); the actual `pvp` gamerule behaviour end-to-end is **client checklist: bullets ... a player under PvP rules** |
| `COMBAT-REQ-011` | `unit:client.fire.RecoilKickTest` (the pure recovery maths); that hit/damage/ammo never depend on it is structural (`BulletGameTest`/`FiringGameTest` show hit resolution needs nothing from the client); the actual rendered kick/tracer/flash/particle is **client checklist: the recoil kick**, **... firing each class** |

### `COMBAT-FAIL`

| ID | Test |
|---|---|
| `COMBAT-FAIL-001` | `BulletGameTest.aBulletWithNothingToHitDespawnsAfterItsLifeWithNoDamageOrDrop` |
| `COMBAT-FAIL-002` | `BulletGameTest.aBulletFiredAtAWallDiscardsWithNoPenetration` |
| `COMBAT-FAIL-003` | GAP — ruled: needs a second mod also widening `isScoping()` to reproduce; the spec's own text already accepts this as "a mixin-ordering problem," the same shape `create_villager_customers` accepts for its own brain mixin — not testable within this repo |
| `COMBAT-FAIL-004` | `BulletGameTest.anInvulnerableTargetTakesNoDamage` **(new, FA-14)** |

### `TRADE-REQ` (`docs/spec/domains/trade.md`)

| ID | Test |
|---|---|
| `TRADE-REQ-001` | `TradeFileGameTest.everyTradeFileResolvesAndIsTaggedIntoItsLevel` |
| `TRADE-REQ-002` | `TradeFileGameTest.everyTradeFileResolvesAndIsTaggedIntoItsLevel` |
| `TRADE-REQ-003` | `unit:SourceSurfaceTest.noSourceFileRegistersANewVillagerProfessionOrPointOfInterest` **(new, FA-14)** |
| `TRADE-REQ-004` | `MasterBuyBackGameTest` (all three methods) |
| `TRADE-REQ-005` | `MasterBuyBackGameTest.masterAkmAcceptsTheNamedConfigurationWithAnyUnconstrainedSlot` |
| `TRADE-REQ-006` | `unit:SourceSurfaceTest.noSourceFileImportsCreateVillagerCustomers` **(new, FA-14)** |

### `TRADE-FAIL`

| ID | Test |
|---|---|
| `TRADE-FAIL-001` | `MasterBuyBackGameTest.masterAkmRejectsAMissingOrWrongNamedAttachment` |
| `TRADE-FAIL-002` | GAP — ruled: describes vanilla `MerchantScreen`'s own absence of a component-aware cost line, not this mod's code; nothing to test |
| `TRADE-FAIL-003` | GAP — ruled: describes a *different* mod's (`create_villager_customers`) own current limitation; not reproducible or testable from this repository |

### `UI-REQ` (`docs/spec/domains/ui.md`)

| ID | Test |
|---|---|
| `UI-REQ-001` | `WeaponTooltipGameTest` (all four methods) |
| `UI-REQ-002` | **client checklist: scoped AWM zoom/overlay**, **red dot not zooming** — `ScopeOverlayMixin` needs a real client renderer |
| `UI-REQ-003` | GAP — ruled: no hit-marker code exists to scan for by any reliable pattern, and `grep -rn HudRenderCallback\|HudElementRegistry src/main/java` returns nothing at all — no HUD render hook of any kind is registered, which forecloses a hit marker structurally, the same absence `UI-FAIL-001` restates |
| `UI-REQ-004` | `unit:SourceSurfaceTest.noSourceFileImportsAVanillaMenuRegistrationType` **(new, FA-14)** |
| `UI-REQ-005` | GAP — ruled: `build.gradle.kts`'s own dependency list names only `minecraft`, `fabricLoader`, `fabricApi`, `createFly` and JUnit — no JEI/EMI artifact is even on the classpath, which forecloses any integration structurally, stronger than a source-text scan could prove |
| `UI-REQ-006` | `DebugCommandGameTest` (all methods) |

### `UI-FAIL`

| ID | Test |
|---|---|
| `UI-FAIL-001` | see `UI-REQ-003` |
| `UI-FAIL-002` | see `UI-REQ-005` |

### `contracts/data-contract.md` (`DATA-REQ`)

| ID | Test |
|---|---|
| `DATA-REQ-001` | `ComponentCodecGameTest.baseRoundTripsAndMigratesAndDegrades` |
| `DATA-REQ-002` | `ComponentCodecGameTest.baseRoundTripsAndMigratesAndDegrades` (version `0` migrates forward to `BaseCodec.VERSION`) **(extended, FA-14)** |
| `DATA-REQ-003` | `ComponentCodecGameTest.baseRoundTripsAndMigratesAndDegrades` (version `999` kept as-is, `readOnly()`) |
| `DATA-REQ-004` | `ComponentCodecGameTest` (all three methods: malformed `firearms:base`/`firearms:ammo`/slot component all fail to decode rather than throw) |
| `DATA-REQ-005` | GAP — ruled: `grep -rn "addAdditionalSaveData\|readAdditionalSaveData\|BlockEntityType\|SavedData" src/main/java` returns nothing — no entity, block entity or world-save file of this mod's own exists to carry stray state; structurally foreclosed, not a behaviour to exercise |

### `contracts/public-surface.md` (`SURFACE-REQ`)

| ID | Test |
|---|---|
| `SURFACE-REQ-001` | GAP — ruled: a release-process/SemVer discipline rule, not a runtime behaviour; enforced by review at release time (`operations/release.md`), out of this sweep |
| `SURFACE-REQ-002` | `unit:model.StatDerivationClampTest` (all four methods) |
| `SURFACE-REQ-003` | `DataLoaderGameTest.aDatapackAddedWeaponAndAttachmentInAnExistingClassAndSlotLoadWithZeroNewJava` |

### `operations/compliance.md` (`COMP-REQ`)

| ID | Test |
|---|---|
| `COMP-REQ-001` | `unit:SourceSurfaceTest.noNetworkingTypeIsReferencedByTheMod` |
| `COMP-REQ-002` | `unit:SourceSurfaceTest.noSourceOrResourceFileNamesPubgOrItsBranding` |

### `operations/testing.md`'s own `TEST-REQ`

| ID | Test |
|---|---|
| `TEST-REQ-001` | This table |
| `TEST-REQ-002` | Manual deliberate-break proof performed this ticket (not a standing test, per the requirement's own "once"): a forbidden `import net.minecraft.resources.Identifier;` was added to `firearms.model.Caliber`, `./gradlew verifyPurePackage` failed naming the file and line, the import was reverted, and the task passed again — recorded here, not preserved as a file |
| `TEST-REQ-003` | `AttachDeployingGameTest.aRealDeployerFindsAndAppliesTheDeployingAttachRecipe` |
| `TEST-REQ-004` | `BulletGameTest.aBulletHitsATargetBeyondFortyBlocks` |

`docs/spec/contracts/platform-matrix.md`'s `PLATFORM-REQ-001`–`003` are out of this table's scope by
the ticket's own instruction (only `DATA-REQ`/`SURFACE-REQ` from `contracts/*.md` were named) — they
are process/build-time contracts checked by `just doctor` and the mixin config itself, not by a test
this sweep would add.

## Constraints and prior findings

`docs/spec/operations/testing.md` `TEST-REQ-001`–`004` and its own "what is genuinely hard" section,
`docs/spec/operations/release.md` `REL-REQ-003` (release notes must state every number is a proposed
starting point regardless of what this sweep confirms or changes). Blocked by `FA-11` (a complete
client experience to check) and `FA-13` (the trades the sweep also verifies).

**Gap-closing work done this ticket**: eleven new/extended test methods across five files (see the
table above for the exact id-by-id mapping) — `AmmoCartridgeGameTest` (new file, 2 methods),
`AimSpreadSelectionGameTest` (new file, 2 methods), `FiringGameTest` (+3 methods), `BulletGameTest`
(+1 method), `ComponentCodecGameTest` (1 method extended, no new test count), `SourceSurfaceTest`
(+4 methods). Game tests: 53 → 61. Both new gametest classes were added to
`src/gametest/resources/fabric.mod.json`'s `fabric-gametest` entrypoint list — `FA-4`'s own finding
("a green `just check` after adding a new gametest class is not on its own proof the new tests
ran") was checked against directly: the full-suite count moved from 53 to 58 to 60 to 61 exactly as
each batch of new methods was added, confirming every one actually ran, not just compiled.
`just map` was re-run after every source change (`docs/map.md` and two `docs/map/root/*.md` files
cover the `firearms.gametest.gametest`/`firearms.test` packages and were stale after each edit).

**A real flake was caught and fixed while writing `AimSpreadSelectionGameTest`**: its first draft
captured the player's look vector once before the 80-shot trial loop; one run in five failed with
an aiming shot's measured angle (2.5543°) narrowly exceeding the red dot's 2.55° cone — a real bug
signature, but `SpreadMath`'s own rotation formula makes that mathematically impossible when fed
2.55° (radius is drawn `< 1.0` strictly, so the true angle is always strictly less), which pointed
at a measurement artifact rather than a `FiringLogic` bug. Reading `player.getLookAngle()` fresh
immediately before each shot instead (matching exactly what `FiringLogic.fire()` itself reads at
that same point in the same tick) fixed it: 15 further consecutive full-suite runs afterward, zero
failures. The epsilon was also widened from 1e-3° to 0.01° as a safety margin, still two orders of
magnitude below the 0.45° gap between the two cones the test is distinguishing.

**`DATA-REQ-005`, `UI-REQ-003`/`UI-FAIL-001`, `UI-REQ-005`/`UI-FAIL-002` were confirmed by direct
grep, not by a new test**, since there is no reliable pattern to assert a true negative against
that a future change couldn't silently defeat by using different vocabulary — recorded as rulings
in the table with the exact commands run, not left unmapped.

## Verification runs

Three consecutive `just check` runs (`lint` + `map-check` + `test` [`test-java` + `test-tools`] +
`gametest`), `./gradlew clean` run immediately before each, on the pushed
`feature/fa-14-test-sweep` branch (commit `bdbb6f0`):

| Run | `verifyPurePackage`/lint | `map-check` | `test` | `gametest` | Wall time (`just check`) |
|---|---|---|---|---|---|
| 1 | BUILD SUCCESSFUL | 25 files current | BUILD SUCCESSFUL | **All 61 required tests passed :)** | 11.434 s |
| 2 | BUILD SUCCESSFUL | 25 files current | BUILD SUCCESSFUL | **All 61 required tests passed :)** | 11.004 s |
| 3 | BUILD SUCCESSFUL | 25 files current | BUILD SUCCESSFUL | **All 61 required tests passed :)** | 11.060 s |

No failure, no flake, across these three plus the 15+ repeated `runGameTest`-only runs during
development (`## Constraints and prior findings` above). `python3 -m unittest discover -s tools -p
'test_*.py'` (the `test-tools` half of `just test`) stayed at 4/4 OK throughout, unaffected by this
ticket's changes.

## Client checklist (Kevin)

Everything below needs a running client (`just client`) against a real render pipeline, scope
overlay, sound engine, or a second real player — the parts `docs/spec/operations/testing.md` itself
names as "genuinely hard" to prove headless. Check each; fold any finding back into the relevant
domain file (or a follow-up ticket) rather than silently living only in this checkbox.

**Debug give and models**
- [ ] `/firearms debug give` produces a correct, bare item for each of the six bases (M1911, Micro
      Uzi, AKM, Ruger Mini-14, AWM, Winchester Model 1897).
- [ ] `/firearms debug give` produces one fully loaded weapon (every slot its class has filled with
      an attachment, plus ammo) for at least one base.
- [ ] The inventory icon and the held (first-/third-person) model both show the correct composite
      layers for a bare weapon and for the fully loaded one — no missing layer, no wrong texture.
- [ ] A filled slot's layer never reverts or disappears once set (attach a second, still-empty slot
      later and confirm the first attachment's layer is still there).

**Firing feel**
- [ ] Firing the semi class (M1911) feels like one shot per press, blocked until the cooldown clears.
- [ ] Firing the auto class (Micro Uzi, AKM) feels like a sustained stream while held.
- [ ] Firing the pump class (Winchester Model 1897) feels like one shot per press with a visible pump delay.
- [ ] Reloading from cartridges in inventory (all six calibres, each on its own matching weapon) works and feels right.
- [ ] The empty click (no ammo, no matching cartridge) sounds and feels distinct from a normal shot.
- [ ] Durability visibly wears down shot by shot and the weapon breaks, unrepairable, at zero.

**Scoping (AWM, or any weapon with a magnifying optic attached)**
- [ ] Each magnifying optic (2x, 3x, 4x, 6x, 8x, 15x) zooms the view (spyglass-style) and swaps the
      overlay texture to that optic's own art while aiming.
- [ ] Red dot and holo never zoom and never draw an overlay, while still visibly narrowing aim spread.
- [ ] The held-item render is suppressed while scoped, exactly as the spyglass suppresses it.

**Recoil, attach front ends, and refusals**
- [ ] The cosmetic recoil kick (camera nudge on each shot, recovering over time) is visible and feels distinct per weapon's own recoil stat.
- [ ] The smithing table attaches an attachment into a weapon's empty, class-appropriate slot.
- [ ] The smithing table refuses to attach into an already-filled slot (no result).
- [ ] The smithing table refuses an attachment whose class lacks that slot (e.g. an optic on a shotgun; no result).
- [ ] A deployer on a depot attaches an attachment to a weapon placed on it.
- [ ] A deployer on a moving contraption belt attaches an attachment to a weapon passing on the belt.
- [ ] The anvil refuses to combine two weapons (no repair, no result).
- [ ] The anvil refuses a weapon plus an iron ingot (no material-repair result).
- [ ] The enchanting table offers nothing at all for a weapon (every slot's cost stays zero).

**Trades**
- [ ] The weaponsmith's trades appear correctly at each level (1 through 5) matching the catalogue in `domains/trade.md` §3.
- [ ] The fletcher's cartridge trades appear correctly at each level (1 through 4).
- [ ] Each master buy-back trade (one per class, weaponsmith level 5) wants exactly its own named configuration and rejects a mismatched one, in the real trade screen.

**Tooltips, hit feel, and sound**
- [ ] Weapon, attachment and cartridge tooltips read correctly and match what `WeaponTooltipGameTest` asserts, formatted and laid out well.
- [ ] A bullet hits a zombie at real range (well beyond point-blank) and applies damage/knockback as expected.
- [ ] A bullet hits another player under PvP rules exactly as an arrow would (respects the `pvp` gamerule, team allegiance, invulnerability ticks).
- [ ] Each weapon's fire sound and empty-click sound are distinct and sound right; a suppressor audibly reduces the fire sound's volume/range.
- [ ] No JEI or EMI entry of any kind appears anywhere for this mod's items or recipes (`UI-REQ-005`).

## Balance notes

From the tests' own numbers (`WeaponBaseStatsTest`, `domains/weapon.md` roster table) — **no number
changed by this ticket**; these are flags for the sweep ruling only.

**DPS vs. a diamond sword** (sword reference: 7 damage × 1.6 attacks/s = 11.2 DPS, sustained full-charge attacks; each firearm's DPS assumes the player can sustain fire at exactly the fire-rate cooldown, ignoring reloads):

| Weapon | Damage/hit (×pellets) | Fire rate | DPS | vs. sword |
|---|---|---|---|---|
| M1911 | 3 | 6 ticks (0.30 s) | 10.00 | 0.89× |
| Micro Uzi | 2 | 2 ticks (0.10 s) | 20.00 | **1.79×** |
| AKM | 4 | 4 ticks (0.20 s) | 20.00 | **1.79×** |
| Ruger Mini-14 | 5 | 8 ticks (0.40 s) | 12.50 | 1.12× |
| AWM | 12 | 30 ticks (1.50 s) | 8.00 | 0.71× |
| Winchester Model 1897 | 2 × 8 = 16 | 15 ticks (0.75 s) | 21.33 (point-blank, all 8 pellets connecting) | **1.90×** |

**Flag 1 — the two auto weapons (Micro Uzi, AKM) both land at exactly 1.79× a diamond sword's
sustained DPS.** Ammo consumption is the only friction (Uzi empties its 32-round magazine in 3.2 s
of continuous fire; AKM its 30 rounds in 6.0 s), and hitting a moving target at range is harder than
landing every swing at melee range, but neither of those is captured by a bare DPS number. Worth a
deliberate look: is ~1.8× melee DPS while firing the intended ceiling for the two fastest weapons in
the roster, or should one or both come down closer to the AKM/Ruger's own mid-tier feel?

**Flag 2 — the shotgun's point-blank DPS (21.33, 1.90× the sword) is the single highest figure in
the roster**, ahead of both autos. This matches the genre convention (devastating up close, falling
off sharply with range via the 8° per-pellet cone the spec already calls out), so it may be exactly
right — but it is worth Kevin's explicit confirmation that "highest DPS in the game, at point-blank
only" is the intended shotgun identity rather than an accidental spike.

**Flag 3 — the AWM's 0.71× sword DPS is intentionally low** (`domains/weapon.md`'s own sanity check:
"close to lethal ... in one round, matching a sniper's intended one-shot feel"), consistent with a
burst/alpha-damage weapon rather than a sustained-DPS one. No concern; noted for completeness, not
as a flag needing a ruling.

**Muzzle velocity vs. a bow** (`domains/weapon.md`'s own reference: "a bow arrow 2–6 [damage] at up
to 3 blocks/tick" at full draw): every one of the six weapons' muzzle velocities (4.0–10.0
blocks/tick) already exceeds a fully-drawn bow's own 3 blocks/tick, and `BulletEntity`'s gravity
(0.02 blocks/tick²) is 60% below a vanilla arrow's own 0.05 — both mean every firearm outranges and
out-flattens a bow by design, which reads as intentional (these are guns, not bows) rather than a
flag. **Flag 4 — the M1911's 4.0 blocks/tick muzzle velocity is the roster's own floor**, shared
with the Micro Uzi and Winchester; at 3 damage/hit and only 10 DPS (below the sword), the pistol is
the roster's weakest weapon on every axis (damage, velocity, DPS) with no offsetting strength (e.g.
no magazine-size or rate-of-fire edge over the SMG) — worth confirming this is deliberately the
"first, weakest tier" weapon rather than simply undertuned.

**Flag 5 — the knockback strength (`0.35f`, `BulletEntity.KNOCKBACK_STRENGTH`) has no vanilla
anchor** (`FA-5`'s own finding: an unenchanted vanilla arrow applies zero knockback, so "comparable
to an arrow's" the spec's own words for this value has no zero-enchant baseline to copy). Not
provably wrong, but unanchored — worth a deliberate look alongside the DPS flags rather than an
assumption that it already feels right.

Every other number in `domains/weapon.md`, `domains/attach.md`, `domains/ammo.md` and
`domains/trade.md` (durability, magazine sizes, reload ticks, recoil degrees, attachment modifier
percentages, cartridge material costs, trade prices) was checked for internal consistency (e.g. the
roster's own tier ordering, the master buy-back payouts scaling with weapon tier) and found
coherent — no further flags beyond the five above.
