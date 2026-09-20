---
title: "create_firearms spec — compliance, security and governance"
type: "spec"
category: "create_firearms"
---

# Compliance, security and governance (`COMP`, sheet §6)

A distributed product carries the same obligations as its four siblings, plus one this family has
not needed before: naming and branding compliance for a weapons-themed mod drawing on a named
commercial game as a design reference.

| Area | Position |
|---|---|
| GDPR: what leaves the user's machine | Nothing. No telemetry, no update check, no outbound network call of any kind (`COMP-REQ-001`). No player data of any kind is touched beyond the item components `contracts/data-contract.md` already covers. |
| Naming and branding | Every weapon uses only its real-world designation (M1911, Micro Uzi, AKM, Ruger Mini-14, AWM, Winchester Model 1897) — these are firearm model names, not any game's property. No PUBG name, branding, logo, splash art, or texture appears anywhere in the mod, its listing, or its repository; PUBG is a design reference for the roster and slot layout only, the same way `create_metered_motor` used the vanilla spyglass as a mechanical reference rather than a branding one (`COMP-REQ-002`). |
| Modrinth content rules | Weapon mods are an established, permitted category on Modrinth; nothing here glorifies real-world violence beyond depicting ordinary firearm mechanics under vanilla damage rules — no gore, no real-world atrocity references, no hate content. Reviewed against Modrinth's published content rules before the first release; revisit if those rules change. |
| Hosted parts we run | None. Modrinth hosts the file and its page. |
| Impressumspflicht | Attaches to a public web presence; there is none beyond the platform pages. Revisit if a site exists. |
| Licence and notices | MIT (`decisions/DEC-003-licence.md`); `NOTICE` credits Create Fly (CC0), Create (MIT), Fabric (Apache-2.0). No Minecraft or Create Fly textures, models, or code are copied — every asset is drawn fresh for this mod's own real-world-designated weapons. |
| Supply chain and release integrity | Builds from a tagged commit with pinned dependencies; the release checksum is in the release notes; no signing at 1.0. |
| Vulnerability disclosure | The public issue tracker only, on the GitHub mirror (`https://github.com/cubealgos/create_firearms/issues`); no private channel, no e-mail address published. Forgejo stays the source of truth for code. |
| Server trust boundary | Every hit result, damage amount, ammo count, and stat derivation runs entirely server-side; the three client mixins affect only local rendering (zoom, overlay, held-item pose) and carry no authority a malicious client could abuse to affect another player's game state (`domains/combat.md` `COMBAT-DEC-003`). |
| AI Act, GoBD, sector regulation | Not applicable: no AI component, no financial records, no regulated sector. Real-world firearm designations are used as descriptive names only, with no connection to any regulated weapons trade. |

`COMP-REQ-001`: the mod shall make no network call of its own; a source-scan test asserts it
(`SourceSurfaceTest`, as all four siblings use).
`COMP-REQ-002`: a source- and asset-scan test asserts no string, texture, or model file anywhere in
the mod's own resources references PUBG, its weapon names, or its branding.

Open: release signing (minisign) before 1.0 or after.
