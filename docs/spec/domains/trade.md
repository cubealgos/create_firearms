---
title: "create_firearms spec — TRADE: weaponsmith, fletcher, and the master buy-back"
type: "spec"
category: "create_firearms"
---

# `TRADE` — villager trades with no new profession

## 1. Purpose

How weapons, attachments and ammunition sell through the two existing villager professions; the
tag-merge mechanism that adds them with zero new profession, job site or POI; the master buy-back
trades and the component predicate that makes them want a specific configuration. Not the weapon or
attachment items themselves (`domains/weapon.md`, `domains/attach.md`).

## 2. Dimensions

| Dimension | Answer |
|---|---|
| **Actors** | The weaponsmith villager (`ACTORS-004`) sells weapons/attachments and runs the buy-back trades; the fletcher villager (`ACTORS-005`) sells ammunition; the player (`ACTORS-001`) buys and, at the top level, sells back. |
| **Over time** | A trade exists the moment the villager's trades are generated at its current level, exactly like any vanilla trade; the buy-back trades only appear once a villager reaches the weaponsmith's top level, the same as any vanilla level-gated trade. |
| **Multiplicity** | One tag-merge file per touched trade level, per profession; six master buy-back trades at 1.0, one per class. A datapack adding a new weapon in `WEAPON-DEC-003`'s shape may also add its own trade entries the identical way. |
| **Unwanted** | Offering a weapon with the wrong configuration to a buy-back trade (the cost predicate simply does not match, `TRADE-FAIL-001`); two mods both merging the same trade level tag (ordinary tag-union behaviour, not a conflict). |
| **Not-you** | A player who never visits a weaponsmith or fletcher never sees a trade of this mod's own; the two villagers' other vanilla trades (axes, swords, bows, arrows) are untouched. |

## 3. Enumerations

### The tag-merge mechanism

`create_metered_motor` already ships this exact pattern for its own toolsmith trade (research
`smithing-and-item-model-layers-26-2.md` §D.2): a `"replace": false` file at
`data/minecraft/tags/villager_trade/<profession>/level_N.json` inside this mod's own resources,
appending this mod's own trade id — vanilla's datapack loader unions same-path tag files from every
loaded pack rather than overwriting — plus the real trade JSON under
`data/firearms/villager_trade/<profession>/N/*.json`. No new profession, job-site block, or POI
registration anywhere.

### Proposed trade catalog (prices proposed, retune at the sweep)

| Profession | Level | Sells | Notes |
|---|---|---|---|
| Weaponsmith | 1 | M1911, Winchester Model 1897 (bare, no attachments) | The two cheapest-to-craft 1.0 weapons |
| Weaponsmith | 2 | Suppressor, compensator, flash hider (muzzle) | |
| Weaponsmith | 3 | Micro Uzi, AKM (bare); extended magazine, quickdraw, extended quickdraw | |
| Weaponsmith | 3 | Grip attachments (vertical, angled, half, light, thumb) | |
| Weaponsmith | 4 | Ruger Mini-14, AWM (bare); stock attachments (tactical stock, cheek pad, bullet loops) | |
| Weaponsmith | 5 | All eight optics (red dot through 15x), priced by tier; **the six master buy-back trades** | Buy-back trades only exist at this level |
| Fletcher | 1 | `.45 ACP`, `9mm` cartridges | Pistol/SMG calibres, the most common |
| Fletcher | 2 | `12 gauge` shells | |
| Fletcher | 3 | `7.62mm`, `5.56mm` cartridges | |
| Fletcher | 4 | `.300 Magnum` cartridges | Rarest calibre, priciest |

### The master buy-back trades

One per 1.0 weapon class, `max_uses: 1`, `gives` an emerald payout scaled roughly to the weapon's
own crafting cost. `wants` names the class's 1.0 weapon plus one or two attachments chosen to best
show off that class — a scope and a suppressor for the sniper rifle, a choke only for the shotgun,
since a shotgun has no optic, grip, or stock slot to name.

| Class | Weapon | Wanted configuration | Payout (emeralds, proposed) |
|---|---|---|---|
| Pistol | M1911 | suppressor | 24 |
| SMG | Micro Uzi | suppressor, extended magazine | 32 |
| Assault rifle | AKM | suppressor, 4x optic | 48 |
| DMR | Ruger Mini-14 | 6x optic, tactical stock | 48 |
| Sniper rifle | AWM | suppressor, 8x optic | 64 |
| Shotgun | Winchester Model 1897 | compensator | 40 |

The predicate constrains only the named slot(s); the magazine, grip and stock (where the class has
them) stay free — proven structurally, since `DataComponentExactPredicate.test()` only ever calls
`stack.get(type)` for the component types its own list names, never for the full stack (research
`smithing-and-item-model-layers-26-2.md` §D.3).

### JSON shape

**Correction (`FA-13`)**: every 1.0 base weapon is a stack of the single `firearms:weapon` item,
distinguished only by its own `firearms:base` component (`docs/spec/contracts/public-surface.md`,
`firearms.item.WeaponItem`) — there is no per-weapon item id such as `firearms:akm` to name in
`wants`/`gives` directly. The shape below is the real one, landed by `FA-13`:

```json
{ "wants": { "id": "firearms:weapon", "count": 1,
             "components": { "firearms:base": { "weapon_id": "firearms:akm" },
                              "firearms:attachment_muzzle": "firearms:suppressor",
                              "firearms:attachment_optic": "firearms:scope_4x" } },
  "gives": { "id": "minecraft:emerald", "count": 48 }, "max_uses": 1 }
```

A bare-weapon sale trade's own `gives` carries the identical `firearms:base` component plus the
weapon's own `minecraft:max_damage`/`minecraft:damage` pair, mirroring that weapon's crafting
recipe output exactly (`data/firearms/recipe/weapon/*.json`); an attachment sale trade's `gives`
names one of the five `firearms:attachment_<slot>` items with that slot's own component set to the
specific attachment id, e.g. `{"id": "firearms:attachment_muzzle", "components":
{"firearms:attachment_muzzle": "firearms:suppressor"}}`.

## 4. Use cases

`UC-015`–`UC-017` in `02-journeys.md`.

## 5. Requirements

| ID | Requirement | Priority | From |
|---|---|---|---|
| `TRADE-REQ-001` | The system shall add weapon and attachment trades to the weaponsmith's existing trade levels via tag-merge files, `gives` carrying each weapon's `firearms:base` component (bare, no attachments) where applicable. | Must | `rulings-2026-09-20.md` |
| `TRADE-REQ-002` | The system shall add cartridge trades to the fletcher's existing trade levels the identical way. | Must | `rulings-2026-09-20.md` |
| `TRADE-REQ-003` | The system shall register no new villager profession, job-site block, or point-of-interest type. | Must | `decisions/DEC-012-villager-integration.md` |
| `TRADE-REQ-004` | The system shall add six master buy-back trades at the weaponsmith's top trade level, one per 1.0 weapon class, each `wants` a component predicate naming that class's base weapon plus the class's showcase attachment(s) and leaving every other slot unconstrained, `gives` an emerald payout, `max_uses: 1`. | Must | Kevin, 2026-09-20: "villagers to get the trade to buy very specific weapons at their final stage for a lot of emeralds"; `UC-017` |
| `TRADE-REQ-005` | The buy-back trades' component predicate shall match only the named slot component(s) by whole-value equality, and shall never inspect an unconstrained slot's component at all. | Must | Research `smithing-and-item-model-layers-26-2.md` §D.3 |
| `TRADE-REQ-006` | The system shall carry no telemetry, no external call, and no dependency of any kind on `create_villager_customers`: the buy-back trades work as a plain villager sale on their own, with or without that sibling mod installed. | Must | `decisions/DEC-016-villager-customers-requirement.md` |

## 6. Failure modes

| ID | Failure | Response |
|---|---|---|
| `TRADE-FAIL-001` | A player offers a weapon whose configuration does not match a buy-back trade's predicate | `ItemCost.test()` fails; the trade shows as unavailable through the ordinary "not enough/wrong item" trade-screen cue, the same as any unmet vanilla trade — no bespoke error. |
| `TRADE-FAIL-002` | A player wants to know exactly what a buy-back trade requires before building it | Reads the trade's own rendered cost-slot item and its tooltip; `MerchantScreen` has no component-aware "this trade also requires X" line, confirmed by research §D.3 — the mod's own README/description is the other place this is documented (`domains/ui.md`). |
| `TRADE-FAIL-003` | A player wants a villager to walk a buy-back weapon home from a table-cloth shop, as `create_villager_customers` would for a plain item | Does not yet work: that sibling's own match rule cannot distinguish two differently-configured weapons of the same base item today (`decisions/DEC-016-villager-customers-requirement.md`). The buy-back trade itself still works as a direct sale to any weaponsmith. |

## 7. Open questions

| Question | Blocks | Decided by |
|---|---|---|
| Exact trade prices across both professions and all levels | `TRADE-REQ-001`, `002` | first ticket, balance sweep; `FA-13` ships the catalogue's proposed prices as-is |

Resolved by `FA-13`: whether a `merchant_predicate` duplicate-offer guard is worth adding — see
`TRADE-DEC-003`, which overturns this sheet's own earlier lean toward omitting one.

## 8. Decisions

- `TRADE-DEC-001` — **Weaponsmith sells weapons and attachments, fletcher sells ammunition, no new
  profession** (Kevin, 2026-09-20, accepting the argument against a gunsmith profession: "a new
  profession needs a new job-site block and POI registration, which reintroduces exactly the
  new-block cost the workstation decision just avoided"). **Cost if wrong:** none identified — a
  dedicated profession remains addable later as a pure superset, since nothing here depends on the
  two existing professions being exclusive.
- `TRADE-DEC-002` — **Master buy-back trades use per-slot component predicates, leaving
  unconstrained slots free** (this sheet's mechanism for Kevin's ruling, confirmed structurally
  sound by research §D.3). The full component-shape reasoning lives in `04-architecture.md`
  `ARCH-DEC-005`; restated here as the trade-domain consumer of that shape.
- `TRADE-DEC-003` — **A `firearms:no_firearm_offered` merchant predicate guards every
  bare-weapon-selling trade, so a weaponsmith never holds more than one weapon offer at once**
  (`FA-13`'s own ticket brief, settling §7's open question the opposite way from this sheet's
  original lean toward omitting a guard). Modelled directly on `create_metered_motor`'s
  `no_motor_offered` condition (research `smithing-and-item-model-layers-26-2.md` §D.2):
  `firearms.trade.NoFirearmOffered` inspects the merchant's live offers and refuses a further
  weapon-selling trade once any offer already gives a `firearms:weapon` stack — item identity
  alone is enough, since every 1.0 base weapon shares that one item, distinguished only by its own
  `firearms:base` component. The guard sits on the bare-weapon sale trades only (the six
  `emerald_<weapon>` trades at weaponsmith levels 1, 3 and 4), never on an attachment or cartridge
  trade, and never on the master buy-back trades themselves — a buy-back trade `wants` a weapon
  rather than giving one, so the same duplicate-sale concern does not apply to it, and each class
  has exactly one buy-back trade in the whole catalogue regardless. **Reason to override the
  sheet's own "most vanilla trades omit one" lean**: with weapon-selling trades spread across three
  separate weaponsmith levels (an SMG at level 1, two more classes at level 3, two more at level 4),
  a villager reaching level 4+ could otherwise draw several different weapons for sale
  simultaneously; the guard keeps a weaponsmith's weapon stock to at most one offer, matching a
  merchant that specializes rather than stocks the whole catalogue. **Cost if wrong:** the
  predicate is one `merchant_predicate` field per trade file plus one loot condition class; dropping
  it later is a data-only edit to the affected trade JSONs, no Java change needed to remove the
  behaviour (only to remove the now-unused registration).
