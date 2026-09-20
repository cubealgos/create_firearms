---
schema_version: 1
id: 01M2YWF4GV1QJ69QQPK9HYHFJJ
key: FA-16
type: chore
title: Release 1.0.0+26.2
created_by: kevin
created_at: 2026-09-20T07:46:08Z
---

## Scope

The `1.0.0+26.2` release: tag `production` at `v1.0.0+26.2`, `just release` builds `dist/` with the
jar, its SHA-256 checksum, and the release notes (`REL-REQ-001`); the notes list the Minecraft,
Fabric Loader and Create Fly versions tested (`REL-REQ-002`), state the 1.0 roster (six weapons,
their calibres and classes) and that every stat and price is a proposed starting point, not a
balanced final number (`REL-REQ-003`); publish to Modrinth using `FA-15`'s assets. Not the assets
themselves (`FA-15`, already landed).

## Approach

`just release` refuses on a dirty checkout or a HEAD not at the expected tag, exactly as every
sibling's own release recipe does. Publish to Modrinth only, CurseForge deferred like all four
siblings (`operations/release.md`). CHANGELOG.md's `[Unreleased]` section becomes
`[1.0.0+26.2] - <date>` with the full feature list this milestone's tickets shipped.

## Acceptance criteria

- [ ] `just release` from a clean checkout at `v1.0.0+26.2` produces `dist/create_firearms-1.0.0+26.2.jar`,
      its `.sha256`, and `dist/release-notes-1.0.0+26.2.md`.
- [ ] The release notes list tested Minecraft/Fabric Loader/Create Fly versions and the
      proposed-numbers disclosure (`REL-REQ-002`, `003`).
- [ ] The Modrinth listing is published using `FA-15`'s icon, body and gallery.
- [ ] `CHANGELOG.md` carries a `[1.0.0+26.2]` section.
- [ ] `just check` green.

## Constraints and prior findings

`docs/spec/operations/release.md` `REL-REQ-001`–`004`. Blocked by `FA-15` (the assets this release
publishes with).
