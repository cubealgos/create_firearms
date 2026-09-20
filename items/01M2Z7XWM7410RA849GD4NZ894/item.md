---
schema_version: 1
id: 01M2Z7XWM7410RA849GD4NZ894
key: FA-23
type: bug
title: "The weapon item model fails to parse: the aiming transformation lacks the rotation and scale keys, every weapon renders as the missing model"
created_by: kevin
created_at: 2026-09-20T11:06:25Z
---

## Scope

<fill this in before committing>

## Approach

<fill this in before committing>

## Acceptance criteria

- [x] <fill this in before committing>

## Constraints and prior findings

<fill this in before committing>

## Scope

Kevin, 2026-09-20, in the client on development 1eec891: every weapon renders as the magenta missing model in the hotbar and in hand. The client log: `Couldn't parse item model 'firearms:weapon' from pack 'firearms': ... No key right_rotation ... No key scale ... No key left_rotation in MapLike[{"translation":[0.0,0.05,-0.1]}]`. The `transformation` block on the aiming `minecraft:model` layer (FA-9's, carried by FA-22's generator) is a `Transformation` codec, which in 26.2 requires `translation`, `left_rotation`, `scale` and `right_rotation` (or a 16-float matrix); ours carried only `translation`, so the whole `firearms:weapon` definition failed and fell back to the missing model. Present since FA-9; unseen because no client check happened between FA-9 and FA-22.

## Acceptance criteria

- [x] `tools/models.py` writes the full transformation (`translation`, `left_rotation` `[0,0,0,1]`, `scale` `[1,1,1]`, `right_rotation` `[0,0,0,1]`) and `items/weapon.json` is regenerated.
- [x] `ModelAssetsTest` walks every `transformation` block in `items/*.json` and asserts the four keys are present with the right arities, so the shape can never regress silently.
- [ ] `just check` green; merged through a Forgejo pull request into `development`; Kevin sees the models in the client.
