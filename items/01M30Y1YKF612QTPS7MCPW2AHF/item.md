---
schema_version: 1
id: 01M30Y1YKF612QTPS7MCPW2AHF
key: FA-28
type: docs
title: "Modrinth slug create-firearms: firearms is taken"
created_by: kevin
created_at: 2026-09-21T02:52:21Z
---

## Scope

Creating the Modrinth draft on 2026-09-21 failed with "Slug is already taken!" for `firearms` (no public project answers at that slug, so it is a private or reserved one). The listing takes the slug `create-firearms`, free on Modrinth; the name "Create: Firearms" is unchanged.

## Approach

`docs/modrinth/body.md` Slug row; `DEC-002-name.md` in the vault records the slug and why; `docs/spec` synced.

## Acceptance criteria

- [x] Slug `create-firearms` in the body and the spec; the draft created on Modrinth at that slug; merged through a Forgejo pull request into `development`.

## Constraints and prior findings

Sibling slugs stay as they are (`brass-compass`, `metered-motor`, `villager-customers`, `synthetic-diamonds`, `wait-they-talk-now`, `grounded-villages`).
