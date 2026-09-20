---
schema_version: 1
id: 01M2YWDMAM3EJVV4DMKAMZD8DJ
key: FA-4
type: feat
title: Anvil combine-repair refusal and enchanting refusal proof
created_by: kevin
created_at: 2026-09-20T07:45:18Z
---

## Scope

Settles `WEAPON-FAIL-006`'s named-but-open question: whether the vanilla same-item anvil
combine-repair path (two damaged copies of the same weapon item, checked by `Item` identity and
`isDamageableItem()`, never `isValidRepairItem`) is an acceptable residual gap or needs a narrow
`AnvilMenu` mixin to close (`decisions/DEC-017-no-detach-durability.md` §"Not repairable in an
anvil", `domains/weapon.md` §7). If Kevin rules it must close: one narrowly-scoped server mixin on
`AnvilMenu.createResult()` refusing a result when both inputs are a firearm of this mod's own,
nothing broader. Either way, a game test proves the material-repair path (weapon + iron ingot) and
the enchanted-book path both already refuse, by omission of `REPAIRABLE`/`ENCHANTABLE` alone
(`WEAPON-REQ-014`, `015`).

## Approach

First confirm which way `WEAPON-FAIL-006` resolves — this is a design question the spec names as
open, not decided inline (`docs/spec/README.md`'s own standing rule): ask Kevin, record the answer
in `docs/spec/domains/weapon.md` §8 in the same change. If the ruling is "close it": a single
`@Mixin(AnvilMenu.class)` `@Inject` at the point `createResult()` would otherwise accept a same-item
combine, cancelling only when both `ItemStack`s carry `firearms:base`. If the ruling is "leave the
residual gap": no mixin, just the game test proving the gap exists and is understood, not silently
missed.

## Acceptance criteria

- [x] The open question in `domains/weapon.md` §7 is answered by Kevin and the answer is recorded
      in the spec (`docs/spec/domains/weapon.md` §8) in this same change.
- [x] A real `AnvilMenu.createResult()` game test proves a weapon plus a repair material (e.g. iron
      ingots) produces no result (`WEAPON-REQ-014`).
- [x] A real `AnvilMenu.createResult()` game test proves a weapon plus an enchanted book transfers
      no enchantment and produces no result (`WEAPON-REQ-015`).
- [x] A real enchanting table offers no enchantment for a weapon item.
- [x] The same-item combine-repair question is settled one way or the other by a game test, per the
      ruling recorded above.
- [x] `just check` green.

## Constraints and prior findings

`docs/spec/decisions/DEC-017-no-detach-durability.md`, `docs/spec/domains/weapon.md`
`WEAPON-REQ-014`, `015`, `WEAPON-FAIL-006`, §7, `docs/spec/operations/testing.md` (this exact game
test named explicitly in the testing sheet's own game-test row). Blocked by `FA-3`'s item
registration (no `.repairable`/`.enchantable`) to test against.

## Findings

**Ruling: close it, not an accepted gap.** Recorded as `WEAPON-DEC-005` in `docs/spec/domains/weapon.md`
§8 (and the vault copy, `just spec-sync`-clean); §7's open question row is removed, `WEAPON-FAIL-006`'s
own row updated to point at the mixin instead of "named, not silently closed." `decisions/DEC-017-no-detach-durability.md`
gets a short "Resolved at FA-4" addendum under its own residual-gap paragraph rather than a rewrite.

**Injection point, decided against the disassembled bytecode (`javap -p -c` on
`minecraft-merged-deobf-26.2.jar`)**: `@Inject(method = "createResult", at = @At("HEAD"), cancellable = true)`
on `firearms.mixin.AnvilMenuMixin` (`@Mixin(AnvilMenu.class)`), registered under `firearms.mixins.json`'s
`"server"` array (new key — the file previously had only `"mixins"`/`"client"`, both empty). Chose `HEAD`
over a `@Redirect` on the same-item identity check: `createResult()` disassembles to one ~270-instruction
method with three separate `isDamageableItem()` calls and no single call site a redirect could retarget
without also catching the material-repair branch above it. `HEAD`-cancel, clearing the result slot and
`cost` to 0 before any vanilla state mutation runs, mirrors exactly what vanilla itself does on every
other "no result" branch of this same method, and needs only one `@Shadow`d field (`cost`, private in
`AnvilMenu` itself — `inputSlots`/`resultSlots` reached instead through the public `getSlot(int)` +
`AnvilMenu.INPUT_SLOT`/`ADDITIONAL_SLOT`/`RESULT_SLOT` constants the existing FA-3 anvil test already
used, no shadowing of the superclass's protected fields needed).

**No `firearms.item.NoRepair` helper**: the refusal predicate is one line
(`left.is(ItemRegistration.WEAPON) && right.is(ItemRegistration.WEAPON)`), and attachment/cartridge
items are never damageable (`.stacksTo(64)`, no `.durability(...)`, `ItemRegistration`) so the gap
`WEAPON-FAIL-006` names cannot reach them — nothing else would call a shared helper. Skipped rather
than adding indirection nothing else needs yet.

**`ItemStack.is(Item)` exists as an inherited default method**, not a declared overload — `ItemStack`
implements `ItemInstance extends TypedInstance<Item>`, and `TypedInstance<T>` declares `default boolean
is(T)` comparing `typeHolder().value() == item`. `javap -p` on `ItemStack.class` alone won't show it
(it lists only members declared directly on the class); worth remembering for the next disassembly-driven
ticket that goes looking for an "is(...)" overload and comes up empty.

**Real game-test entrypoint gotcha, caught by this ticket's own verification pass, not by `just check`**:
a new `src/gametest/java/firearms/gametest/*GameTest.java` file is invisible to `runGameTest` until its
class is also added to `src/gametest/resources/fabric.mod.json`'s `"fabric-gametest"` entrypoint list —
Fabric's game-test API discovers test classes from that list, not by classpath/annotation scanning. A
first pass here left `AnvilCombineRefusalGameTest` off that list; `./gradlew runGameTest` reported "22
required tests passed" both with the mixin wired up and (temporarily, for verification) with it disabled,
because the four new tests silently never ran either time — `just check`'s green result alone would not
have caught this. Caught only by deliberately disabling the mixin and confirming the anvil test then
failed for real (`isEmpty=false damage=0 cost=2` for two combined `m1911` stacks), which is what forced
finding the missing entrypoint line. Recorded here since `FA-3`'s own game-test gotcha
(`makeMockServerPlayerInLevel()`'s creative-mode enchant bypass) was exactly this kind of trap, and this
is a second, different one in the same area: **a green `just check` after adding a new gametest class is
not on its own proof the new tests ran — check the new class is in `fabric-gametest`'s entrypoint list,
or better, deliberately break the thing under test once and confirm the new test actually goes red.**

**Real `EnchantmentMenu` proof, not just `isEnchantable()` re-asserted**: `EnchantmentMenu.slotsChanged(Container)`
disassembles to checking `stack.isEnchantable()` before anything else — if false (or the stack is empty),
every `costs[i]`/`enchantClue[i]`/`levelClue[i]` is zeroed/`-1`'d without ever touching bookshelf count or
the player's XP level. `Slot.container` is a public field, so the real menu's own backing container can be
passed back into `slotsChanged(...)` without needing a `@Shadow` or reflection: `menu.getSlot(0).container`.
Chosen over `Enchantment.canEnchant(...)` on individual enchantments since it is strictly closer to "the
table offers nothing" than checking one or two enchantments by name.
