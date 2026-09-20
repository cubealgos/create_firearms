---
title: "create_firearms DEC-003 — MIT, no CLA, public under cubealgos from the first commit"
type: "spec"
category: "create_firearms"
---

# `DEC-003` — MIT, no CLA, public under cubealgos from the first commit

**Status:** decided by Kevin, 2026-09-20.

MIT, following Create's own licence and the add-on norm all four siblings already established,
with no CLA; `NOTICE` credits Create Fly, Create and Fabric (`operations/compliance.md`). The
repository is public under the `cubealgos` organisation on Forgejo from the first commit, mirrored
to GitHub with the issue tracker there — the same place every sibling ended up, so this project
starts there instead of retracing that path. Ticket prefix `FA`, per gitkontor's file-based
workflow (`operations/release.md`).

This diverges from two heimathafen defaults: `standards/legal/default-license-apache-2-cla.md`
(Apache-2.0 plus a CLA) and "no remote until justified" (every repo starts local-only). Both
divergences are the ones `create_brass_compass` already made and every sibling since has recorded;
this project inherits them rather than deciding them fresh. Cost if wrong: MIT and a public remote
are hard to walk back once someone has forked.
