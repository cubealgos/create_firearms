---
title: "create_firearms spec — WEAPON: base weapons, stats, firing, components, models"
type: "spec"
category: "create_firearms"
---

# `WEAPON` — base weapons and how they fire

## 1. Purpose

The six 1.0 base weapons, their classes, calibres and base stats; the pure function that derives
final stats from a weapon's components; firing itself (cooldown, fire mode, ammo consumption,
reload, durability loss, and why a worn weapon cannot be repaired or enchanted); the components a
weapon carries; its item model and sounds. Not what an attachment item is or how it gets onto a
weapon (`domains/attach.md` — including that it never comes off), not the ammunition item itself
(`domains/ammo.md`), not what happens once a bullet leaves the barrel (`domains/combat.md`), and
not the tooltip's exact rendering (`domains/ui.md`).

## 2. Dimensions

| Dimension | Answer |
|---|---|
| **Actors** | The player (`ACTORS-001`) crafts, loads and fires; the server (`ACTORS-002`) resolves the recipe, derives stats, and applies every effect of firing; the deployer (`ACTORS-003`) never fires, only reconfigures (`domains/attach.md`). |
| **Over time** | Crafted (bare, no attachments, no ammo) → gains attachments, permanently, one empty slot at a time, at the smithing table or a deployer (`domains/attach.md`) → loaded (`domains/ammo.md`) → fired, repeatedly, losing one round and one durability point per shot → reloaded or emptied → eventually broken (durability exhausted, unrepairable — `WEAPON-REQ-014`) and retired. |
| **Multiplicity** | Six 1.0 weapons, one per class, six calibres, none repeated (Kevin, 2026-09-20). Zero to five attachments present at once, one per slot, gated by the class's own slot set. A datapack may add more weapons in an existing class with zero new Java (`WEAPON-DEC-003`). |
| **Unwanted** | Firing with no ammo loaded (empty click, no error); firing while the smithing/deploying attach step is mid-recipe (not possible — attaching and firing are never concurrent on the same item, since an item is either sitting in a recipe slot or held, never both); an attachment id in a slot component that no longer exists after a datapack removal (treated as absent by the stat function, `WEAPON-FAIL-004`). |
| **Not-you** | A player who never touches this mod sees no change to any vanilla item, sound, or screen. A modpack author who removes Create Fly loses the deploying front end but keeps the smithing one and every crafting recipe, since only the deploying path depends on Create Fly at all. |

## 3. Enumerations

### Classes and their slots

| Class | Slots |
|---|---|
| Pistol | muzzle, optic, magazine |
| SMG | muzzle, optic, magazine, grip, stock |
| Assault rifle | muzzle, optic, magazine, grip, stock |
| DMR | muzzle, optic, magazine, stock |
| Sniper rifle | muzzle, optic, magazine, stock |
| Shotgun | muzzle, magazine |

Fixed per class, not per weapon: every weapon in a class has exactly that class's slot set, so a
datapack adding a new pistol needs no new slot logic (`WEAPON-DEC-003`).

### The 1.0 roster — base stats (proposed, retune at the sweep)

Damage is in half-hearts (vanilla damage points — a diamond sword deals 7, a bow arrow 2–6 at up
to 3 blocks/tick, `rulings-2026-09-20.md` reference points). Muzzle velocity is in blocks/tick.
Spread is the hip-fire cone half-angle in degrees. Fire rate is the minimum ticks between shots
(the `ItemCooldowns` duration). Recoil is the vertical camera kick in degrees per shot, cosmetic
only (`domains/combat.md` `COMBAT-DEC-003`). Durability is shots before the weapon breaks.

| Weapon | Class | Calibre | Damage/hit | Muzzle velocity | Spread (°) | Fire rate (ticks) | Fire mode | Magazine | Reload (ticks) | Recoil (°) | Durability |
|---|---|---|---|---|---|---|---|---|---|---|---|
| M1911 | Pistol | `.45 ACP` | 3 | 4.0 | 3.0 | 6 | semi | 7 | 30 | 1.5 | 250 |
| Micro Uzi | SMG | `9mm` | 2 | 4.0 | 4.5 | 2 | auto | 32 | 40 | 1.0 | 300 |
| AKM | Assault rifle | `7.62mm` | 4 | 6.0 | 3.5 | 4 | auto | 30 | 50 | 2.5 | 400 |
| Ruger Mini-14 | DMR | `5.56mm` | 5 | 7.0 | 2.0 | 8 | semi | 20 | 45 | 2.0 | 350 |
| AWM | Sniper rifle | `.300 Magnum` | 12 | 10.0 | 0.5 | 30 | semi (bolt-cycle) | 5 | 60 | 4.0 | 300 |
| Winchester Model 1897 | Shotgun | `12 gauge` | 2 ×8 pellets | 4.0 | 8.0 (per pellet) | 15 | pump | 6 | 55 | 3.5 | 200 |

Sanity check against the reference points: the AWM's 12-damage hit is close to lethal against an
unarmored 20-HP player in one round, matching a sniper's intended one-shot feel without exceeding a
diamond sword's 7 by an implausible multiple once armor is accounted for; the shotgun's 16 maximum
total damage requires every pellet to connect at point-blank range, falling off sharply with
distance via the pellet cone. **All six rows, and every attachment modifier in
`domains/attach.md`, are proposed starting numbers, not balanced** — this sheet marks them exactly
that, per the ticket's own instruction.

### Fire modes

| Mode | Behaviour |
|---|---|
| `semi` | One shot per press of the fire control; the next shot is blocked until the fire-rate cooldown clears, regardless of how long the control is held. |
| `auto` | Repeats firing at the fire-rate interval for as long as the control is held and ammo remains (`UC-008`). |
| `pump` | Behaves as `semi` for cooldown purposes; the assemble step for a shotgun spawns several pellet bullets per trigger pull instead of one (`domains/combat.md` `COMBAT-REQ-005`). |

### Components

| Component | Shape | Present when |
|---|---|---|
| `firearms:base` | `{ version: int, weapon_id: Identifier }` | Always, on every item of this mod's own weapon type. |
| `firearms:ammo` | `{ caliber: Identifier, loaded: int }` | Once the weapon has been loaded at least once (`contracts/data-contract.md`). |
| `firearms:attachment_muzzle` | attachment id | A muzzle attachment is attached. |
| `firearms:attachment_optic` | attachment id | An optic attachment is attached. |
| `firearms:attachment_magazine` | attachment id | A magazine attachment is attached. |
| `firearms:attachment_grip` | attachment id | A grip attachment is attached. |
| `firearms:attachment_stock` | attachment id | A stock attachment is attached. |

Only slots the weapon's class actually has can ever carry a component (`WEAPON-REQ-001`); a
shotgun, for instance, never carries `firearms:attachment_optic`.

### Item model layers

One composite model per base weapon: a base layer plus one `minecraft:condition` layer per slot
the class has, each gated on `minecraft:has_component` against that slot's own component
(`04-architecture.md` `ARCH-DEC-006`). A display transform on the base layer positions the weapon
for aiming vs. hip-fire, distinguished by whether the player is currently using the item
(`ItemDisplayContext`, research `smithing-and-item-model-layers-26-2.md` §B.4).

Since `decisions/DEC-018-art-direction.md` (Kevin, 2026-09-20) every layer is a cuboid element
model, not a sprite: the base layer is the weapon's 3D model with its own texture atlas, each slot
layer is a 3D part positioned at the slot's anchor in model units, and the same composite renders
in every display context (inventory included) as Create's potato cannon does. Standalone
attachment items and cartridges remain flat 16×16 sprites in the vanilla/Create pixel style.

## 4. Use cases

`UC-001`, `UC-007`–`UC-014`, `UC-018`, `UC-019` in `02-journeys.md`.

## 5. Requirements

| ID | Requirement | Priority | From |
|---|---|---|---|
| `WEAPON-REQ-001` | The system shall define six weapon classes, each with a fixed slot set per the class/slot table, shared by every weapon in that class. | Must | Enumerations |
| `WEAPON-REQ-002` | The system shall ship the six 1.0 weapons in the roster table above, each with the declared base stats, calibre, and class. | Must | `rulings-2026-09-20.md` |
| `WEAPON-REQ-003` | The system shall derive a weapon's final stats as a pure function of `firearms:base`'s weapon id and every present `firearms:attachment_<slot>` component, applying each present attachment's own modifiers in fixed slot order (muzzle, optic, magazine, grip, stock) for determinism; the function shall be re-run at every point stats are needed (firing, tooltip, trade display) and never written back to the item. | Must | `04-architecture.md` `ARCH-DEC-005` |
| `WEAPON-REQ-004` | The system shall fire according to the weapon's `fire_mode` base stat: `semi` once per press, `auto` repeating while held, `pump` once per press with multiple pellets (`domains/combat.md`). | Must | `UC-007`–`009` |
| `WEAPON-REQ-005` | While the player holds the aim control, the system shall narrow the derived spread cone by the attached optic's own spread modifier, for every optic including non-zooming ones (red dot, holo); with no optic attached, aiming narrows spread by no amount beyond the weapon's own hip-fire value. | Must | `UC-011`, `UC-012` |
| `WEAPON-REQ-006` | The system shall provide a plain crafting-table recipe (shaped) for every base weapon; exact grids and materials are confirmed at the first ticket, proposed as iron ingots, sticks, and redstone in proportions scaled roughly to each weapon's own derived stats. | Must | `rulings-2026-09-20.md`: "all bullets + attachments + bases should have normal crafting recipes" |
| `WEAPON-REQ-007` | The system shall refuse to fire, with no error and no sound beyond an empty-click cue, when `firearms:ammo` is absent or its `loaded` field is zero. | Must | `WEAPON-FAIL-002` |
| `WEAPON-REQ-008` | On every shot fired, the system shall decrement `firearms:ammo.loaded` by one, reduce the weapon's vanilla durability (`DataComponents.DAMAGE`) by one, and start the weapon's fire-rate cooldown, keyed to its own cooldown group. | Must | `UC-007` |
| `WEAPON-REQ-009` | The system shall pace fire rate through vanilla `ItemCooldowns`, using the derived `fire_rate` stat as the cooldown's tick duration, the same mechanism the potato cannon uses. | Must | Research `create-fly-potato-cannon-and-deploying-26-2.md` §A.2, §C.1 |
| `WEAPON-REQ-010` | On reload, the system shall scan the player's inventory for a cartridge item matching the weapon's `firearms:ammo.caliber` (or the weapon's base calibre if unloaded), consume cartridges up to the derived magazine size, set `firearms:ammo.loaded` accordingly, and start the derived `reload_ticks` cooldown. | Must | `UC-010`; `domains/ammo.md` `AMMO-REQ-004` |
| `WEAPON-REQ-011` | Where no matching cartridge exists anywhere in the player's inventory, the system shall leave the weapon's current ammo unchanged and produce no error. | Must | `WEAPON-FAIL-003` |
| `WEAPON-REQ-012` | The system shall render a weapon's item model as one composite file per base weapon (`04-architecture.md` `ARCH-DEC-006`), never one file per base×attachment combination. | Must | `04-architecture.md` |
| `WEAPON-REQ-013` | The system shall play a distinct fire sound and a distinct empty-click sound per weapon; a suppressor attachment shall reduce the fire sound's effective volume/range (`domains/combat.md` `COMBAT-REQ-009`). | Must | `00-context.md` |
| `WEAPON-REQ-014` | The system shall not set `DataComponents.REPAIRABLE` (`Item.Properties.repairable(...)`) on any weapon item, so `ItemStack.isValidRepairItem` always returns false and the anvil's material-repair path (combining a weapon with a repair item such as an iron ingot) never produces a result for it. | Must | Kevin, 2026-09-20: "not repairable in an anvil"; `decisions/DEC-017-no-detach-durability.md` |
| `WEAPON-REQ-015` | The system shall not set `DataComponents.ENCHANTABLE` (`Item.Properties.enchantable(...)`) on any weapon item and shall not add any weapon item to any vanilla enchantment's supported-items tag, so `ItemStack.isEnchantable()` returns false (the enchanting table offers nothing for it) and `Enchantment.canEnchant()` fails for every enchantment (an anvil combination with an enchanted book transfers nothing and produces an empty result). | Must | Kevin, 2026-09-20: "not enchantable"; `decisions/DEC-017-no-detach-durability.md` |
| `WEAPON-REQ-016` | Every base weapon's item model shall be a cuboid element model with a per-weapon texture atlas, and every mounted attachment a cuboid part model positioned at its slot's anchor, composited as `WEAPON-REQ-012` describes and rendered as a 3D model in every display context; both generated deterministically by `tools/models.py`. | Must | `decisions/DEC-018-art-direction.md` |
| `WEAPON-REQ-017` | Every texture and sprite of the mod shall follow the vanilla/Create pixel style: 16×16 canvases for sprites, a one-pixel outline in the material's darkest tone, three to four tones per material lit from the top-left, colours from the shared palette in `tools/palette.py`, no gradients or anti-aliasing. | Must | `decisions/DEC-018-art-direction.md` |
| `WEAPON-REQ-018` | The attack control (left click) shall fire the held firearm and the use control (right click), held, shall aim it; a use press shall never fire. Firing travels as this mod's own client-to-server payload and is validated by the server's `FiringLogic` (cooldown, ammo, durability) exactly as before; `auto` repeats while the attack control is held. While a firearm is held, the attack control shall not swing, break blocks or melee-hit. | Must | `decisions/DEC-019-controls.md` |
| `WEAPON-REQ-019` | Aiming shall last exactly as long as the use control is held (a bow-length use duration ended only by release, never by a shot or a cooldown); with no magnifying optic (none, red dot, holo) the aiming pose is iron sights: centred model, no zoom, no overlay. | Must | `decisions/DEC-019-controls.md` |

## 6. Failure modes

| ID | Failure | Response |
|---|---|---|
| `WEAPON-FAIL-001` | Attempting to fire an item that is not a weapon of this mod's own (no `firearms:base`) | Not reachable: firing is only ever bound to items carrying `firearms:base`; nothing else exposes the fire control. |
| `WEAPON-FAIL-002` | Firing with no ammo loaded | `WEAPON-REQ-007`: empty-click sound, no shot, no error. |
| `WEAPON-FAIL-003` | Reloading with no matching cartridge in inventory | `WEAPON-REQ-011`: ammo unchanged, no error. |
| `WEAPON-FAIL-004` | A slot component names an attachment id a datapack has since removed | Treated as absent by the stat derivation function: that slot contributes no modifier, no crash. |
| `WEAPON-FAIL-005` | Durability reaches zero mid-magazine | The weapon breaks exactly as any vanilla durability item does, unrepairable (`WEAPON-REQ-014`); any remaining loaded ammo is lost with it, same as an arrow left in a broken bow-equivalent item is not recovered. |
| `WEAPON-FAIL-006` | Two damaged copies of the same weapon combined at an anvil | Vanilla's own same-item combine-repair path in `AnvilMenu.createResult()` would otherwise restore some durability regardless of `REPAIRABLE`, since it checks only item identity and `isDamageableItem()`, never `isValidRepairItem`. `WEAPON-REQ-014` closes the material-repair path by component omission alone; this residual gap is closed separately, by `firearms.mixin.AnvilMenuMixin` (`WEAPON-DEC-005`, `FA-4`). |

## 7. Open questions

| Question | Blocks | Decided by |
|---|---|---|
| Exact crafting-table grids and material quantities for the six base weapons | `WEAPON-REQ-006` | first ticket |

## 8. Decisions

- `WEAPON-DEC-001` — **Stats are dynamic, a pure function, never baked** (Kevin, 2026-09-20: "stats
  to be dynamic"; `04-architecture.md` `ARCH-DEC-005`). The full reasoning lives there; restated
  here as the mechanism this domain's firing and tooltip logic both call.
- `WEAPON-DEC-002` — **One weapon per class, six calibres, none repeated, at 1.0** (Kevin,
  2026-09-20), out of the full PUBG-shaped roster the proposal listed. Every attachment ships at
  1.0 regardless — the roster is the smaller dial, not the attachment set. **Cost if wrong:** the
  rest of the roster is a data-file addition per `WEAPON-DEC-003`, not a rewrite.
- `WEAPON-DEC-003` — **A new weapon in an existing class needs zero new Java** (this sheet's
  reading of the proposal's data shape, unchanged by the rulings): a base weapon is one data file
  naming its class, calibre and base stats; the per-class slot table is the only place slot
  membership is expressed, so nothing about adding a weapon touches it. **Cost if wrong:** if a
  future weapon needs a class the current table does not have, that is a genuine new-class ticket,
  not a data file — accepted, since 1.0's six classes were chosen to cover the full slot space
  already.
- `WEAPON-DEC-004` — **Ordinary vanilla durability, no repair, no enchanting** (Kevin, 2026-09-20:
  "it should lose durability like any other tool; not repairable in an anvil; not enchantable";
  `decisions/DEC-017-no-detach-durability.md`). Restated here as the mechanism `WEAPON-REQ-008`,
  `014` and `015` implement: `Item.Properties.durability(int)` and its usual per-shot `DAMAGE`
  decrement are unchanged from the original proposal; omitting `.repairable(...)` and
  `.enchantable(...)` from that same properties chain, and never tagging a weapon into any
  enchantment's supported-items set, closes the material-repair and enchanting paths at their
  source components. **Cost if wrong:** re-adding either component later is one line in the item's
  registration, not a redesign — but the residual anvil combine-repair gap `WEAPON-FAIL-006` names
  needs a mixin either way if it must close too, regardless of which way this decision goes.
- `WEAPON-DEC-005` — **The `WEAPON-FAIL-006` residual gap closes, by a narrow server mixin, not by
  leaving it as an accepted gap** (Kevin, 2026-09-20, settling §7's own open question at `FA-4`):
  `firearms.mixin.AnvilMenuMixin` injects at `AnvilMenu.createResult()`'s `HEAD`, cancellable, and
  when both the input and additional slot stacks are `firearms:weapon` clears the result slot and
  sets the anvil's cost to 0 before any of vanilla's own same-item combine-repair math runs. Scoped
  to `firearms:weapon` alone by `Item` identity, the same identity vanilla's own gate already checks
  — an unrelated item pair (two iron pickaxes, proven by `FA-4`'s own game test) still combines
  exactly as vanilla intends. **Why `HEAD` over a narrower redirect**: `AnvilMenu.createResult()`
  disassembles to one long method with no single call site a `@Redirect` could retarget without also
  matching the method's other, unrelated `isDamageableItem()`/`is(Item)` calls (the material-repair
  branch above the same-item branch calls `isDamageableItem()` too); cancelling at `HEAD` once both
  stacks are already known to be `firearms:weapon` touches nothing about the method's control flow
  for any other item and mirrors exactly what vanilla itself does on every other "no result" branch
  of this same method. **Cost if wrong:** removing the mixin later is a one-file deletion plus the
  matching mixin-config entry, not a redesign — the material-repair and enchanting refusals
  (`WEAPON-REQ-014`, `015`) do not depend on it and are unaffected either way.
- `WEAPON-DEC-006` — **The AWM's "semi (bolt-cycle)" reuses plain `semi`; no third fire-mode value**
  (`FA-6`, settling §7's own open question; confirmed rather than re-litigated per that ticket's own
  instruction, since testing did not show the reuse to be observably wrong): `FireMode` stays a
  two-real-value enum (`semi`, `auto`) plus `pump`, and the AWM's `WeaponBase` constant carries
  `FireMode.SEMI` with its own 30-tick `fireRateTicks` — the only place its bolt-cycle feel needed
  to live, per this sheet's own original proposal. **Cost if wrong:** a fourth `FireMode` value is
  an additive enum constant plus one new `switch` arm wherever fire mode is dispatched
  (`firearms.item.WeaponItem`), not a redesign of the roster or the derivation function.
- `WEAPON-DEC-007` — **Fire-rate pacing is vanilla `ItemCooldowns`, called by hand once per shot from
  a server-side `onUseTick`, not the automatic `useCooldown` property** (`FA-6`, settling §7's own
  open question): `useCooldown`/`UseCooldown.apply()` only fires from `ItemStack.use()`/
  `finishUsingItem()`, once per interaction, which cannot pace `auto`'s repeated per-tick shots at
  all and would double-apply for `semi`/`pump` (once from the component, once from this mod's own
  call) if used at all; `firearms.item.WeaponItem` instead starts the vanilla "using item" session
  uniformly from `use()`/`useOn()` and dispatches every fire-or-reload attempt through
  `firearms.fire.FiringLogic#attempt` from `onUseTick`, calling `player.getCooldowns().addCooldown(...)`
  itself with the derived `fireRateTicks`/`reloadTicks` value. Every base weapon shares the one
  `firearms:weapon` item, so `ItemCooldowns`'s own default cooldown-group-by-item-id would collide
  across different base weapons; `FiringLogic` keys every check and every start to a throwaway stack
  copy carrying a `minecraft:use_cooldown` component whose `cooldownGroup` is the base weapon's own
  id (`firearms:m1911`, and so on), never persisted back onto the real stack
  (`docs/spec/contracts/data-contract.md` `DATA-REQ-005`). **Cost if wrong:** switching to a
  `useCooldown`-driven `semi`/`pump` path later is a small `WeaponItem`/`FiringLogic` change, not a
  redesign — `auto` would still need the hand-rolled per-shot call regardless, since no vanilla
  mechanism paces a repeating interaction.
