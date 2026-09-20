---
title: "create_firearms spec — UI: tooltips, the scope overlay, and what is deliberately absent"
type: "spec"
category: "create_firearms"
---

# `UI` — what a player sees, and what they deliberately do not

## 1. Purpose

The weapon tooltip's content; the scope overlay as a player-facing surface (the mechanism itself is
`domains/combat.md` `COMBAT-REQ-008`); whether hit markers exist; the debug command. Not the
smithing table or deployer screens themselves, which are Create's and vanilla's own, unmodified.

## 2. Dimensions

| Dimension | Answer |
|---|---|
| **Actors** | The player (`ACTORS-001`) reads a tooltip or looks through a scope; the client renders both from server-computed or server-synced data, never inventing either locally. |
| **Over time** | A tooltip recomputes on every hover, from whatever components the item currently carries — never cached across an attach. The scope overlay appears only while `COMBAT-REQ-006`'s widened `isScoping()` gate holds, and disappears the instant it releases. |
| **Multiplicity** | One tooltip per item stack; one overlay texture per zoom-bearing optic (six, since red dot and holo never trigger it); zero hit markers, zero new screens. |
| **Unwanted** | A tooltip that shows stale stats after gaining an attachment (prevented structurally, since the tooltip calls the same live stat function firing does — `domains/weapon.md` `WEAPON-REQ-003`); a scope overlay bleeding through to an unrelated item once the widened gate is released. |
| **Not-you** | A player who never picks up a weapon of this mod's own never sees a tooltip line, an overlay, or a debug command output that did not exist before installing it. |

## 3. Enumerations

### What exists vs. what does not, at 1.0

| Surface | Present? |
|---|---|
| Weapon tooltip (derived stats, attached components) | Yes |
| Scope overlay (per zoom-bearing optic) | Yes, via `domains/combat.md` `COMBAT-REQ-008` |
| Hit markers (a confirmation cue on a successful hit) | No at 1.0 |
| New screen of this mod's own | No — attaching uses the vanilla smithing table screen unmodified |
| JEI/EMI recipe-viewer category | No at 1.0, deferred like `create_synthetic_diamonds`'s own pressing recipes (`UI-DEC-001`) |
| Debug command | Yes, development-environment only |

### Tooltip content

| Line | Source |
|---|---|
| Weapon name and class | `firearms:base`'s `weapon_id` |
| Damage, muzzle velocity, spread, fire rate, fire mode, magazine size, reload ticks, recoil, durability remaining | The live stat derivation function (`domains/weapon.md` `WEAPON-REQ-003`), run fresh on every tooltip render |
| Attached components, per slot | Each present `firearms:attachment_<slot>`'s own display name; an empty slot the weapon's class has is shown as unequipped, never hidden |
| Loaded ammunition | `firearms:ammo.loaded` / derived magazine size, where present |

## 4. Use cases

`UC-011`, `UC-012`, `UC-018` in `02-journeys.md`.

## 5. Requirements

| ID | Requirement | Priority | From |
|---|---|---|---|
| `UI-REQ-001` | The system shall render a tooltip on every weapon item listing its derived final stats and every slot's occupant (or "empty," for a slot the class has but nothing fills), recomputed on every hover rather than cached on the item. | Must | `UC-018` |
| `UI-REQ-002` | The system shall draw a scope overlay only while `domains/combat.md` `COMBAT-REQ-006`'s widened `isScoping()` gate holds, using that optic's own texture (`COMBAT-REQ-008`); red dot and holo, which never satisfy the gate, draw no overlay. | Must | `UC-011`, `UC-012` |
| `UI-REQ-003` | The system shall add no hit-marker or hit-confirmation cue of its own at 1.0. | Must, scoped out | `UI-DEC-002` |
| `UI-REQ-004` | The system shall add no screen of its own for attaching or otherwise configuring a weapon: the vanilla smithing table screen and Create's deployer are the only interaction surfaces (`00-context.md`). | Must | `00-context.md` |
| `UI-REQ-005` | The system shall add no JEI or EMI recipe-viewer category at 1.0; the smithing and deploying recipes function regardless of whether either is displayed anywhere (`UI-DEC-001`). | Should, scoped out | `decisions/DEC-015-jei-deferred.md` |
| `UI-REQ-006` | The system shall provide a development-environment-only debug command printing a given item stack's derived final stats and component state, for verification without relying on hovering. | Should | `operations/testing.md` |
| `UI-REQ-007` | The system shall register one creative-mode tab `firearms:firearms` holding every item of the mod: the six bare weapons, one fully loaded example per class, every attachment item and every cartridge, iconed with the M1911. | Must | Kevin, 2026-09-20; `decisions/DEC-019-controls.md` |

## 6. Failure modes

| ID | Failure | Response |
|---|---|---|
| `UI-FAIL-001` | A player expects a hit marker after landing a shot | None exists at 1.0; the only feedback is the target's own hurt animation/sound, exactly as for an arrow. |
| `UI-FAIL-002` | A player opens JEI expecting to see this mod's smithing/deploying recipes | Create Fly's own recipe-viewer categories were not extended by this mod at 1.0; the tables in `domains/attach.md` and `domains/ammo.md` are the documented reference instead, the same fallback `create_synthetic_diamonds` uses for its own recipes. |

## 7. Open questions

None at 1.0: hit markers and JEI integration are both explicit scope decisions, not open questions
(`UI-DEC-001`, `UI-DEC-002`).

## 8. Decisions

- `UI-DEC-001` — **No JEI/EMI category at 1.0**, mirroring `create_synthetic_diamonds`'s own
  `DEC-009`: the smithing and deploying recipes work regardless of whether a recipe viewer displays
  them, and a self-contained category is additive future work, not a 1.0 blocker.
- `UI-DEC-002` — **No hit markers at 1.0** (this sheet's own scope call, not a Kevin ruling): a hit
  marker needs a server-to-client hit-confirmation signal beyond what vanilla's own hurt animation
  already gives for free, and nothing in `rulings-2026-09-20.md` asks for one. **Cost if wrong:** a
  small additive S2C cue is a later ticket, not a redesign of `domains/combat.md`.
