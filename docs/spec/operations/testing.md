---
title: "create_firearms spec — testing"
type: "spec"
category: "create_firearms"
---

# Testing (`TEST`)

| Layer | What | Where |
|---|---|---|
| Unit | The stat derivation function (base + every combination of present slot components → expected final stats, including the fixed application order); the attach function's slot-match/merge logic in isolation from either recipe class; the weighted spread roll given a fixed random source; the weight/modifier clamp-and-log path (`contracts/public-surface.md` `SURFACE-REQ-002`) — all pure, no Minecraft imports needed, checked by a package-purity check as the four siblings use | `src/test` |
| Game tests | Firing: a weapon with known components fires, decrements ammo and durability, starts the correct cooldown, and refuses to fire at zero ammo; the bullet entity's hit detection against a real target at both low and very high muzzle velocity (`domains/combat.md` `COMBAT-REQ-002`), proving no tunneling regardless of speed; a shotgun's 8-pellet spawn and single-round consumption; attaching on **both** front ends — a real `SmithingMenu` result for the smithing path, a real `DeployerBlockEntity.getRecipe()` result for the deploying path — proving they reach the identical component state via the shared attach function, and that neither ever matches an already-occupied slot (`domains/attach.md` `ATTACH-REQ-002`); a real `AnvilMenu.createResult()` against a damaged weapon confirms no material repair and no book-enchant, and separately confirms `WEAPON-FAIL-006`'s same-item combine-repair gap either does or does not fire, settling the open question; a real enchanting table offers no enchantment for a weapon item; the villager-trade tag-merge actually appears in a real weaponsmith's/fletcher's generated offers at the right level; a master buy-back trade's `ItemCost.test()` accepts a matching configuration and rejects a mismatched one on an unconstrained vs. a constrained slot | `src/gametest`, Loom `runGameTest` |
| Client / Kevin's checklist | Firing feel: muzzle flash, tracer, cosmetic recoil kick, sounds, at each of the six 1.0 weapons' own fire modes; the scope mixins actually zoom, overlay, and suppress the held-item render for a magnified optic and do none of those for red dot/holo; attaching visibly changes the composite item model's layers, and a filled slot's layer never reverts; a deployer on a real contraption belt visibly attaches to a weapon; confirming no JEI/EMI entry appears anywhere at 1.0 (`domains/ui.md`) | release checklist |
| Development tool | `/firearms debug stats <item>` prints a given item stack's derived final stats and full component state, for verification without hovering (`domains/ui.md` `UI-REQ-006`); registered only when Fabric reports a development environment, as the siblings' own debug commands are | `firearms.debug`, `just client` |

**What is genuinely hard here, stated plainly:** the stat derivation function, the attach function,
and the spread roll are all fully unit-testable given fixed inputs — pure algorithms with no
Minecraft dependency once components and a random source are supplied. What is **not**
pure-testable is whether the two recipe classes are actually found and run by a real
`SmithingMenu`/`DeployerBlockEntity` (depends on `RecipeManager`/`RecipeMap` behaving as the
research's disassembly predicts, `04-architecture.md` `ARCH-FAIL-002`), whether the three scope
mixins actually produce a visible zoom/overlay/suppression against a real client renderer, and
whether automatic-fire bullet-entity volume is acceptable at real server tick rates
(`04-architecture.md` `ARCH-FAIL-004`) — all three need a running client or server to confirm.

`TEST-REQ-001`: every `WEAPON-REQ`, `ATTACH-REQ`, `AMMO-REQ`, `COMBAT-REQ`, `TRADE-REQ` and `UI-REQ`
names its test in the ticket that implements it.
`TEST-REQ-002`: a deliberate-break proof for the pure-package check, once.
`TEST-REQ-003`: a game test proves the smithing attach recipe and the deploying attach recipe reach
byte-identical component output for the same base weapon and attachment, since both call the one
shared attach function (`domains/attach.md` `ATTACH-REQ-008`).
`TEST-REQ-004`: a game test fires each of the six 1.0 weapons at a target placed beyond 40 blocks —
comfortably inside every weapon's muzzle-velocity/despawn-life range — and confirms a hit still
registers, proving the swept-segment hit test holds over real travel distance, not just at
point-blank range.
