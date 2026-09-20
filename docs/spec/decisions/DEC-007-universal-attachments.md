---
title: "create_firearms DEC-007 — Universal attachments by slot, full attachment set ships at 1.0"
type: "spec"
category: "create_firearms"
---

# `DEC-007` — Universal attachments by slot, full attachment set ships at 1.0

**Status:** decided by Kevin, 2026-09-20.

Kevin, verbatim: "attachments work on all weapon types that accept an attachment, so an SMG and a
sniper rifle can use the same suppressor." One item per slot option, universal across every class
that has the slot, rather than PUBG's own per-weapon or per-calibre attachment variants — 22
attachments at 1.0 (3 muzzle, 8 optic, 3 magazine, 5 grip, 3 stock, `domains/attach.md`), all of
them, unlike the weapon roster which is deliberately cut down (`decisions/DEC-005-roster.md`). Fit
is governed purely by an attachment's own slot against the target weapon's class's slot set
(`domains/weapon.md` §3), never by a weapon-specific or calibre-specific attachment variant.

Alternative considered: PUBG's own per-weapon/per-calibre attachment variants (e.g. a distinct
suppressor for each calibre). Rejected outright by Kevin's own framing above — simplification to
universal slots is the explicit point, not a fallback if the variant approach proves hard. Cost if
wrong: none — universal attachments are strictly less data and less Java than per-weapon variants
would be.
