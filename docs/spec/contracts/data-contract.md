---
title: "create_firearms spec — data contract: seven components, one shared version"
type: "spec"
category: "create_firearms"
---

# Data contract (`DATA`)

## What this mod persists

Only what lives on a weapon item's own data components, exactly the seven types
`04-architecture.md` `ARCH-DEC-005` defines:

```
firearms:base               { version: int, weapon_id: Identifier }
firearms:ammo                { caliber: Identifier, loaded: int }
firearms:attachment_muzzle   <attachment id>, or absent
firearms:attachment_optic    <attachment id>, or absent
firearms:attachment_magazine <attachment id>, or absent
firearms:attachment_grip     <attachment id>, or absent
firearms:attachment_stock    <attachment id>, or absent
```

No memory module, no capability, no block entity, no world-save file of this mod's own exists
anywhere — attaching, firing, reloading and trading all read and write only these seven component
types on the item stacks already involved, and attaching only ever sets a slot component, never
clears one (`decisions/DEC-017-no-detach-durability.md`). Bullet entities are transient and never
persisted across a save (they either resolve or despawn within seconds, `domains/combat.md`).

## Rules

| ID | Rule |
|---|---|
| `DATA-REQ-001` | `firearms:base.version` shall be the single schema version governing all seven component types together: a weapon's full component set is read and migrated as one unit, mirroring `create_metered_motor`'s own single-versioned `stats` component rather than versioning each slot component independently. |
| `DATA-REQ-002` | On loading an item whose `firearms:base.version` is older than the mod's current schema, the system shall apply a forward-only migration to every present component of this mod's own, exactly once, on first read. | 
| `DATA-REQ-003` | On loading an item whose `firearms:base.version` is newer than the mod's current schema (a downgrade scenario), the system shall treat the weapon as a bare, unmodifiable item rather than crash or silently reinterpret unknown fields — logged once. |
| `DATA-REQ-004` | A malformed or unreadable `firearms:attachment_<slot>` value shall degrade to "slot empty," and a malformed or unreadable `firearms:ammo` value shall degrade to "no ammo loaded" — never a crash, mirroring `create_villager_customers`'s own degrade-to-safe-default pattern for its memory modules. |
| `DATA-REQ-005` | No component, memory module, capability, or NBT tag of this mod's own shall be written to any entity, block entity, or world-save file: item stack components are the only persisted state. |

## Versioning: why one shared version, not seven

`create_villager_customers`'s two memory-module values needed no version at all, because they carry
no economic identity — losing one degrades to "no trip in progress," never to a wrong stat
(`vault/projects/create_villager_customers/spec/contracts/data-contract.md`). A firearm's
components are the opposite: `firearms:base`'s `weapon_id` and every attachment slot's identity are
exactly the kind of durable, economically-meaningful state `create_metered_motor`'s versioned
`stats` component protects. Splitting that one version across seven component types would let a
weapon's base and its attachments drift out of sync mid-migration; keeping one version on `base`
and migrating every present component together, in one pass, keeps the seven types acting as one
logical schema despite being seven separate `DataComponentType` registrations (`04-architecture.md`
`ARCH-DEC-005`'s own reason for the split is orthogonal to this: engine constraints on item models
and trade predicates, not a reason to version them independently too).

## Out of scope (sheet §8)

No record of past shots, past trades, or past attach operations anywhere: the only evidence
a weapon has been fired is its own reduced durability and ammo count, exactly as a vanilla tool's
wear is the only record of its own use. No import of another mod's weapon or attachment state; no
export.
