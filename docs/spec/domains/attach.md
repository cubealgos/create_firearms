---
title: "create_firearms spec — ATTACH: attachments, slots, and the two attach front ends"
type: "spec"
category: "create_firearms"
---

# `ATTACH` — attachments and the shared attach function

## 1. Purpose

The 22 attachment items and their stat modifiers; which slots exist per class (restated from
`domains/weapon.md` §3 as the constraint this domain enforces); the smithing recipe and the
deploying recipe that both call one shared attach function. Not the base weapon items themselves or
the stat derivation function that reads these modifiers (`domains/weapon.md`), and not what a
bullet does once fired (`domains/combat.md`).

**Attachments never come off** (Kevin, 2026-09-20: "they don't come off; a weapon is crafted and
done"; `decisions/DEC-017-no-detach-durability.md`). There is no detach path, by either front end,
at 1.0 or at any later version this sheet currently anticipates. **This sheet's own reading of
"crafted and done"** — stated here plainly for Kevin to confirm or correct — is that it applies to
a *filled* slot only: once an attachment is on, it is permanent, but an *empty* slot on an
already-crafted weapon stays fillable at any time. Filling a previously-empty slot later, not
swapping a filled one, is the upgrade path the rulings' own "easy upgrade paths" and "swappable and
transferable" language pointed at before this ruling landed.

## 2. Dimensions

| Dimension | Answer |
|---|---|
| **Actors** | The player (`ACTORS-001`) attaches at a smithing table; the deployer (`ACTORS-003`) does the identical thing on a contraption; the server (`ACTORS-002`) matches and assembles both recipe types through one shared function. |
| **Over time** | An attachment is crafted or bought → attached into an empty slot on some weapon → permanent for the life of that weapon. A weapon may gain attachments in its other, still-empty slots at any later time, in any order, but a filled slot never changes again. |
| **Multiplicity** | Zero to five attachments on one weapon at once, one per slot, gated by the weapon's class; a slot fills at most once, ever. 22 attachment items exist at 1.0 across 5 slots; a datapack may add more per slot with zero new Java, the same way `WEAPON-DEC-003` covers new weapons. |
| **Unwanted** | Attaching into an already-occupied slot (the recipe simply does not match, `ATTACH-REQ-002`); attaching an attachment to a weapon whose class lacks that slot (does not match, `ATTACH-FAIL-001`). |
| **Not-you** | A player who only ever fires bare weapons never opens a smithing table for this mod at all. A modpack author without Create Fly loses the deploying front end but keeps the smithing one — attaching still works entirely by hand. |

## 3. Enumerations

### Attachments and their stat modifiers (proposed, retune at the sweep)

All modifiers are multiplicative on the named stat unless noted; applied in the fixed slot order
`domains/weapon.md` `WEAPON-REQ-003` defines. Zoom is a separate field, read only by
`domains/combat.md`'s scope mechanism, not by the stat function's own multiplication.

| Slot | Attachment | Modifiers | Zoom |
|---|---|---|---|
| Muzzle | Suppressor | spread ×0.90, muzzle velocity ×0.97 | — |
| Muzzle | Compensator | recoil ×0.75 | — |
| Muzzle | Flash hider | spread ×0.95 | — |
| Optic | Red dot | spread-while-aiming ×0.85 | none (1.0) |
| Optic | Holo | spread-while-aiming ×0.85 | none (1.0) |
| Optic | 2x | spread-while-aiming ×0.75 | 2.0 |
| Optic | 3x | spread-while-aiming ×0.65 | 3.0 |
| Optic | 4x | spread-while-aiming ×0.55 | 4.0 |
| Optic | 6x | spread-while-aiming ×0.45 | 6.0 |
| Optic | 8x | spread-while-aiming ×0.35 | 8.0 |
| Optic | 15x | spread-while-aiming ×0.25 | 15.0 |
| Magazine | Extended | magazine size ×1.5, reload ×1.1 | — |
| Magazine | Quickdraw | reload ×0.8 | — |
| Magazine | Extended quickdraw | magazine size ×1.3, reload ×0.95 | — |
| Grip | Vertical | recoil ×0.85 | — |
| Grip | Angled | reload ×0.9 | — |
| Grip | Half | spread ×0.9 | — |
| Grip | Light | fire rate ×0.95 (faster) | — |
| Grip | Thumb | recoil ×0.9 | — |
| Stock | Tactical stock | recoil ×0.85 | — |
| Stock | Cheek pad | spread ×0.9 | — |
| Stock | Bullet loops | reload ×0.85 | — |

"Zoom" of `none (1.0)` means the optic never satisfies the widened `isScoping()` check
(`domains/combat.md` `COMBAT-DEC-002`): it still contributes its own aim-spread modifier, just
never a magnified view.

### Slot availability per class

Restated from `domains/weapon.md` §3: pistol has muzzle/optic/magazine; SMG and assault rifle add
grip and stock; DMR and sniper rifle have muzzle/optic/magazine/stock (no grip); shotgun has only
muzzle/magazine (no optic, no grip, no stock). An attachment's own slot, not its own class list,
determines fit: any muzzle attachment fits any class with a muzzle slot, universally
(`rulings-2026-09-20.md`: "attachments work on all weapon types that accept an attachment").

### Recipe shapes

**Attach (smithing)**: base slot = a weapon (`firearms:base` present) whose class has the named
slot and whose corresponding `firearms:attachment_<slot>` component is **absent**; addition slot =
an attachment item for that slot; template slot = empty (`ATTACH-REQ-001`). If the named slot is
already occupied, the recipe does not match — there is no other outcome to reach for it.

**Attach (deploying)**: target = the same weapon shape as above; ingredient (deployer's held item)
= the attachment item; `keep_held_item: false` (consumed, like planks into a cogwheel).

There is no detach recipe of either kind (`decisions/DEC-017-no-detach-durability.md`).

## 4. Use cases

`UC-001`, `UC-002`, `UC-005` in `02-journeys.md`. `UC-003`, `UC-004` and `UC-006` are withdrawn
(they described a detach path that no longer exists, `decisions/DEC-017-no-detach-durability.md`).

## 5. Requirements

| ID | Requirement | Priority | From |
|---|---|---|---|
| `ATTACH-REQ-001` | The system shall provide a custom `SmithingRecipe` implementor that matches a base-slot weapon and an addition-slot attachment only when the weapon's class has that attachment's slot and the slot's component is currently absent. | Must | `UC-002` |
| `ATTACH-REQ-002` | The system shall never match an attach recipe against an already-occupied slot, and shall provide no other recipe or path that changes or clears a filled slot: once a slot is filled it is permanent for the life of the weapon (Kevin, 2026-09-20: "they don't come off; a weapon is crafted and done"). | Must | `decisions/DEC-017-no-detach-durability.md` |
| `ATTACH-REQ-003` | On a successful attach match, the system shall assemble a copy of the base weapon with the named slot's component set to the attachment's own id, every other component unchanged. | Must | `UC-002` |
| ~~`ATTACH-REQ-004`~~ | ~~Five detach tool items, one per slot, clearing a slot component and returning the displaced attachment.~~ **Withdrawn** — no detach path exists (Kevin, 2026-09-20; `decisions/DEC-017-no-detach-durability.md`). Id kept, not reused. | Withdrawn | — |
| `ATTACH-REQ-005` | The system shall provide a custom `Recipe<ItemApplicationInput>` reporting `getType() == AllRecipeTypes.DEPLOYING`, sharing the identical attach logic as `ATTACH-REQ-001`–`003`, matched via a deployer's target (the weapon on a belt or depot) and held item (an attachment). | Must | `UC-005` |
| `ATTACH-REQ-006` | The deploying attach recipe shall consume the held attachment (`keep_held_item: false`). | Must | `UC-005` |
| `ATTACH-REQ-007` | The system shall provide a plain crafting-table recipe (shaped or shapeless) for every attachment item; exact materials are confirmed at the first ticket. | Must | `rulings-2026-09-20.md` |
| `ATTACH-REQ-008` | The system shall implement both recipe front ends by calling one shared attach function, never duplicating the slot-matching or component-merge logic between the smithing and deploying recipe classes. | Must | `04-architecture.md` `ARCH-DEC-002`, `ARCH-DEC-003` |

## 6. Failure modes

| ID | Failure | Response |
|---|---|---|
| `ATTACH-FAIL-001` | Attaching an attachment to a weapon class that lacks that slot (e.g. a scope onto a shotgun) | The recipe simply does not match; no error, no highlight in the smithing table's slot filter beyond the ordinary "this doesn't fit" cue. |
| ~~`ATTACH-FAIL-002`~~ | ~~Detaching an empty slot.~~ **Withdrawn** — no detach recipe exists to attempt this against. Id kept, not reused. | — |
| `ATTACH-FAIL-003` | Attaching over an occupied slot | Never matches (`ATTACH-REQ-002`); there is no other recipe to reach for changing it — the slot stays exactly as first filled for the life of the weapon. |
| `ATTACH-FAIL-004` | A deployer holds an attachment that does not fit the target weapon at all | No recipe matches; the deployer behaves as it does for any unmatched item application — it simply does nothing that cycle. |

## 7. Open questions

| Question | Blocks | Decided by |
|---|---|---|
| Exact crafting-table materials for all 22 attachments | `ATTACH-REQ-007` | first ticket |
| Whether "crafted and done" should also be read as blocking a *second, still-empty* slot from ever being filled after the weapon's first attach — this sheet reads it as permitting that (see §1's stated reading) | `ATTACH-REQ-001`, `002` | Kevin, to confirm or correct this sheet's own reading |

## 8. Decisions

- `ATTACH-DEC-001` — **One shared attach function behind two recipe front ends** (Kevin, 2026-09-20:
  "it should be possible to use it with Create contraptions ... but it should also be able to be
  used by the player"). The smithing recipe and the deploying recipe are different `Recipe`
  implementations for different lookup mechanisms, but both call the same slot-match-and-merge
  logic, mirroring `create_villager_customers` `DEC-005`'s "both directions are one act." **Cost if
  wrong:** none identified — this is strictly less code than two independent implementations.
- ~~`ATTACH-DEC-002`~~ — ~~Detach tools, one item per slot, and no second recipe class.~~
  **Withdrawn** (Kevin, 2026-09-20: "they don't come off"; `decisions/DEC-017-no-detach-durability.md`
  supersedes this decision outright — there is no detach mechanism of any shape to choose between
  anymore). Id kept, not reused.
- `ATTACH-DEC-003` — **Attach never matches an occupied slot, and there is no path to free one**
  (`ATTACH-REQ-002`). Originally justified only by the vanilla smithing table's single result slot
  (a technical constraint on *replacement*); now additionally, and more fundamentally, required by
  Kevin's own ruling that attachments are permanent once set
  (`decisions/DEC-017-no-detach-durability.md`) — the technical reason and the design reason now
  point the same way. **Cost if wrong:** none identified against the current ruling; if a future
  ruling reopens detaching, this decision and `ATTACH-DEC-002`'s withdrawn per-slot-tool shape are
  both immediate candidates to revive.
