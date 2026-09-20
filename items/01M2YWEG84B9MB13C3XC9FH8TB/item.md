---
schema_version: 1
id: 01M2YWEG84B9MB13C3XC9FH8TB
key: FA-12
type: feat
title: Dev-only /firearms debug give <base> [attachments...] and /firearms debug stats
created_by: kevin
created_at: 2026-09-20T07:45:47Z
---

## Scope

Two development-environment-only commands, registered only when Fabric reports a dev environment,
mirroring every sibling's own debug command shape: `/firearms debug give <base>
[attachments...]` gives the caller a weapon of the named base with the named attachments already
attached (bypassing the smithing/deploying front ends, for fast iteration); `/firearms debug
stats <item>` (or the held item, if no argument) prints the item's derived final stats and full
component state without hovering (`UI-REQ-006`). Not the tooltip itself (`FA-11`, already landed).

## Approach

Registered in a `firearms.debug` package, guarded by `FabricLoader.getInstance().isDevelopmentEnvironment()`,
the same gate every sibling's own debug command uses. `debug give` calls `FA-7`'s shared attach
function directly per named attachment, in the fixed slot order, rather than reimplementing
component assembly. `debug stats` calls the same live stat function the tooltip and firing both
use.

## Acceptance criteria

- [ ] Neither command is registered outside a development environment (proven by a game test or an
      explicit environment-gate unit test, mirroring the siblings' own pattern).
- [ ] `/firearms debug give akm suppressor scope_4x` produces an AKM with exactly those two slots
      filled and every other slot empty.
- [ ] `/firearms debug stats` prints the same numbers the tooltip renders for an identical item.
- [ ] `just check` green.

## Constraints and prior findings

`docs/spec/domains/ui.md` `UI-REQ-006`, `docs/spec/operations/testing.md` (development tool row).
Blocked by `FA-7` (calls the shared attach function to assemble a debug weapon).
