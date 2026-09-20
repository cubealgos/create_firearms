---
schema_version: 1
id: 01M2Z2FDVZ2S17WD6EMZJWQ4N6
key: FA-20
type: chore
title: Five game test classes are not registered in the gametest entrypoint after the parallel merges
created_by: kevin
created_at: 2026-09-20T09:31:09Z
---

## Scope

Five game test classes on `development` are not listed in `src/gametest/resources/fabric.mod.json`'s gametest entrypoint (`DebugCommandGameTest`, `FiringGameTest`, `MasterBuyBackGameTest`, `NoDuplicateOfferGameTest`, `TradeFileGameTest`): each parallel branch added its own line and the merges kept only one side, so 48 annotated test methods exist but 32 ran. Register every class and prove the count.

## Approach

A script registers every `@GameTest` class found on disk; the merge tooling runs it after every merge from now on.

## Acceptance criteria

- [ ] Every `@GameTest` class under `src/gametest/java` is listed in the entrypoint; `just check` runs all of them.
- [ ] Merged through a Forgejo pull request into `development`.

## Constraints and prior findings

FA-8's Findings first noticed two unregistered classes; the parallel merges reintroduced the gap.
