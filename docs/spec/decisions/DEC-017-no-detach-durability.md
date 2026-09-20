---
title: "create_firearms DEC-017 — Attachments never come off; ordinary durability; not repairable; not enchantable"
type: "spec"
category: "create_firearms"
---

# `DEC-017` — Attachments never come off; ordinary durability; not repairable; not enchantable

**Status:** decided by Kevin, 2026-09-20, overriding the final rulings' own "attach and detach both
ways" and "swappable and transferable" lines.

Kevin, verbatim: "they don't come off; a weapon is crafted and done; it should lose durability like
any other tool; not repairable in an anvil; not enchantable."

## No detach, by either front end

Attaching was always two front ends over one shared function (`decisions/DEC-006-no-new-block.md`,
`domains/attach.md` `ATTACH-DEC-001`); this ruling removes the reverse direction entirely, not just
one side of it. There is no detach recipe at the smithing table and no reverse deploying recipe for
a Create deployer — `domains/attach.md`'s five per-slot detach tool items and its detach recipe
shapes are withdrawn outright (`ATTACH-REQ-004`, `ATTACH-DEC-002`, both marked withdrawn, ids kept
per this sheet's identifier policy). An attach recipe still only ever matches an *empty* slot
(`ATTACH-REQ-001`, `002`), but there is now no other recipe to reach for an *occupied* one: once
set, a slot's component is permanent for the life of the weapon.

**This sheet's own reading of "a weapon is crafted and done," stated for Kevin to confirm or
correct**: the phrase applies to a filled slot, not to the weapon as a whole. A weapon may still
gain attachments in its other, still-empty slots at any later time — that remains the upgrade path
the rulings' earlier "easy upgrade paths" and "swappable and transferable" language described,
just narrowed from *swap* to *fill-once*. If Kevin instead means a weapon's attachment loadout is
fixed entirely at the moment of its first attach (no further slots fillable later either), that is
a further tightening of `ATTACH-REQ-001`/`002`, not a large redesign — `domains/attach.md` §7 names
this explicitly as an open question rather than assuming the more permissive reading is correct.

## Ordinary vanilla durability, unchanged

`WEAPON-REQ-008`'s per-shot `DataComponents.DAMAGE` decrement was already the design before this
ruling; nothing here changes it. A weapon breaks exactly as any vanilla damageable tool does once
its damage value reaches its `MAX_DAMAGE`.

## Not repairable in an anvil — the exact 26.2 mechanism, checked against the jar

`Item.Properties.repairable(Item)`/`repairable(TagKey<Item>)` are the calls that set
`DataComponents.REPAIRABLE` (confirmed present: `javap -p -cp minecraft-merged-deobf-26.2.jar
'net.minecraft.world.item.Item$Properties'`). `ItemStack.isValidRepairItem(ItemStack)`
disassembles to reading that exact component and returning `false` when it is absent — never true
by any other path. **No weapon of this mod's own calls `.repairable(...)`**, so the anvil's
material-repair branch (`AnvilMenu.createResult()`, the branch gated on `isValidRepairItem`) never
fires for it: combining a weapon with an iron ingot, or any other item, produces no result.

**A residual gap, found and named, not silently closed**: `AnvilMenu.createResult()` disassembles
to a *second*, independent repair path — combining two `ItemStack`s of the *same* `Item` (checked
by identity, not `REPAIRABLE`) that are both damageable restores some durability to the first from
the second. This path does not check `isValidRepairItem` at all, so two damaged copies of the same
weapon model combined at an anvil would still partially repair one from the other under ordinary
vanilla behaviour. Omitting `REPAIRABLE` closes the *material*-repair path completely; it does not
close this same-item combine path. `domains/weapon.md` `WEAPON-FAIL-006` and its open question
carry this forward rather than overclaiming "not repairable in an anvil" is fully achieved by one
component omission alone.

**Resolved at `FA-4`**: Kevin ruled the gap must close rather than stand as an accepted residual
(`domains/weapon.md` `WEAPON-DEC-005`). A narrow server-side `@Inject` at `AnvilMenu.createResult()`'s
`HEAD` (`firearms.mixin.AnvilMenuMixin`) cancels the result whenever both anvil inputs are
`firearms:weapon`, before vanilla's own same-item repair math runs, leaving every other item pair —
vanilla or another mod's own — untouched. "Not repairable in an anvil" is now fully achieved: the
material path by component omission, the same-item path by this one mixin.

## Not enchantable — the exact 26.2 mechanism, checked against the jar

`Item.Properties.enchantable(int)` sets `DataComponents.ENCHANTABLE`. `ItemStack.isEnchantable()`
disassembles to an immediate `return false` when that component is absent — the first branch in the
method, before anything else is inspected. **No weapon of this mod's own calls `.enchantable(...)`**,
so the enchanting table offers it nothing. Separately, `AnvilMenu.createResult()`'s enchanted-book
path walks the book's enchantments and calls `Enchantment.canEnchant(ItemStack)` for each one before
applying it; every vanilla enchantment's `canEnchant` checks the target against that enchantment's
own supported-items tag. **No weapon of this mod's own is added to any vanilla enchantment's
supported-items tag**, so every `canEnchant` call fails, no enchantment transfers, and the anvil's
result stays empty for a weapon-plus-enchanted-book combination. Both gates are independent and both
are closed by omission, not by a mixin.

## Alternative considered

Keeping detaching as `domains/attach.md`'s first draft designed it (five per-slot detach tools,
sharing the attach recipe class). Superseded outright by this ruling, not weighed against it — Kevin
overrode the earlier "swappable and transferable" framing directly. Cost if wrong: reviving
detaching later is a real design change (a new recipe branch, new items, new test coverage) — this
sheet does not pretend it is a cheap toggle either way.

## Cost if wrong

None identified for the durability/repair/enchant parts — these are direct component omissions with
no code to maintain. The permanent-attach reading (§"No detach, by either front end") is explicitly
flagged as this sheet's own interpretation, not a verified Kevin ruling on that specific point, and
is the one item in this decision Kevin may need to correct rather than merely confirm.
