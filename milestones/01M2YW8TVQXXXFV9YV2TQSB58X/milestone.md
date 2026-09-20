---
schema_version: 1
id: 01M2YW8TVQXXXFV9YV2TQSB58X
key: M3
title: Attach
status: backlog
created_at: 2026-09-20T07:42:41Z
---

## Goal

Both attach front ends — the smithing table for a player, a deployer for a contraption — resolve
through one shared attach function, so a weapon actually gains attachments.

## Scope

The shared attach function (slot-match against an empty `firearms:attachment_<slot>` and merge)
and the smithing recipe class implementing `SmithingRecipe`, found by the vanilla smithing table
with zero mixin (`FA-7`); the deploying recipe class implementing `Recipe<ItemApplicationInput>`
reporting `getType() == AllRecipeTypes.DEPLOYING`, found by a Create deployer with zero mixin,
consuming the held attachment (`FA-8`). Not firing (`M2`, already done) and not the scope mixins
(`M4`), which read the optic slot this milestone first makes fillable.

## Exit criteria

- A real `SmithingMenu` result and a real `DeployerBlockEntity.getRecipe()` result reach
  byte-identical component output for the same base weapon and attachment (`TEST-REQ-003`).
- Neither front end ever matches an already-occupied slot (`ATTACH-REQ-002`).
- A deployer on a real contraption belt visibly attaches an attachment to a weapon.

## Tickets

FA-7, FA-8.

## Depends on

M1 (the per-slot attachment components and the attachment data files) and M2 (a fireable weapon to
attach onto, so the client checklist has something to test end to end).
