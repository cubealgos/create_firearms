---
title: "create_firearms DEC-004 — Java 25, Gradle 9.5.1, Loom 1.17, Kotlin DSL, version catalog, one Gradle project, three client-only mixin targets"
type: "spec"
category: "create_firearms"
---

# `DEC-004` — Java 25, Gradle 9.5.1, Loom 1.17, Kotlin DSL, version catalog, one Gradle project, three client-only mixin targets

**Status:** decided by Kevin, 2026-09-20.

The same toolchain as all four siblings, verified in the vault's Minecraft notes: Java 25, Gradle
9.5.1, Loom 1.17, Kotlin DSL with a version catalog. One Gradle project — the pure surface (the
stat derivation function, the shared attach function, the spread roll) is small enough that a
package-purity check gives the same guarantee a second module would, the same reasoning every
sibling gives (`operations/testing.md`).

**Mixin targets are all client-side and all exist for one reason: `Player.isScoping()` is an exact
`Items.SPYGLASS` identity check** (research `smithing-and-item-model-layers-26-2.md` §C.1). Three
small `@Mixin`s — on `isScoping()` itself, the FOV constant, and the overlay texture read — cover
the whole scope mechanic (`04-architecture.md` `ARCH-DEC-001`). **No server-side mixin exists
anywhere in this mod**: both recipe front ends (`ARCH-DEC-002`, `ARCH-DEC-003`) and the
villager-trade extension (`domains/trade.md`) are found through ordinary registry mechanics, the
same zero-server-mixin shape `create_synthetic_diamonds` confirmed for its own recipe class.
`create_villager_customers` needed two server-side mixin targets for its own reasons (no Fabric
event for the villager brain, no non-player checkout API); this mod needs zero server-side, three
client-side, for a different reason again — the closest precedent is `create_metered_motor`'s
MM-15 accessor mixin for a derived `select` property, except here the fix is a widened boolean
identity check rather than an accessor.

Alternative considered: splitting a pure module from the Minecraft-facing one, as
`create_civilization` does. Rejected for the same reason every sibling gives: the pure surface is
small enough that a package check gives the same guarantee at a fraction of the build complexity.
Cost if wrong: if the first ticket finds the zero-server-mixin approach does not work as predicted
for either recipe type, the fallback is one narrowly-scoped mixin per recipe class
(`04-architecture.md` `ARCH-FAIL-002`) — a ticket-sized addition, not a toolchain change.
