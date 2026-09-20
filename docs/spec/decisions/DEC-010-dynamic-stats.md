---
title: "create_firearms DEC-010 — Stats are dynamic, a pure function of base plus attachments, never baked"
type: "spec"
category: "create_firearms"
---

# `DEC-010` — Stats are dynamic, a pure function of base plus attachments, never baked

**Status:** decided by Kevin, 2026-09-20.

Kevin, verbatim: "I want the weapon mod to be somewhat realistic and stats to be dynamic." A
weapon's final stats are computed fresh, every time they are needed (firing, tooltip, trade
display), as a pure function of `firearms:base`'s weapon id plus whichever
`firearms:attachment_<slot>` components are present — never written back to the item
(`domains/weapon.md` `WEAPON-REQ-003`; `04-architecture.md` `ARCH-DEC-005`). This is the direct
mechanism behind Kevin's "easy upgrade paths": filling a further, still-empty slot later needs no
recomputation step beyond the function itself running again on the larger set of present
components — unchanged by `decisions/DEC-017-no-detach-durability.md`'s later ruling that a filled
slot itself is permanent, since this function never needed a detach to work correctly in the first
place.

Alternative considered: baking final stats onto the item at craft/attach time, refreshed only on
the next attach event. Rejected: it would need an explicit recompute-and-write step on every
mutation this mod or a future one might perform on a weapon's components, and risks a
tooltip/actual-firing mismatch if any path forgets to trigger it. A pure function has no such path
to forget. Cost if wrong: none identified — the pure-function cost (recomputing on every read) is
negligible against the correctness it buys, and `operations/testing.md` treats it as the mod's most
straightforwardly unit-testable surface.
