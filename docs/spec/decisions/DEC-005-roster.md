---
title: "create_firearms DEC-005 — One weapon per class, six calibres, none repeated, at 1.0"
type: "spec"
category: "create_firearms"
---

# `DEC-005` — One weapon per class, six calibres, none repeated, at 1.0

**Status:** decided by Kevin, 2026-09-20.

1.0 ships six weapons, one per class, six calibres, none repeated: M1911 (`.45 ACP`, pistol),
Micro Uzi (`9mm`, SMG), AKM (`7.62mm`, assault rifle), Ruger Mini-14 (`5.56mm`, DMR), AWM
(`.300 Magnum`, sniper rifle), Winchester Model 1897 (`12 gauge`, shotgun) — out of the full
PUBG-shaped roster the proposal listed (18 weapons across the same six classes). Every one of the
22 attachments ships at 1.0 regardless: the roster is the smaller dial, the attachment set is not
(`domains/weapon.md` `WEAPON-DEC-002`). The rest of the roster ships as later data-file additions,
costing zero new Java per weapon (`domains/weapon.md` `WEAPON-DEC-003`).

Every weapon uses its real-world designation only, never a PUBG name — the roster follows PUBG's
own set as a design reference, not a naming or branding source (`operations/compliance.md`
`COMP-REQ-002`).

Alternative considered: shipping the full 18-weapon roster at 1.0, exercising every calibre
multiple times. Rejected: a six-weapon roster already exercises every 1.0 slot, every attachment,
and every calibre at least once, which is the actual goal (proving the mechanism generalizes)
without the extra balance and asset surface of 12 more weapons that add coverage, not new
mechanism. Cost if wrong: none — the data-file addition path exists specifically so under-shipping
at 1.0 is reversible without a rewrite.
