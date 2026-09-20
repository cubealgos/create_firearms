---
title: "create_firearms spec — glossary"
type: "spec"
category: "create_firearms"
---

# 03 — Glossary

| Term | Means |
|---|---|
| **base weapon** | An item carrying only `firearms:base` (weapon id, schema version) and, once loaded, `firearms:ammo`; no attachment slot component present. What a fresh craft produces (`domains/weapon.md`). |
| **class** | One of pistol, SMG, assault rifle, DMR, sniper rifle, shotgun; fixes which attachment slots a weapon has (`domains/weapon.md` §3). |
| **calibre** | The ammunition type a weapon accepts (`.45 ACP`, `9mm`, `7.62mm`, `5.56mm`, `.300 Magnum`, `12 gauge` at 1.0); a weapon only loads a cartridge of its own calibre (`domains/ammo.md`). |
| **slot** | One of muzzle, optic, magazine, grip, stock; which slots a given class has is fixed by the per-class slot table (`domains/weapon.md` §3); each slot holds at most one attachment. |
| **attachment** | An item that occupies one slot on a weapon it is compatible with, modifying one or more derived stats; universal across every class that has the slot (`rulings-2026-09-20.md`, `domains/attach.md`). |
| **stat derivation function** | The pure function `stats(base component, present slot components) → final stats`, re-run every time stats are needed (firing, tooltip, trade display); never baked onto the item (`domains/weapon.md` `WEAPON-REQ-003`). |
| **attach function** | The shared merge logic both the smithing recipe and the deploying recipe call: given a base weapon and an attachment, returns the reconfigured weapon. One-directional only — attachments never come off, so this function never displaces or clears a slot (`domains/attach.md` `ATTACH-DEC-001`; Kevin, 2026-09-20, `decisions/DEC-017-no-detach-durability.md`). |
| **`firearms:base`** | The base identity component: `{ version, weapon_id }`. Present on every weapon of this mod; absent means "not a weapon of this mod's own." |
| **`firearms:ammo`** | The loaded-ammunition component: `{ caliber, loaded }`. Present only once a weapon has been loaded at least once (`domains/weapon.md` `contracts/data-contract.md`). |
| **`firearms:attachment_<slot>`** | One data component type per slot (`_muzzle`, `_optic`, `_magazine`, `_grip`, `_stock`), holding the attached attachment's id, or absent when the slot is empty. Once set, never cleared or changed for the life of the weapon (`04-architecture.md`; `decisions/DEC-017-no-detach-durability.md`). |
| **smithing path** | The player-facing attach front end: a custom `Recipe<SmithingRecipeInput>` implementing `SmithingRecipe`, found by the vanilla smithing table with zero mixin (`04-architecture.md` `ARCH-DEC-002`; research `smithing-and-item-model-layers-26-2.md` §A.3). |
| **deploying path** | The contraption-facing attach front end: a custom `Recipe<ItemApplicationInput>` reporting `getType() == AllRecipeTypes.DEPLOYING`, found by a Create deployer with zero mixin (`04-architecture.md` `ARCH-DEC-003`; research `create-fly-potato-cannon-and-deploying-26-2.md` §B.3). |
| **bullet** | This mod's projectile entity: gravity-affected, per-tick swept-segment hit detection along its full movement vector (`ProjectileUtil.getHitResultOnMoveVector`), a short fixed life, then despawn (`domains/combat.md`). Kevin's ruling, against the research's hitscan recommendation. |
| **pellet** | One of several bullet entities a shotgun spawns per trigger pull, each independently spread and hit-tested (`domains/combat.md` `COMBAT-REQ-005`). |
| **cooldown group** | The vanilla `ItemCooldowns` key (defaulting to the weapon's own item id) this mod's fire-rate pacing rides on, the same mechanism the potato cannon uses (research §A.2, §C.1). |
| **fire mode** | `semi`, `auto`, or `pump`; a base-weapon-level constant fixing how the fire control behaves (`domains/weapon.md` §3). |
| **scoping** | The vanilla `Player.isScoping()` state, widened by one client mixin to also cover a firearm with a zoom-bearing optic attached while aiming; gates the FOV zoom, the overlay texture, and held-item render suppression, all three at once (`domains/combat.md` `COMBAT-REQ-006`; research `smithing-and-item-model-layers-26-2.md` §C.2). |
| **zoom-bearing optic** | An optic attachment whose zoom factor is greater than 1: the six magnified tiers (2x–15x). Red dot and holo have no zoom factor and never trigger scoping (`domains/combat.md` `COMBAT-DEC-002`). |
| **aim assist** | The spread-tightening every optic (including red dot and holo) applies while aiming, independent of whether it also zooms (`domains/weapon.md` `WEAPON-REQ-005`). |
| **weaponsmith** | The existing vanilla villager profession this mod adds trades to for weapons and attachments; no new profession, job site, or POI (`domains/trade.md`). |
| **fletcher** | The existing vanilla villager profession this mod adds trades to for ammunition. |
| **master buy-back trade** | A weaponsmith trade at its top level whose cost side (`wants`) names a specific base weapon plus one or more specific slot components via `DataComponentExactPredicate`, leaving every other slot unconstrained, paying many emeralds (`domains/trade.md` `TRADE-REQ-005`; research `smithing-and-item-model-layers-26-2.md` §D.3). |
| **cartridge** | The ammunition item for one calibre; crafted from a casing, a propellant and a projectile at a plain crafting table (`domains/ammo.md`). |
| **casing** | `create:brass_sheet`, reused as-is from Create Fly's own brass chain rather than a new item of this mod's own (`domains/ammo.md` `AMMO-DEC-001`). |
| **`REPAIRABLE`** | The vanilla `DataComponents.REPAIRABLE` component (`Item.Properties.repairable(...)`), naming which items repair an item at an anvil; never set on a weapon of this mod's own, so `ItemStack.isValidRepairItem` always returns false for it (`domains/weapon.md` `WEAPON-REQ-014`; Kevin, 2026-09-20, `decisions/DEC-017-no-detach-durability.md`). |
| **`ENCHANTABLE`** | The vanilla `DataComponents.ENCHANTABLE` component (`Item.Properties.enchantable(...)`), gating `ItemStack.isEnchantable()`; never set on a weapon of this mod's own, so the enchanting table offers it nothing (`domains/weapon.md` `WEAPON-REQ-015`; Kevin, 2026-09-20, `decisions/DEC-017-no-detach-durability.md`). |
