---
title: "create_firearms DEC-013 — Master buy-back trades want a specific configuration for many emeralds"
type: "spec"
category: "create_firearms"
---

# `DEC-013` — Master buy-back trades want a specific configuration for many emeralds

**Status:** decided by Kevin, 2026-09-20.

Kevin, verbatim: "I would like for the villagers to get the trade to buy very specific weapons at
their final stage for a lot of emeralds, so the player can have late game emerald automation on
mass; also ties in with metered motor and villager customers." The weaponsmith's top trade level
carries six master buy-back trades, one per 1.0 weapon class, each `wants` a component predicate
naming that class's base weapon plus one or two showcase attachments, leaving every other slot
unconstrained, and pays many emeralds, `max_uses: 1` (`domains/trade.md` `TRADE-REQ-004`, `005`).
This depends directly on `decisions/DEC-011-per-slot-components.md`'s component shape: research
confirmed `ItemCost` carries a real `DataComponentExactPredicate` and that its matching is sparse
across component types but whole-value within one, which is exactly what a per-slot shape needs and
a combined map could not express (research `smithing-and-item-model-layers-26-2.md` §D.3).

**Not fully closed by this mod alone**: the "ties in with villager customers" half of Kevin's own
framing needs that sibling mod's own match rule taught to honour component predicates too — a
required cross-project change, recorded separately and out of this mod's own scope
(`decisions/DEC-016-villager-customers-requirement.md`). The buy-back trade itself works as a plain
villager sale today, with or without that change landing.

Alternative considered: a buy-back trade constrained only by item id and count, ignoring
attachments entirely. Rejected: it would let a bare, unattached weapon satisfy the same trade as a
fully-kitted one, defeating the entire "very specific weapons at their final stage" framing of
Kevin's own ruling. Cost if wrong: none identified — the per-slot predicate shape is already the
only one research confirms is technically capable of this at all.
