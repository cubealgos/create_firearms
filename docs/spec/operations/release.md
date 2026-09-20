---
title: "create_firearms spec — release engineering and distribution"
type: "spec"
category: "create_firearms"
---

# Release engineering, distribution and support (`REL`, sheet §7)

| Item | Position |
|---|---|
| Version scheme | `<mod>+<mc>`, SemVer on the mod part over `contracts/public-surface.md`: `1.0.0+26.2` |
| Branches | gitkontor's: `development`, `production`; releases are tags on `production` |
| Ticket prefix | `FA` |
| Channels | Modrinth only; CurseForge deferred, as all four siblings |
| CI | `just check` on every merge: lint, unit tests, game tests, by the Woodpecker file, live from the first push since the repo is public on Forgejo from the bootstrap (`https://git.cubealgos.de/cubealgos/create_firearms`, GitHub mirror `https://github.com/cubealgos/create_firearms`, which carries the public issue tracker) |
| Always a playable build | `just client` boots with Create Fly at every merge; a player can craft an M1911, attach a suppressor at a smithing table, load `.45 ACP` cartridges, and fire at a mob |
| Support | Issue tracker only; no SLA; a `SUPPORT.md` says so |
| Ports | A new Minecraft version is a new `+<mc>` build from a port branch; the smithing/deploying recipe lookup mechanisms and the three client mixin targets are re-verified against both the new Minecraft jar and the tested Create Fly build on each port |

`REL-REQ-001`: every release jar is built by `just release` from a clean checkout at a tag.
`REL-REQ-002`: the release notes list the Minecraft, Fabric Loader and Create Fly versions tested.
`REL-REQ-003`: the release notes state the 1.0 roster (six weapons, their calibres and classes) and
that every stat and price in this sheet is a proposed starting point, not a balanced final number.
`REL-REQ-004`: the rest of the full PUBG-shaped roster (`rulings-2026-09-20.md`'s proposal) ships
as later data-file releases; each such release is a minor version, never a major one, since adding
a weapon to an existing class breaks no stable surface (`contracts/public-surface.md`
`SURFACE-REQ-003`).
