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

- [x] All seven component types round-trip through a codec test, including the malformed-value
      degrade-to-absent/degrade-to-no-ammo path (`DATA-REQ-004`).
- [x] A weapon item stack never offers a repair result at a real `AnvilMenu.createResult()` against
      a repair material, and is never enchantable (`WEAPON-REQ-014`, `015` — the anvil's own
      same-item combine-repair path is `FA-4`'s own proof, not this ticket's).
- [x] Every one of the six base weapons, 22 attachments and six cartridges craft at a real crafting
      table via their own data file.
- [x] A datapack-added weapon or attachment in an existing class/slot loads with zero new Java
      (`WEAPON-DEC-003`, `SURFACE-REQ-003`).
- [x] `just check` green.

## Constraints and prior findings

`docs/spec/04-architecture.md` `ARCH-DEC-005`, `docs/spec/contracts/data-contract.md`,
`docs/spec/contracts/public-surface.md`, `docs/spec/domains/weapon.md` `WEAPON-REQ-006`, `014`,
`015`, §7, `docs/spec/domains/attach.md` `ATTACH-REQ-007`, §7, `docs/spec/domains/ammo.md`
`AMMO-REQ-001`–`003`. Blocked by `FA-2`'s model records, which this ticket's loaders read into.

## Findings

**Item shape: one weapon item, five attachment items (not 22), six cartridge items.** Registering a
new `Item` is Java-registry code, never datapack-addable — so `WEAPON-DEC-003` ("a new weapon in an
existing class needs zero new Java") and `domains/attach.md`'s identical claim for attachments only
hold if the physical item pool is fixed and every *variant* is carried on a component. Shipped:
`firearms:weapon` (one item; `firearms:base.weapon_id` says which of the six it is) and
`firearms:attachment_muzzle|optic|magazine|grip|stock` (five items, one per slot; that slot's own
`firearms:attachment_<slot>` component — the *same* component type a weapon's slot carries — says
which of that slot's attachments a given stack is). A custom `WeaponItem`/`AttachmentItem`
(`firearms.item`) overrides `getName(ItemStack)` to read that component and return a per-variant
translation key (`item.firearms.weapon.<id>`, `item.firearms.attachment.<id>`), falling back to the
item's own default name when the component is absent. Cartridges are the one exception, per
`AMMO-REQ-003`'s explicit "identified purely by its own item id, no component": six separate items,
`firearms:cartridge_<caliber>`. `domains/ammo.md`'s own Multiplicity row claims "zero new Java" for a
*new calibre* too, which is not actually achievable while `Caliber` stays a closed six-value enum —
flagged, not fixed here, since 1.0 ships exactly those six.

**Data folders**: `data/firearms/weapon/*.json` and `data/firearms/attachment/*.json`, exactly as
proposed. Implementation note for whoever touches `WeaponDataLoader`/`AttachmentDataLoader` next:
`FileToIdConverter.json(prefix)` resolves under `data/<namespace>/<prefix>/`, and this mod's own
namespace ("firearms") already supplies the `firearms/` segment — the prefix passed to the
constructor is `"weapon"`/`"attachment"`, **not** `"firearms/weapon"`/`"firearms/attachment"`.
Passing the doubled prefix compiles fine and fails silently at runtime (0 entries loaded, no error)
until a game test actually checks the loaded count — caught by
`DataLoaderGameTest` on the first real `runGameTest`, not by `compileJava`.

**Component shapes**: `firearms:base { version: int (optional, default 1), weapon_id: Identifier }`;
`firearms:ammo { caliber: Identifier, loaded: int, loaded >= 0 enforced by the codec via
`Codec.validate`, not a throwing record constructor }`; `firearms:attachment_<slot>` is a **bare**
`Identifier` (`Identifier.CODEC` directly) — no wrapper object, no per-component version. This
resolves the ticket's own Build-paragraph hedge ("`{version?, attachment_id}` as the spec shapes
them") against `data-contract.md`'s more specific "`<attachment id>`, or absent": the latter wins,
consistent with `DATA-REQ-001`'s single shared version living only on `firearms:base`. Every
`Base`/`Ammo` record avoids a throwing compact constructor for any field a codec could decode from
untrusted data — a thrown `IllegalArgumentException` mid-decode propagates as an uncaught exception
rather than a graceful `DataResult` error, which is the opposite of `DATA-REQ-004`'s "degrade to
absent, never crash."

**Durability**: no weapon calls `Item.Properties.durability(int)` — one shared `Item` can't carry six
different fixed `max_damage` values at the properties level. Instead every base's own crafting
recipe sets `minecraft:max_damage` and `minecraft:damage: 0` directly in its output `components`
patch (`ItemRegistration`'s own class doc explains this choice). `.stacksTo(1)` is set once on the
shared `Item.Properties` instead.

**Naming convention for JSON/identifier data**: every `firearms.model` enum (`Caliber`,
`WeaponClass`, `FireMode`, `Slot`, `Stat`, `Op`) is spelled in data files and in derived identifiers
(cartridge item paths, `firearms:ammo.caliber`) as `Ids.slug(Enum)` — the Java constant's own
`name()`, lowercased (`Caliber.ACP_45` → `"acp_45"`). Not previously named anywhere in
`docs/spec/contracts/public-surface.md`; worth promoting there explicitly once `FA-6` needs to match
a cartridge to a weapon's calibre by this same id.

**Recipe grids, as shipped** (materials restricted to iron ingot/nugget, stick, redstone, copper
ingot, gunpowder and `create:brass_sheet`, per the ticket's own allowed palette; all "proposed,
retune at the sweep" like every other 1.0 number):

*Weapons* (shaped, 3-wide; `I`=iron ingot, `#`=stick, `R`=redstone):
- M1911: `III` / `# R` — 3 iron, 1 stick, 1 redstone
- Micro Uzi: `III` / `I R` / ` ##` — 4 iron, 1 redstone, 2 stick
- AKM: `III` / `II#` / `#RR` — 5 iron, 2 stick, 2 redstone
- Ruger Mini-14: `III` / `II#` / `##R` — 5 iron, 3 stick, 1 redstone
- AWM: `III` / `III` / `##R` — 6 iron, 2 stick, 1 redstone
- Winchester Model 1897: `III` / `I#R` / `R  ` — 4 iron, 1 stick, 2 redstone

*Attachments* (shapeless; nugget = iron nugget, copper = copper ingot):
muzzle — suppressor 3 nugget+1 ingot, compensator 2 nugget+1 ingot, flash hider 1 nugget+1 ingot;
optic — red dot 1 nugget+1 redstone, holo 2 nugget+1 redstone, 2x–15x scopes 1–3 copper + 1–2
nugget scaling with magnification (2x: 1 copper+1 nugget … 15x: 3 copper+2 nugget); magazine —
extended 2 ingot, quickdraw 1 ingot+1 redstone, extended quickdraw 2 ingot+1 redstone; grip —
vertical 1 stick+1 nugget, angled 1 stick+2 nugget, half 2 stick+1 nugget, light 1 stick alone,
thumb 1 stick+1 redstone; stock — tactical 2 stick+1 ingot, cheek pad 1 stick+1 ingot, bullet loops
2 stick+1 redstone. Every one of the 22 multisets is pairwise distinct (verified by hand before
writing the JSON) so no two attachment recipes — or a weapon/cartridge recipe — can ever match the
same crafting-grid contents.

**Spec defect found and fixed: `domains/ammo.md`'s cartridge table gives two pairs of calibres
identical materials.** `.45 ACP` and `9mm` were both "1 brass sheet, 1 gunpowder, 1 iron nugget";
`7.62mm` and `5.56mm` were both "...2 iron nuggets" — a literal implementation would ship two
shapeless recipes per pair with the *same* ingredient multiset and *different* outputs, which is an
unreachable-recipe bug (a real crafting table can only resolve one of the two). Shipped instead:
iron-nugget count 1 through 6, one per calibre in the roster's own order (`.45 ACP`=1, `9mm`=2,
`7.62mm`=3, `5.56mm`=4, `.300 Magnum`=5, `12 gauge`=6) — every cartridge now uniquely reachable,
smallest possible diff from the spec's own numbers. `domains/ammo.md`'s own table heading already
reads "(proposed, retune at the sweep)," so this is within that license, but the collision itself
was not something the sheet's authors seem to have caught — flagged for Kevin to confirm rather than
silently overridden.

**Spec wording gap**: `04-architecture.md` `ARCH-DEC-005`'s opening line says "Eight component
types," then names and this ticket implements exactly seven (`firearms:base`, `firearms:ammo`, five
`firearms:attachment_<slot>`) — matching `data-contract.md`, `public-surface.md`, and this ticket's
own Scope paragraph everywhere else. Treated as a stray word in the architecture doc, not a real
eighth type; worth a one-word fix at the next spec sync.

**Game-test gotcha for whoever writes `FA-4`'s own anvil proof**: `GameTestHelper
.makeMockServerPlayerInLevel()`'s player reports `hasInfiniteMaterials() == true`. `AnvilMenu
.createResult()` has a real vanilla creative-mode convenience that lets such a player combine *any*
enchanted book with *any* item, bypassing the enchantment's own supported-items tag check entirely —
exactly the mechanism `WEAPON-REQ-015` relies on being closed. A first pass at this ticket's own
anvil game test used that mock player and got a false failure (a non-empty enchant result) that had
nothing to do with the weapon item itself. Fixed by using `helper.makeMockServerPlayer(GameType
.SURVIVAL)` instead (also sidesteps `makeMockServerPlayerInLevel`'s own deprecation-for-removal
warning). Recorded here since `WEAPON-FAIL-006`'s own anvil proof (`FA-4`) will hit the identical
trap if it reuses the deprecated mock-player helper.

**Not this ticket**: item models/textures (`ARCH-DEC-006`'s composite/condition layers) and any
creative-tab listing — none of the twelve items registered here has a model or appears in a creative
tab; every one is reachable only via its own crafting recipe or `/give` at this ticket's own state.
Left for whichever ticket lands `ARCH-DEC-006`.
