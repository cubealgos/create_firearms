---
schema_version: 1
id: 01M2YWEKYTXQ6MBF6BMVNZT784
key: FA-13
type: feat
title: Weaponsmith and fletcher trade files, tags, and the master buy-back trades
created_by: kevin
created_at: 2026-09-20T07:45:51Z
---

## Scope

Tag-merge files at `data/minecraft/tags/villager_trade/{weaponsmith,fletcher}/level_N.json`
(`"replace": false`) appending this mod's own trade ids, plus the real trade JSONs under
`data/firearms/villager_trade/` — no new profession, job-site block or POI anywhere
(`TRADE-REQ-001`–`003`). The proposed catalog: weapons and attachments across the weaponsmith's
five levels, cartridges across the fletcher's four (`domains/trade.md` §3 table). Six master
buy-back trades at the weaponsmith's top level, one per 1.0 class, each `wants` a
`DataComponentExactPredicate` naming that class's base weapon plus its showcase attachment(s), every
other slot left unconstrained, `gives` an emerald payout, `max_uses: 1` (`TRADE-REQ-004`, `005`).
No telemetry, no dependency on `create_villager_customers` (`TRADE-REQ-006`). Settles the
no-duplicate-offer `merchant_predicate` open question (`domains/trade.md` §7). Not the item models
(`FA-9`, already landed).

## Approach

The tag-merge mechanism is `create_metered_motor`'s own toolsmith-trade pattern, reused as-is
(research `smithing-and-item-model-layers-26-2.md` §D.2). The buy-back predicate's structural
soundness — matching only the named slot(s), never inspecting an unconstrained slot — is already
confirmed by research §D.3; this ticket wires the JSON, it does not need to re-derive the mechanism.
Prices and the exact wanted-configuration-per-class table are the proposal in `domains/trade.md` §3;
retune at the balance sweep (`FA-14`), ship as proposed for now.

## Acceptance criteria

- [x] The tag-merge actually appears in a real weaponsmith's and fletcher's generated offers at the
      documented trade level, for every item in the catalog table.
- [x] A master buy-back trade's `ItemCost.test()` accepts a matching configuration (named slots
      correct, others free) and rejects a mismatched one, tested on both an unconstrained and a
      constrained slot.
- [x] No new villager profession, job-site block, or POI type is registered anywhere in this ticket
      (`TRADE-REQ-003`).
- [x] The no-duplicate-offer `merchant_predicate` question is answered (omit unless a duplicate-offer
      problem is actually observed, per this sheet's own proposal) and recorded in
      `domains/trade.md` §8.
- [x] `just check` green.

## Constraints and prior findings

`docs/spec/domains/trade.md` `TRADE-REQ-001`–`006`, `TRADE-DEC-001`, `002`, §7,
`docs/spec/decisions/DEC-012-villager-integration.md`, `docs/spec/decisions/DEC-013-master-buy-back.md`.
Not fully closed by this mod alone: `decisions/DEC-016-villager-customers-requirement.md` records a
required cross-project change on `create_villager_customers`'s own tracker, out of this ticket's
scope — the buy-back trade itself still works as a direct villager sale without it
(`TRADE-FAIL-003`). Blocked by `FA-7` (a real filled-slot weapon to test the predicate against).

## Findings

- **The spec's own `wants`/`gives` JSON example named a non-existent item.** `docs/spec/domains/trade.md`
  §3's "JSON shape" showed `"wants": {"id": "firearms:akm", ...}`, but `FA-9`'s landed code has one
  generic `firearms:weapon` item; a weapon's identity lives entirely in its own `firearms:base`
  component (`{version, weapon_id}`, `firearms.item.WeaponItem`). Every trade this ticket ships uses
  the real shape instead, e.g. the AKM master buy-back's `wants`:
  ```json
  { "id": "firearms:weapon", "count": 1,
    "components": { "firearms:base": { "weapon_id": "firearms:akm" },
                     "firearms:attachment_muzzle": "firearms:suppressor",
                     "firearms:attachment_optic": "firearms:scope_4x" } }
  ```
  Likewise attachments are one item per slot (`firearms:attachment_<slot>`), distinguished by that
  slot's own component carrying the specific attachment id — `gives` for a Suppressor sale is
  `{"id": "firearms:attachment_muzzle", "components": {"firearms:attachment_muzzle":
  "firearms:suppressor"}}`. Corrected in the vault spec (`TRADE-DEC-003`'s surrounding edit) and
  synced to `docs/spec/domains/trade.md`.
- **The `merchant_predicate` question is answered the opposite way from the sheet's own lean**,
  per this ticket's own brief overriding `domains/trade.md` §7's proposal to omit one:
  `firearms:no_firearm_offered` (`firearms.trade.NoFirearmOffered`, registered by
  `firearms.trade.TradeRegistration.register()`, called once from `Firearms.onInitialize()`) guards
  every bare-weapon sale trade (the six `emerald_<weapon>` trades at weaponsmith levels 1, 3 and 4)
  so a weaponsmith never holds more than one weapon-selling offer at once — item identity alone
  (`offer.getResult().getItem() == ItemRegistration.WEAPON`) is enough, since every base weapon
  shares that one item. Not applied to attachment or cartridge trades, and not applied to the six
  master buy-back trades themselves (those `want` a weapon rather than `give` one; the same
  duplicate-sale concern does not apply, and each class already has exactly one buy-back trade in
  the whole catalogue). Recorded as `TRADE-DEC-003` in the vault spec.
- **Trade counts per level** (40 trade files total): weaponsmith L1 = 2 (bare weapons), L2 = 3
  (muzzle attachments), L3 = 10 (2 bare weapons, 3 magazine, 5 grip attachments), L4 = 5 (2 bare
  weapons, 3 stock attachments), L5 = 14 (8 optics + 6 master buy-back trades); fletcher L1 = 2,
  L2 = 1, L3 = 2, L4 = 1 (cartridges).
- **`just check` green**: `lint` (`./gradlew check -x test -x runGameTest`) BUILD SUCCESSFUL;
  `map-check` 17 files current; `test` (`./gradlew test` + `tools/test_*.py`) BUILD SUCCESSFUL, 4
  Python tests OK; `gametest` (`./gradlew runGameTest`) "26 GAME TESTS COMPLETE ... All 26 required
  tests passed" (7 new: 1 `TradeFileGameTest`, 3 `NoDuplicateOfferGameTest`, 3
  `MasterBuyBackGameTest`, plus the 19 pre-existing tests, all green).
- **Commit** `ff29ff91b7cf065a4f55af728154463c5e866368` on `feature/fa-13-trades`, pushed to origin.
  Exact prices are the proposed catalogue per `domains/trade.md` §3/§7 — not retuned, deferred to
  `FA-14`'s balance sweep as the ticket's own Approach section directs.
