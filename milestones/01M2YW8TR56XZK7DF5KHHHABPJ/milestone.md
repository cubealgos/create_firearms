---
schema_version: 1
id: 01M2YW8TR56XZK7DF5KHHHABPJ
key: M1
title: Model and data
status: todo
created_at: 2026-09-20T07:42:41Z
---

## Goal

The pure surface this whole mod is built on: a weapon's classes, calibres, slots and stats as
data, the pure function that derives final stats from a base plus present attachments, and the
seven data component types that carry a weapon's identity, ammo and slot state.

## Scope

The pure stat model and derivation function in `firearms.model` (classes, calibres, per-class slot
table, base stats, attachment modifiers, `stats(base, present slots) → final stats`,
`verifyPurePackage`, unit tests over every 1.0 weapon and every attachment, `FA-2`); the seven data
components (base, five per-slot attachment components, ammo; codecs; `DATA-REQ-001`–`005`
versioning) and the item registration with `max_damage`, no `REPAIRABLE`, no `ENCHANTABLE`
(`WEAPON-REQ-014`, `015`), the data files for the six 1.0 bases, 22 attachments and six calibres,
the loaders reading them, and plain crafting recipes for every one of them (`FA-3`); the anvil
combine-repair mixin closing `WEAPON-FAIL-006`'s residual gap and a game test proving both the
material-repair and the enchant paths refuse (`FA-4`). Not firing, not either attach recipe front
end, not trades (`M2`, `M3`, `M5`).

## Exit criteria

- The stat derivation function is unit-tested over every 1.0 weapon and every attachment, applying
  modifiers in the fixed slot order (`WEAPON-REQ-003`).
- `verifyPurePackage` passes over `firearms.model` with zero Minecraft, Fabric or Create imports.
- Every base weapon, attachment and cartridge has a working crafting-table recipe.
- A worn weapon is refused at the anvil (material repair) and the enchanting table; the same-item
  combine-repair question (`WEAPON-FAIL-006`) is settled by a game test, one way or the other.

## Tickets

FA-2, FA-3, FA-4.

## Depends on

M0 (the bootstrapped scaffold, `verifyPurePackage`, the empty mixin config).
