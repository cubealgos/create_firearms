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

- [ ] The open question in `domains/weapon.md` §7 is answered by Kevin and the answer is recorded
      in the spec (`docs/spec/domains/weapon.md` §8) in this same change.
- [ ] A real `AnvilMenu.createResult()` game test proves a weapon plus a repair material (e.g. iron
      ingots) produces no result (`WEAPON-REQ-014`).
- [ ] A real `AnvilMenu.createResult()` game test proves a weapon plus an enchanted book transfers
      no enchantment and produces no result (`WEAPON-REQ-015`).
- [ ] A real enchanting table offers no enchantment for a weapon item.
- [ ] The same-item combine-repair question is settled one way or the other by a game test, per the
      ruling recorded above.
- [ ] `just check` green.

## Constraints and prior findings

`docs/spec/decisions/DEC-017-no-detach-durability.md`, `docs/spec/domains/weapon.md`
`WEAPON-REQ-014`, `015`, `WEAPON-FAIL-006`, §7, `docs/spec/operations/testing.md` (this exact game
test named explicitly in the testing sheet's own game-test row). Blocked by `FA-3`'s item
registration (no `.repairable`/`.enchantable`) to test against.
