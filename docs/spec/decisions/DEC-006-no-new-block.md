---
title: "create_firearms DEC-006 — No new block: the smithing table and the deployer replace the workstation"
type: "spec"
category: "create_firearms"
---

# `DEC-006` — No new block: the smithing table and the deployer replace the workstation

**Status:** decided by Kevin, 2026-09-20, accepting the argument he himself asked for.

Kevin's own opening asked to be argued out of a dedicated workstation block. The argument: the
vanilla smithing table already does everything a bespoke workstation would need — a
multi-item-slot recipe with cross-slot component merging, found by the vanilla game with zero
mixin (research `smithing-and-item-model-layers-26-2.md` §A.3) — and Create's own deploying recipe
covers the contraption-facing half the same way (research
`create-fly-potato-cannon-and-deploying-26-2.md` §B.3). One shared attach function behind two
recipe front ends replaces a new block entirely (`04-architecture.md` `ARCH-DEC-002`,
`ARCH-DEC-003`; `domains/attach.md` `ATTACH-DEC-001`). Every base weapon, attachment and cartridge
additionally gets a plain crafting-table recipe (`rulings-2026-09-20.md`: "all bullets +
attachments + bases should have normal crafting recipes"), so nothing in this mod is gated behind
the smithing table or a deployer alone — both are attach front ends, not crafting gates (and, since
`decisions/DEC-017-no-detach-durability.md`, attach-only: attachments never come off, by either
front end).

This is the single decision the rest of the mod's architecture follows from: no new block means no
new job-site block either, which is what makes `decisions/DEC-012-villager-integration.md`'s
"no new profession" possible in turn.

Alternative considered: a dedicated `firearms:workbench` block with its own screen and recipe type.
Rejected: it would duplicate the smithing table's own multi-slot, component-merging machinery for
no capability the smithing table lacks, and would need its own POI/job-site registration to support
a matching villager profession — exactly the cost Kevin asked to be talked out of. Cost if wrong:
none identified — nothing about the smithing-table/deployer shape blocks adding a dedicated block
later if a genuine capability gap turns up.
