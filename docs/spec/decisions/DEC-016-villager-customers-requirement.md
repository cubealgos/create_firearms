---
title: "create_firearms DEC-016 — create_villager_customers needs a component-predicate match rule; recorded, not scoped here"
type: "spec"
category: "create_firearms"
---

# `DEC-016` — `create_villager_customers` needs a component-predicate match rule; recorded, not scoped here

**Status:** decided by Kevin, 2026-09-20 (by ruling this a required cross-project change rather
than something `create_firearms` should work around unilaterally).

`create_villager_customers`'s own shop-matching rule (`StackShape`, a plain `(itemId, count)`
record; `TransactionExecutor.shapeOf` discards components when reading a live stack) treats two
differently-attached weapons of the same base item as identical today — a shop selling *any* AKM
would satisfy a villager wanting one specific configuration (research
`smithing-and-item-model-layers-26-2.md` §D.3, citing that mod's own source directly). This blocks
the full value of `decisions/DEC-013-master-buy-back.md`'s "ties in with villager customers"
framing: a villager cannot yet walk a master-buy-back weapon home from a table-cloth shop the way
it already can for a plain item (`create_villager_customers` `UC-001`).

**This mod does not implement the fix.** `create_villager_customers`'s `StackShape` needs extending
with a components predicate (or equivalent), and its `MatchRule` taught to honour it — a change to
that sibling's own domain and public surface, not something `create_firearms` can add from outside
without duplicating or forking that mod's matching logic. Recorded here as an explicit, named
requirement on that project rather than left as an unstated assumption behind
`decisions/DEC-013-master-buy-back.md`'s framing.

**What still works without it**: every other use case in this sheet, including the master buy-back
trade itself as a direct villager sale (`domains/trade.md` `TRADE-FAIL-003`). Only the
"villager then resells it at a shop" leg of the automation loop is blocked.

Alternative considered: `create_firearms` reading or duplicating `create_villager_customers`'s
matching logic directly, to close the loop without touching that mod. Rejected: it would create an
undeclared coupling between two mods neither depends on the other for anywhere else in this sheet,
and the correct fix belongs in the shop-matching domain that already owns this concept. Cost if
wrong: none for this mod — the requirement is purely informational until someone picks up the
ticket on `create_villager_customers`'s own tracker.
