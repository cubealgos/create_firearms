---
schema_version: 1
id: 01M2YW8TSYQ18TTW0MBH4S1AH1
key: M2
title: Combat
status: backlog
created_at: 2026-09-20T07:42:41Z
---

## Goal

A weapon actually fires: the bullet entity flies, hits and despawns under vanilla damage rules,
and the fire control loop (ammo, cooldown, fire mode, reload, durability) drives it correctly.

## Scope

The bullet entity — velocity, gravity, per-tick swept-segment hit test, `firearms:bullet` damage
type, knockback, fixed life, water behaviour, shotgun pellets (`FA-5`); firing itself — the fire
control per `fire_mode` (semi/auto/pump), the fire-rate cooldown, ammo drawn from
`firearms:ammo`, reload scanning inventory by calibre, empty-click on no ammo, per-shot durability
loss, sounds, server-authoritative spread, and the cosmetic client-side recoil packet
(`COMBAT-DEC-003`) (`FA-6`). Not either attach recipe front end (`M3`) and not the scope mixins,
which need an attached optic that does not exist until `M3` (`M4`).

## Exit criteria

- A weapon with known components fires, decrements ammo and durability, starts the correct
  cooldown, and refuses to fire at zero ammo.
- The bullet's hit detection is proven correct at both low and very high muzzle velocity, and
  against a target beyond 40 blocks (`TEST-REQ-004`) — no tunneling regardless of speed.
- A shotgun trigger pull spawns 8 independently-spread pellets and consumes exactly one round.
- Every `DamageSource` this mod constructs follows the direct/causing-entity convention so `pvp`
  and team-allegiance checks apply automatically.

## Tickets

FA-5, FA-6.

## Depends on

M1 (the stat derivation function firing reads, and the data components it reads from).
