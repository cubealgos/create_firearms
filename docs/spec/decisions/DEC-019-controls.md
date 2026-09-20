---
title: "create_firearms DEC-019 — Controls: left click fires, right click held aims; iron sights without an optic; a creative tab"
type: "spec"
category: "create_firearms"
---

# `DEC-019` — Controls: left click fires, right click held aims

**Status:** decided by Kevin, 2026-09-20, in the first client session of the mechanics: "we have to
change inputs as well: currently scoping happens on right click which is fine, but shooting should
happen with left click when in scope, otherwise scoping instantly fires the weapon"; on the AWM:
"holding right click gets it in scope, then it resets and instantly rescopes, no shot fired; it
doesn't even do the full zoom"; "do we have a red dot sight already? how would weapons without a
scope attachment work?"; and "we should register a creative tab for all items".

## The scheme

| Control | Held weapon | Effect |
|---|---|---|
| Attack (left click, `key.attack`) | any firearm | Fires: `semi` and `pump` once per press, `auto` repeats while held at the fire rate. Hip-fire when not aiming, aimed when aiming. Never swings, never breaks a block, never melee-hits (a bullet does the hitting). |
| Use (right click, `key.use`), held | any firearm | Aims: the weapon is "in use" for as long as the button is held (a bow-length use duration that never finishes on its own), the aiming pose replaces the hip pose, spread narrows per `WEAPON-REQ-005`. With a magnifying optic the view zooms and the overlay draws (`COMBAT-REQ-006`..`008`). With a red dot or holo, or no optic, the pose is iron sights: the model comes to the screen centre, no zoom, no overlay, the FOV unchanged. |
| Use released | | Back to the hip pose. |
| Reload | | Unchanged (`WEAPON-REQ-010`): a use press when the magazine is empty and no aim is possible reloads; `UC-010` decides the exact key at FA-24 (proposed: the use press while empty, or a dedicated key `R` registered through Fabric's key-binding API, whichever FA-24's research finds cleaner; a dedicated key is preferred so an empty weapon can still be aimed). |

Firing is a client input, so the attack press travels as this mod's own client-to-server payload
(`ServerboundFirePayload`, Fabric `ClientPlayNetworking.send`), validated server-side by the same
`FiringLogic` that ran from `use()` before: cooldown, ammo, durability and the bullet spawn stay
authoritative on the server (`COMBAT-DEC-003`). `auto` is driven by the client sending one payload
per fire-rate interval while the key is held; the server's cooldown rejects anything faster.
Sneaking is no longer the aiming stand-in (`FiringLogic.isAiming` reads the use state).

## Why the AWM looped

`WeaponItem.use()` fired on the press and the use action ended with the shot (or the cooldown),
so a held right click restarted the use every few ticks: scope, reset, scope. Separating aim
(use, held) from fire (attack) removes the loop by construction; the use duration becomes 72,000
ticks like a bow's, ended only by release.

## Creative tab

One `CreativeModeTab` `firearms:firearms` listing the six bare weapons, one fully loaded
example per class, every attachment item, and the six cartridges, with the M1911 as its icon
(`UI-REQ-007`).

## Cost if wrong

One payload, one client key handler, a client mixin cancelling the attack swing while a firearm is
held, and the `use()` rewrite; the server-side firing path is untouched. Reverting to use-to-fire is
deleting the payload.
