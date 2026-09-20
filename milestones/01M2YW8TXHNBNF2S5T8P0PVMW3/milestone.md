---
schema_version: 1
id: 01M2YW8TXHNBNF2S5T8P0PVMW3
key: M4
title: Client
status: backlog
created_at: 2026-09-20T07:42:41Z
---

## Goal

Everything a player actually sees and feels: item models that reflect attached components, the
scope mechanic Kevin asked to reuse from the vanilla spyglass, tooltips, recoil, and a dev tool for
checking derived stats without hovering.

## Scope

Composite item models with one `minecraft:condition` layer per slot, gated on
`minecraft:has_component`, plus display transforms for aiming vs. hip-fire (`FA-9`); the three
client-only scope mixins — widened `Player.isScoping()`, the per-optic FOV factor, the per-optic
overlay texture — all three gated on the same widened check (`FA-10`); the weapon tooltip (derived
stats, every slot's occupant or "empty") and the cosmetic recoil camera kick (`FA-11`); the
dev-environment-only `/firearms debug give <base> [attachments...]` and `/firearms debug stats`
commands (`FA-12`). Not trades (`M5`).

## Exit criteria

- Attaching visibly changes the composite item model's layers, and a filled slot's layer never
  reverts.
- The scope mixins zoom, overlay and suppress the held-item render for a magnified optic, and do
  none of those for red dot or holo (`COMBAT-DEC-004`).
- The tooltip recomputes on every hover from whatever components the item currently carries.
- `/firearms debug give` and `/firearms debug stats` work only in a development environment.

## Tickets

FA-9, FA-10, FA-11, FA-12.

## Depends on

M3 (a weapon with an attached optic to zoom through and a filled slot to render).
