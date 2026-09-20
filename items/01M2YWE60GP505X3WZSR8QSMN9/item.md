---
schema_version: 1
id: 01M2YWE60GP505X3WZSR8QSMN9
key: FA-9
type: feat
title: "Item models: composite layers, condition on slot components, display transforms"
created_by: kevin
created_at: 2026-09-20T07:45:36Z
---

## Scope

One composite item model per base weapon: a base layer plus one `minecraft:condition` layer per
slot the class has, each gated on `minecraft:has_component` against that slot's own
`firearms:attachment_<slot>` type — zero mixin (`04-architecture.md` `ARCH-DEC-006`,
`WEAPON-REQ-012`). A display transform on the base layer distinguishing aiming vs. hip-fire, keyed
on whether the player is currently using the item (`ItemDisplayContext`). One model file per base
weapon, never one per base×attachment combination. Not the scope mixins (`FA-10`) and not the
tooltip (`FA-11`).

## Approach

Confirm the exact `minecraft:select`/`minecraft:condition` JSON shape against the 26.2 item-model
schema at this ticket (research `smithing-and-item-model-layers-26-2.md` §B.2, §B.3, cited by
`ARCH-DEC-006`); this only holds because each slot is its own directly-valued component
(`ARCH-DEC-005`) — a combined map or any derived branch key would need the accessor-mixin fallback
`create_metered_motor`'s `MM-15` already used once, which this ticket does not need. Textures and
layer art are drawn fresh for each weapon's own real-world designation; no Minecraft, Create Fly or
third-party asset is copied (`operations/compliance.md`).

## Acceptance criteria

- [ ] Attaching visibly changes the composite item model's layers in a real client render, and a
      filled slot's layer never reverts (client checklist item).
- [ ] A shotgun (no optic/grip/stock slots) never renders a layer for a slot its class lacks.
- [ ] The aiming vs. hip-fire display transform is visibly distinct on at least one weapon.
- [ ] No texture, model file, or string anywhere in this ticket's assets references PUBG or its
      branding (`COMP-REQ-002`, enforced by `SourceSurfaceTest`'s existing scan).
- [ ] `just check` green.

## Constraints and prior findings

`docs/spec/04-architecture.md` `ARCH-DEC-005`, `ARCH-DEC-006`, `docs/spec/domains/weapon.md`
`WEAPON-REQ-012`, `013` (item model), `docs/spec/operations/compliance.md` `COMP-REQ-002`. Blocked
by `FA-7` (needs a real filled slot to test the condition layer against, from the smithing attach
recipe).

## Findings

Confirmed directly against the 26.2 merged-deobf jar (`SelectItemModelProperties`,
`ConditionalItemModelProperties`, `ItemModels`, and the `Unbaked` record of each item-model type),
not from memory or docs, since none of this project's own prior notes covered 26.2's exact class
names:

- **`minecraft:component` select property exists** (`ComponentContents`, registered under the id
  `"component"`) and reads any normally-registered `DataComponentType`'s raw decoded value via that
  component's own persistent codec — no mixin needed, confirming `ARCH-DEC-006`'s premise for both
  slot components (`firearms:attachment_<slot>`, an `Identifier`, matched by a plain string `when`)
  and the base-weapon select. For `firearms:base` (`{version, weapon_id}`, `BaseCodec`) the `when`
  value need not repeat `version`: `BaseCodec.CODEC` decodes `version` via `optionalFieldOf` back
  to `1`, so `{"weapon_id": "firearms:m1911"}` alone decodes to the same `Base` value every real
  crafted weapon carries and compares equal — `items/weapon.json` uses this, not a per-base
  `condition` chain.
- **`minecraft:has_component` (`HasComponent`) gates slot presence** exactly as `ARCH-DEC-006`
  describes: `{"property": "minecraft:has_component", "component": "firearms:attachment_<slot>"}`.
- **`minecraft:component_matches` (`ComponentMatches`) is *not* usable for this mod's own
  components.** It wraps `net.minecraft.core.component.predicates.DataComponentPredicate`, a closed
  registry of vanilla-only predicate types (`damage`, `trim`, `enchantments`, `potions`,
  `fireworks`, `custom_data`, `container`, `attribute_modifiers`, `written_book`, `writable_book`,
  `bundle`, `jukebox_playable`, `villager_type`) with no extension point for a mod's own
  `DataComponentType` — confirmed by decompiling `DataComponentPredicate` and its sibling classes:
  none references `firearms:base` or `firearms:attachment_<slot>`, and no mixin adds one. Every
  identity/value branch in this ticket's assets goes through `minecraft:component` (select) or
  `minecraft:has_component` (presence only) instead.
- **A using-state display condition exists and needed no mixin or client code at all**:
  `minecraft:using_item` (`IsUsingItem`, a zero-field boolean condition — true while the holding
  entity is actively using the item stack). This is what the base layer's aiming-vs-hip-fire
  transform is keyed on (`items/weapon.json`'s `base_layer` is a `minecraft:condition` wrapping the
  same base model twice, the `on_true` branch carrying an extra `"transformation"` translation) —
  not a `minecraft:display_context` select on `ItemDisplayContext` as the ticket's own prose
  guessed; that select property also exists (`DisplayContext`, id `"display_context"`) but answers
  a different question (which context is rendering: gui/thirdperson/firstperson/...), not whether
  the item is mid-use. No `ItemDisplayContext` value itself carries a "using" state — `IsUsingItem`
  reads `LivingEntity`'s own use-item state, entirely client-side, so FA-10/FA-11 need nothing from
  this ticket to render an aim pose; it is not deferred to them.
- **`minecraft:model`'s implementing class in 26.2 is `CuboidItemModelWrapper$Unbaked`**, not the
  `BasicItemModel` name older notes might expect — but its JSON shape is unchanged:
  `{"type": "minecraft:model", "model": "<ref>"}` plus optional `"transformation"`/`"tints"`,
  resolving `"model"` to an ordinary `models/item/**.json` (`"parent": "minecraft:item/generated"`,
  `"textures": {"layer0": ...}`) exactly as before.
- **`minecraft:empty` exists** (`EmptyModel$Unbaked`, no fields) — used as the "nothing attached"
  leaf on both sides of a slot's `has_component` condition's `on_false` and as a slot select's
  `fallback`, so an empty slot renders nothing rather than reverting to a wrong attachment.
- **Layering avoids per-(weapon × attachment) texture combinatorics.** Every attachment's
  weapon-layer overlay is drawn once, on a transparent canvas the same 32x16 size as every base
  weapon texture, with the glyph placed at that slot's own fixed anchor
  (`tools/sprites.py::SLOT_ANCHOR`) — not baked per weapon. The same 22 overlay textures stack under
  `minecraft:composite` against any of the six base layers, so the asset count is `6 base + 22
  attachment (icon + overlay, one glyph reused for both) + 6 cartridge`, not `Σ(weapon × its
  slots' attachments)`.
- `just check` (`lint` + `map-check` + `test-java` + `test-tools` + `gametest`) is green:
  `BUILD SUCCESSFUL in 17s, 7 actionable tasks: 1 executed, 6 up-to-date`; `ModelAssetsTest` (5
  tests) and `SourceSurfaceTest` (3 tests, its PUBG/branding scan now reading resource files as raw
  bytes through ISO-8859-1 rather than strict UTF-8 so it can cover the new PNGs without throwing)
  both pass; the game-test server logs "All 26 required tests passed".
