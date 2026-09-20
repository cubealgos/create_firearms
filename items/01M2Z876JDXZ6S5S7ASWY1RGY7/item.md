---
schema_version: 1
id: 01M2Z876JDXZ6S5S7ASWY1RGY7
key: FA-24
type: feat
title: "Controls: left click fires through a client-to-server payload, right click held aims with iron sights when no magnifying optic is attached"
created_by: kevin
created_at: 2026-09-20T11:11:30Z
---

## Scope

<fill this in before committing>

## Approach

<fill this in before committing>

## Acceptance criteria

- [ ] <fill this in before committing>

## Constraints and prior findings

<fill this in before committing>

## Scope

Kevin, 2026-09-20, first client session: right click both aimed and fired, so scoping fired instantly and the AWM looped scope/reset/scope with no shot. Ruled as `decisions/DEC-019-controls.md`, `WEAPON-REQ-018`, `WEAPON-REQ-019` (vault first, `docs/spec/` synced in this ticket). Attack (left click) fires; use (right click) held aims for as long as it is held; a use press never fires; with no magnifying optic (none, red dot, holo) aiming is an iron-sights pose with no zoom and no overlay; sneaking stops being the aiming stand-in. Reload: a dedicated key (Fabric key binding, default R) preferred so an empty weapon can still be aimed; fall back to the use press while empty if the key binding turns out awkward, and record the choice in `domains/weapon.md` §8.

## Approach

A `ServerboundFirePayload` (`CustomPacketPayload` + `PayloadTypeRegistry` + `ServerPlayNetworking.registerGlobalReceiver`), validated on the server by the existing `FiringLogic` (cooldown, ammo, durability, bullet spawn) unchanged. Client: a key handler on `ClientTickEvents.END_CLIENT_TICK` reading `Minecraft.getInstance().options.keyAttack` (`isDown` for auto at the derived fire-rate interval, `consumeClick` for semi/pump) while the main hand holds a `firearms:weapon`, plus a client mixin cancelling `Minecraft.startAttack`/`continueAttack` (no swing, no block breaking, no melee) while a firearm is held. `WeaponItem.use()` becomes aim-only: `startUsingItem`, use duration 72,000, `releaseUsing` ends it, `onUseTick` never fires. `FiringLogic.isAiming` reads `player.isUsingItem() && player.getUseItem() == stack`. The iron-sights pose: the `using_item` transformation in `tools/models.py` centres the model (tune the translation so the sights sit on the crosshair in first person); the scope mixins keep their zoom-factor gate, so red dot/holo/no optic get no zoom and no overlay by construction. Game tests: a fire payload with a valid weapon fires exactly once and respects the cooldown; a payload while holding no weapon does nothing; use never decrements ammo; auto payloads faster than the fire rate are rejected. Update `docs/spec` client checklist rows on FA-14's ticket text if they mention right-click firing.

## Acceptance criteria

- [ ] Left click fires (semi/pump once per press, auto while held), validated server-side through the payload; a use press never fires; sneaking is no longer read for aiming.
- [ ] Right click held aims for exactly as long as held, no loop; magnifying optics zoom with the overlay, red dot/holo/none give the centred iron-sights pose without zoom or overlay.
- [ ] While a firearm is held, left click never swings, breaks a block or melee-hits.
- [ ] Reload bound to a key or the empty use press, recorded in the spec.
- [ ] Game tests for the payload path; `just check` green; spec synced; merged through a Forgejo pull request into `development`.

