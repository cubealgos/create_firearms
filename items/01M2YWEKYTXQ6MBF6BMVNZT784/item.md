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

- [ ] The tag-merge actually appears in a real weaponsmith's and fletcher's generated offers at the
      documented trade level, for every item in the catalog table.
- [ ] A master buy-back trade's `ItemCost.test()` accepts a matching configuration (named slots
      correct, others free) and rejects a mismatched one, tested on both an unconstrained and a
      constrained slot.
- [ ] No new villager profession, job-site block, or POI type is registered anywhere in this ticket
      (`TRADE-REQ-003`).
- [ ] The no-duplicate-offer `merchant_predicate` question is answered (omit unless a duplicate-offer
      problem is actually observed, per this sheet's own proposal) and recorded in
      `domains/trade.md` §8.
- [ ] `just check` green.

## Constraints and prior findings

`docs/spec/domains/trade.md` `TRADE-REQ-001`–`006`, `TRADE-DEC-001`, `002`, §7,
`docs/spec/decisions/DEC-012-villager-integration.md`, `docs/spec/decisions/DEC-013-master-buy-back.md`.
Not fully closed by this mod alone: `decisions/DEC-016-villager-customers-requirement.md` records a
required cross-project change on `create_villager_customers`'s own tracker, out of this ticket's
scope — the buy-back trade itself still works as a direct villager sale without it
(`TRADE-FAIL-003`). Blocked by `FA-7` (a real filled-slot weapon to test the predicate against).
