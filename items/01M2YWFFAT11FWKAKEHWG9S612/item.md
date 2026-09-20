---
schema_version: 1
id: 01M2YWFFAT11FWKAKEHWG9S612
key: FA-19
type: feat
title: Create automation for cartridges
created_by: kevin
created_at: 2026-09-20T07:46:19Z
---

## Scope

The Create automation path for cartridges deferred at 1.0 by `AMMO-REQ-005`: `create:pressing`
(brass sheet from a brass ingot, already Create Fly's own recipe) feeding `create:mixing`
(gunpowder + iron nuggets + the pressed casing, combined in one basin step, optionally
`heat_requirement: heated`). The plain crafting-table recipe (`FA-3`) remains the non-automated
baseline regardless, since Create's processing recipe types and vanilla crafting recipes are
independent, non-exclusive registrations on the same output item.

## Approach

Zero new Java: both recipe types are Create Fly's own existing processing recipe types, parametrized
by data files the same way `FA-3`'s crafting recipes already are. Confirm the exact basin/mixing
JSON shape against a working Create Fly example at this ticket.

## Acceptance criteria

- [ ] Each of the six cartridges has a working `create:pressing` + `create:mixing` automation chain,
      alongside its existing crafting-table recipe.
- [ ] The crafting-table recipe still works unchanged (`AMMO-REQ-001` unaffected).
- [ ] `just check` green.

## Constraints and prior findings

`docs/spec/domains/ammo.md` `AMMO-REQ-005`, §3 "Deferred: Create automation path". Backlog, `M7`;
not scoped for 1.0. Blocked by `FA-16` (ships only after 1.0 stabilizes).
