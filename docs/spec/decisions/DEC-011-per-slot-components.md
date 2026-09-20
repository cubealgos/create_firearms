---
title: "create_firearms DEC-011 — Per-slot data components, not one combined attachment map"
type: "spec"
category: "create_firearms"
---

# `DEC-011` — Per-slot data components, not one combined attachment map

**Status:** decided by Kevin, 2026-09-20, following the research's structural finding.

Seven component types: `firearms:base`, `firearms:ammo`, and one `firearms:attachment_<slot>` per
slot (`04-architecture.md` `ARCH-DEC-005`), rather than the proposal's original single combined
`attachments` map component. Research found two independent engine reasons the split is required,
not merely convenient (`smithing-and-item-model-layers-26-2.md` §B.2, §D.3): a combined map cannot
drive a zero-mixin `minecraft:condition`/`minecraft:has_component` item-model layer without a
derived value, which would force the same accessor-mixin fallback `create_metered_motor`'s MM-15
already needed once; and a villager `wants` predicate (`DataComponentExactPredicate`) matches a
named component's value as a whole, so only a per-slot shape lets a master buy-back trade name just
the slot(s) it cares about and leave the rest free — exactly what `decisions/DEC-013-master-buy-back.md`
depends on.

Alternative considered: the proposal's own original single-map shape. Superseded by the research
finding above, not by a Kevin ruling against it directly — Kevin's ruling ("per-slot data
components... so `minecraft:condition`/`select` item models and trade predicates work without
mixins") confirms the research's own recommendation rather than overruling it, the only place in
this sheet where the research's technical finding and Kevin's ruling are the same decision stated
twice. Cost if wrong: none — the per-slot shape is strictly more capable than the combined map for
every consumer this mod has (`domains/weapon.md`, `domains/attach.md`, `domains/trade.md`).
