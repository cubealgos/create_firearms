---
schema_version: 1
id: 01M2YWDH1GVQZ9XQ1KZ9GHFD2G
key: FA-3
type: feat
title: Data components, item registration and data files for bases, attachments and calibres
created_by: kevin
created_at: 2026-09-20T07:45:15Z
---

## Scope

The seven `DataComponentType` registrations — `firearms:base` (`{ version, weapon_id }`),
`firearms:ammo` (`{ caliber, loaded }`), and one `firearms:attachment_<slot>` per slot
(`04-architecture.md` `ARCH-DEC-005`), with codecs and `DATA-REQ-001`–`005`'s forward-only,
one-shared-version migration and degrade-to-safe-default rules (`contracts/data-contract.md`); the
weapon item itself, registered with `Item.Properties.durability(int)` and explicitly **no**
`.repairable(...)` and **no** `.enchantable(...)` (`WEAPON-REQ-014`, `015`); the six 1.0 base
weapon data files, the 22 attachment data files, and the six cartridge items (`AMMO-REQ-002`,
`003`); the loaders reading weapon/attachment data files into `firearms.model` values
(`ACTORS-008`, `WEAPON-DEC-003`); a plain crafting-table recipe for every base weapon, every
attachment, and every cartridge (`WEAPON-REQ-006`, `ATTACH-REQ-007`, `AMMO-REQ-001`). Not the
derivation function itself (`FA-2`, already landed), not either attach recipe (`FA-7`, `FA-8`), not
firing (`FA-6`).

## Approach

Item and component registration follows `create_metered_motor`'s own `BuiltInRegistries
.DATA_COMPONENT_TYPE` pattern (research `create-fly-potato-cannon-and-deploying-26-2.md` §C.6, cited
by `04-architecture.md` `ARCH-DEC-005`) — no mixin needed for registration itself. Weapon and
attachment data files live under `data/firearms/weapon/` and `data/firearms/attachment/` (or the
registry-folder shape the public surface names, confirmed at this ticket), each naming a class,
calibre and base stats or a slot and modifiers respectively; a loader turns each file into the
`firearms.model` record `FA-2` already defines, so the model itself needs no change here. Exact
crafting-table grids and material quantities (open questions in `domains/weapon.md` §7,
`domains/attach.md` §7) are confirmed at this ticket, proposed as iron ingots, sticks and redstone
scaled roughly to each weapon's own stats, per `rulings-2026-09-20.md`.

## Acceptance criteria

- [ ] All seven component types round-trip through a codec test, including the malformed-value
      degrade-to-absent/degrade-to-no-ammo path (`DATA-REQ-004`).
- [ ] A weapon item stack never offers a repair result at a real `AnvilMenu.createResult()` against
      a repair material, and is never enchantable (`WEAPON-REQ-014`, `015` — the anvil's own
      same-item combine-repair path is `FA-4`'s own proof, not this ticket's).
- [ ] Every one of the six base weapons, 22 attachments and six cartridges craft at a real crafting
      table via their own data file.
- [ ] A datapack-added weapon or attachment in an existing class/slot loads with zero new Java
      (`WEAPON-DEC-003`, `SURFACE-REQ-003`).
- [ ] `just check` green.

## Constraints and prior findings

`docs/spec/04-architecture.md` `ARCH-DEC-005`, `docs/spec/contracts/data-contract.md`,
`docs/spec/contracts/public-surface.md`, `docs/spec/domains/weapon.md` `WEAPON-REQ-006`, `014`,
`015`, §7, `docs/spec/domains/attach.md` `ATTACH-REQ-007`, §7, `docs/spec/domains/ammo.md`
`AMMO-REQ-001`–`003`. Blocked by `FA-2`'s model records, which this ticket's loaders read into.
