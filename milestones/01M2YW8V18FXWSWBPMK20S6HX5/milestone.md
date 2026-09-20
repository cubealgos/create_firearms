---
schema_version: 1
id: 01M2YW8V18FXWSWBPMK20S6HX5
key: M6
title: Release
status: backlog
created_at: 2026-09-20T07:42:41Z
---

## Goal

Prove the mechanism is solid and make it a real Modrinth listing: every requirement mapped to a
test, three clean `just check` runs, a client checklist done by Kevin, real assets, and the
`1.0.0+26.2` release.

## Scope

The requirement-to-test table across every `WEAPON-REQ`, `ATTACH-REQ`, `AMMO-REQ`, `COMBAT-REQ`,
`TRADE-REQ` and `UI-REQ` id, three consecutive green `just check` runs, and Kevin's client
checklist (`FA-14`); Modrinth assets — the navy badge icon rendered from an in-game asset of this
mod's own (the AKM item model if the block-model renderer applies, else the M1911 sprite), the
listing body, and the gallery shot list (`FA-15`); the `1.0.0+26.2` release build and its
publication, stating plainly that every stat and price in the sheet is a proposed starting point,
not a balanced final number (`REL-REQ-003`) (`FA-16`). Not the rest of the roster or JEI (`M7`).

## Exit criteria

- Every requirement id in the six domain files is mapped to a test or a recorded ruling.
- `just check` green three consecutive times on a clean checkout.
- Kevin's client checklist done.
- Published on Modrinth with real assets, and a tagged `1.0.0+26.2` release with its notes and
  checksum.

## Tickets

FA-14, FA-15, FA-16.

## Depends on

M4 (a complete, playable client experience) and M5 (the trades the listing itself describes).
