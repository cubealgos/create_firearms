---
schema_version: 1
id: 01M2YWF140Q34DGWWC4H00QD6M
key: FA-15
type: docs
title: "Modrinth assets: navy badge from an in-game asset, listing body, gallery shot list"
created_by: kevin
created_at: 2026-09-20T07:46:04Z
---

## Scope

`tools/icon.py`, rendering the cubealgos navy badge from an in-game asset of this mod's own — the
AKM item model rendered by the block-model renderer if that renderer applies to this mod's own item
models, else the M1911 sprite (`docs/modrinth/body.md`'s own placeholder note). The listing body
(`docs/modrinth/body.md`'s "Body" section, from `00-context.md` and the domain files), the gallery
shot list, and confirming the Modrinth slug `firearms` is still free at publish time (checked `404`
against `https://api.modrinth.com/v2/project/firearms` at bootstrap; re-check here since slugs can
be claimed between bootstrap and release). Not the release build itself (`FA-16`).

## Approach

Follow `create_synthetic_diamonds`'s own `SD-6` icon pipeline shape (`just icon` renders the badge),
adapted to this mod's own renderer needs — confirm at this ticket whether the block-model renderer
`create_metered_motor`'s own icon tooling uses applies to an item model, or whether the fallback
(the M1911 sprite, rendered the simpler item-sprite way `create_villager_customers`'s own icon
tooling uses) is needed instead. The listing body states plainly that every stat and price is a
proposed starting point, not a balanced final number (`REL-REQ-003`), and that every weapon uses
only its real-world designation (`operations/compliance.md`).

## Acceptance criteria

- [x] `just icon` renders `docs/modrinth/icon.png` on the cubealgos navy badge from an in-game
      asset of this mod's own, no third-party or Mojang texture copied.
- [x] `docs/modrinth/body.md`'s "Body" section is written, covering the roster, the attach
      mechanism, and combat, with the proposed-numbers disclosure and the compliance statement.
- [x] A gallery shot list is recorded (crafting, attaching, firing, scoped view, a trade screen).
- [x] The Modrinth slug `firearms` is re-confirmed free (or `create-firearms` substituted if
      claimed in the meantime) before publish.
- [x] `just check` green.

## Constraints and prior findings

`docs/modrinth/body.md` (bootstrap placeholder, `FA-1`), `docs/spec/operations/release.md`
`REL-REQ-003`, `docs/spec/operations/compliance.md` `COMP-REQ-002`. Blocked by `FA-14` (the
balance-swept, client-checked mod this listing describes).
