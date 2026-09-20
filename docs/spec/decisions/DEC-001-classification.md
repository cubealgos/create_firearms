---
title: "create_firearms DEC-001 — Distributed product, full spec sheet"
type: "spec"
category: "create_firearms"
---

# `DEC-001` — Distributed product, full spec sheet

**Status:** decided by Kevin, 2026-09-20.

The fifth Create Fly add-on built on the way to `create_civilization`, and the largest yet: two
recipe front ends, an entity, seven components, three client mixins, and a villager-trade
extension, against the smallest-surface sibling (`create_synthetic_diamonds`, zero mixin, no new
entity) and the largest before it (`create_villager_customers`, two mixin targets). Like all four
siblings, it ships to real users on Modrinth, not only into the civilization mod, and follows the
same process: the full sheet, so the release path, the compliance table and the testing layers are
decided once, up front. The pipeline is the same as every sibling's: Sonnet subagents draft, Claude
reviews, Kevin reviews the findings.

Alternative considered: treating it as internal tooling and skipping §5–§7 (interface contracts,
compliance, release engineering), since `create_civilization` is the real goal. Rejected for the
same reason as all four siblings, more sharply here: a mod adding real-world-designated firearms
combat needs its compliance table (naming, Modrinth content rules) settled before the first line of
Java, not after. Cost if wrong: an evening of spec for the family's largest mod yet.
