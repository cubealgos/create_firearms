---
schema_version: 1
id: 01M2Z877MKMSR81V08CR95ND60
key: FA-25
type: feat
title: A creative tab listing every item of the mod
created_by: kevin
created_at: 2026-09-20T11:11:31Z
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

Kevin, 2026-09-20: "we should register a creative tab for all items". `UI-REQ-007`, `decisions/DEC-019-controls.md` §Creative tab: one `CreativeModeTab` `firearms:firearms`, iconed with the M1911, listing the six bare weapons, one fully loaded example per class (built through the real `Attach` function, loaded to capacity, the same way `debug give` builds one), every attachment item (one stack per attachment id, with its component set) and the six cartridges, in that order.

## Approach

`FabricItemGroup.builder()` registered in `BuiltInRegistries.CREATIVE_MODE_TAB` from `Firearms` init; `displayItems` builds the stacks from the data-driven weapon and attachment catalogues so a datapack-added weapon appears too; lang key `itemGroup.firearms.firearms` = "Create: Firearms". A game test asserting the tab exists and its display list holds 6 bare weapons, 6 loaded, 22 attachments and 6 cartridges with valid components.

## Acceptance criteria

- [ ] The tab appears in the creative inventory with the M1911 icon and every item listed as above.
- [ ] Game test for the tab's contents; `just check` green; merged through a Forgejo pull request into `development`.

