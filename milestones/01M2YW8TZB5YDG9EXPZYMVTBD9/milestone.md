---
schema_version: 1
id: 01M2YW8TZB5YDG9EXPZYMVTBD9
key: M5
title: Trades
status: backlog
created_at: 2026-09-20T07:42:41Z
---

## Goal

Weapons, attachments and ammunition sell through the existing weaponsmith and fletcher, with a
master buy-back trade that wants a specific, fully-configured weapon.

## Scope

Weaponsmith and fletcher trade files and the tag-merge files at
`data/minecraft/tags/villager_trade/{weaponsmith,fletcher}/level_N.json`, with component-bearing
`gives` for weapons; the six master buy-back trades at the weaponsmith's top level, each `wants` a
component predicate naming that class's 1.0 weapon plus its showcase attachment(s) and leaving
every other slot free; the no-duplicate-offer merchant predicate question settled one way or the
other (`FA-13`). No new profession, job site or POI anywhere (`TRADE-REQ-003`).

## Exit criteria

- The tag-merge actually appears in a real weaponsmith's/fletcher's generated offers at the right
  trade level.
- A master buy-back trade's `ItemCost.test()` accepts a matching configuration and rejects a
  mismatched one, on both an unconstrained and a constrained slot.
- No new profession, job-site block, or POI type is registered anywhere in this mod.

## Tickets

FA-13.

## Depends on

M3 (a weapon that actually carries the component shape a buy-back trade's predicate names).
