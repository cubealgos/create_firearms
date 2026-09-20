---
title: "create_firearms DEC-012 — Weaponsmith and fletcher trades, no new gunsmith profession"
type: "spec"
category: "create_firearms"
---

# `DEC-012` — Weaponsmith and fletcher trades, no new gunsmith profession

**Status:** decided by Kevin, 2026-09-20, accepting the argument he himself asked for.

Kevin's own opening asked for a new "gunsmith" villager profession tied to the (now rejected)
dedicated workstation. The argument against it: a new profession needs a new job-site block and POI
registration — the exact new-block cost `decisions/DEC-006-no-new-block.md` was written to avoid —
and nothing about firearm trades actually requires a dedicated profession, since two existing ones
already fit: the weaponsmith for weapons and attachments, the fletcher for ammunition. Both are
extended via a tag-merge into their existing trade level tags, the identical mechanism
`create_metered_motor` already ships for its own toolsmith trade (research
`smithing-and-item-model-layers-26-2.md` §D.2), zero mixin, zero new profession, job site, or POI
(`domains/trade.md` `TRADE-DEC-001`).

Alternative considered: the new gunsmith profession Kevin's own opening asked for. Rejected on
Kevin's own acceptance of the argument against it, once the workstation itself was already off the
table — a new profession's entire justification (serving a dedicated workstation) disappeared with
`DEC-006`. Cost if wrong: none identified — the two-existing-profession shape scales to more
professions later if a genuine need turns up, without touching anything decided here.
