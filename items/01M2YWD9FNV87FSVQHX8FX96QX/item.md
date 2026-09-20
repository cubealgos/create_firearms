---
schema_version: 1
id: 01M2YWD9FNV87FSVQHX8FX96QX
key: FA-1
type: chore
title: "Bootstrap: Gradle with Loom and Create Fly, entrypoints, licence, notice, routing, tools, spec copy, smoke game test"
created_by: kevin
created_at: 2026-09-20T07:45:07Z
---

## Scope

The repository as `docs/spec/04-architecture.md` `ARCH-DEC-001` and `docs/spec/decisions/DEC-004-toolchain.md`
describe it: one Gradle project on Loom 1.17.21 with Create Fly `26.2-rc-2-6.0.9-1` pinned and
Fabric API `0.160.0+26.2`, Java 25, Gradle 9.5.1 wrapper, Kotlin DSL and version catalog copied
from `create_synthetic_diamonds`'s verified `SD-1` scaffold: `build.gradle.kts`,
`settings.gradle.kts`, `gradle/`, `gradlew`, `gradle.properties`, `justfile`, `tools/` (`doctor.py`,
`map.py`, `release_notes.py` and their tests; no `icon.py` yet, `FA-15`'s territory),
`.ci/install-tools.sh`, `.woodpecker.yml`, `.gitea/default_merge_message/*`, `CLAUDE.md` routing
adapted to this repo's six domains (weapon, attach, ammo, combat, trade, ui) and stating the
compliance rule, `.gitignore`. MIT `LICENSE` (`decisions/DEC-003-licence.md`), `NOTICE` crediting
Create Fly (CC0), Create (MIT) and Fabric (Apache-2.0), `README.md`, `SUPPORT.md`, `CHANGELOG.md`
skeleton, `docs/spec/` as a byte-identical copy of the vault spec via `just spec-sync`, and
`docs/modrinth/body.md` as a placeholder (project settings table filled, the body itself left for
`FA-15`). Main and client entrypoints for mod id `firearms` (package `firearms`, classes `Firearms`
and `client.FirearmsClient`); a `firearms.mixins.json` with `compatibilityLevel: JAVA_25` (unlike
`create_synthetic_diamonds`, this mod will gain three client-only scope mixins at `FA-10` —
`DEC-004-toolchain.md` — so the config is empty but not permanently so, and matches
`create_metered_motor`'s own `JAVA_25` compatibility level rather than a sibling that stays
mixin-free forever); `verifyPurePackage` gating the `firearms.model` package (the pure stat
derivation and attach functions land there at `FA-2`); a smoke game test proving the mod loads
beside Create Fly (`SmokeGameTest`); `SourceSurfaceTest` asserting no networking type is referenced
(`COMP-REQ-001`) and, since this mod carries a real branding-compliance requirement none of the
four siblings have, a third assertion that no source or resource file names PUBG or its branding
(`COMP-REQ-002`). `fabric.mod.json`'s `contact` block: source
`https://git.cubealgos.de/cubealgos/create_firearms`, issues
`https://github.com/cubealgos/create_firearms/issues`, Modrinth slug `firearms`, display name
"Create: Firearms" (`decisions/DEC-002-name.md`).

## Approach

Copy the verified toolchain and repository shape from `create_synthetic_diamonds`'s `SD-1`
bootstrap commit (its exact file list, not its current HEAD, since later `synthetic_diamonds`
tickets added recipe/roll-specific content not applicable here) into one Gradle project for
`create_firearms`; rename every `synthetic_diamonds`/`SyntheticDiamonds`/`Synthetic
Diamonds`/`SD` identifier to `firearms`/`Firearms`/`Firearms`/`FA`, then rewrite the prose in
`README.md`, `CLAUDE.md`, `NOTICE`, `SUPPORT.md`, `fabric.mod.json`, the `model` package-info and
the entrypoint's log line for this mod's own subject matter. Point `just spec-sync`'s vault path at
`../heimathafen/vault/projects/create_firearms/spec`. Diverge from `SD-1` in three ways this
ticket's own brief calls for: the mixin config carries `JAVA_25` rather than staying permanently
empty at `JAVA_21`, since this mod's `04-architecture.md` `ARCH-DEC-001` calls for three later
client-side mixins; `SourceSurfaceTest` gains a third, `COMP-REQ-002` assertion this mod's own
compliance domain requires and none of the siblings needed; and `CLAUDE.md` states the naming/
branding compliance rule explicitly, since it is a standing risk unique to this mod.

## Acceptance criteria

- [x] `just check` is green: lint (`verifyPurePackage`), `map-check` (5 files current), `test-java`
      (3 tests, including the `COMP-REQ-002` branding scan), `test-tools` (4 tests), `gametest`
      (2/2 game tests, including `SmokeGameTest` asserting both `create` and `firearms` are
      loaded).
- [x] `just doctor-toolchain` is clean: Java 25, Gradle wrapper 9.5.1, `just` present, Python 3,
      `kontor` present, map current, `docs/spec/` identical to the vault.
- [x] `fabric.mod.json`'s `contact` block links the Forgejo repo, the GitHub issues tracker, and
      the Modrinth slug `firearms`; `name` is "Create: Firearms".
- [x] `firearms.mixins.json` exists with an empty `mixins`/`client` array and `compatibilityLevel:
      JAVA_25`.
- [x] `docs/modrinth/body.md` exists with the project-settings table filled and the body itself
      marked as `FA-15`'s territory.
- [x] Forgejo repo `cubealgos/create_firearms` created (public), `development` pushed and set as
      default branch, branch protection on `development`/`production`, merge message templates
      present, `gitkontor/data` pushed.

## Constraints and prior findings

`docs/spec/contracts/platform-matrix.md`, `docs/spec/decisions/DEC-004-toolchain.md`,
`docs/spec/decisions/DEC-003-licence.md`, `docs/spec/operations/compliance.md`. Create Fly
coordinate, mod id `create`, declared version `6.0.9-1`, and the 26.2 toolchain floors are
inherited unverified-again from the four siblings' own bootstraps
(`create_metered_motor`'s `MM-1`, `create_brass_compass`'s `BC-1`, `create_villager_customers`'s
`VC-1`, `create_synthetic_diamonds`'s `SD-1`), which is the direct model for this ticket's shape.
`create_synthetic_diamonds`'s own `SD-1` Findings name three slips this ticket must avoid: `kontor
init` parks the checkout on `chore/bootstrap`, not `development`
(`vault/technical/gitkontor/kontor-init-parks-the-checkout-on-chore-bootstrap.md`); `kontor ticket
new` lands a ticket in `backlog` regardless of its milestone's own status, so a ticket meant to
start immediately needs an explicit `kontor ticket status KEY todo` before `kontor claim`
(`vault/technical/gitkontor/ticket-new-lands-in-backlog-claim-needs-todo.md`); and the copied
`.ci/install-tools.sh` lost its executable bit at `SD-1` itself, fixed only later at `SD-9` —
`git update-index --chmod=+x` (or a `chmod +x` before the first `git add`) must land before the
first commit this time, not after a broken CI run.

## Findings

**Repo.** `https://git.cubealgos.de/cubealgos/create_firearms`, id `19`, public. GitHub mirror
`https://github.com/cubealgos/create_firearms` created via `gh repo create`, issues enabled,
projects disabled, wiki disabled.

**Local identity set before `kontor init`**: `git config user.name`/`user.email` set to
`Kevin Scheeren <scheeren@cubealgos.de>` in the fresh checkout *before* running `kontor init`,
avoiding `SD-1`'s own finding that the tool's two root commits otherwise inherit the environment's
global identity (`kevinscheeren@icloud.com`). Both `chore/bootstrap`'s root commit and
`gitkontor/data`'s root commit carried the correct author from the start; no `--reset-author` pass
was needed this time.

**`git checkout development` run immediately after `kontor init`**, before any `pull` or `branch
new`, per the vault's own rule — avoided the stale-branch trap entirely.

**Executable bit preserved**: `.ci/install-tools.sh` was `chmod +x`'d immediately after copying,
before `git add`; `git ls-files -s` confirmed mode `100755` in the index prior to the first commit,
unlike `SD-1`'s own bootstrap which shipped it at `100644` and needed a follow-up ticket (`SD-9`)
to fix.

**A genuine bug found in the `SD-1` scaffold, not reproduced here**: `create_synthetic_diamonds`'s
`tools/doctor.py` reads the spec-copy override from an environment variable named `VC_VAULT_SPEC`
— a leftover from copying `create_villager_customers`'s own `VC-1` scaffold, never renamed to
`SD_VAULT_SPEC` the way `justfile`'s own `vault_spec` variable was. Every other sibling
(`create_villager_customers`, `create_metered_motor`, `create_brass_compass`) renamed this
consistently; only `create_synthetic_diamonds` carries the mismatch. `tools/doctor.py` here uses
`FA_VAULT_SPEC` throughout, matching `justfile`. Worth a `create_synthetic_diamonds` ticket on that
sibling's own tracker; not fixed here since it is out of this ticket's repository.

**`kontor ticket new` numbering is sequential per prefix and does not take an explicit key**: the
first ticket created in this repo landed as `FA-1` regardless of title, so the milestone/ticket
creation order matters — the stat-model ticket had to be created, deleted (nothing was yet
committed to `gitkontor/data`, so a plain `rm -rf` of the item directory was safe), and recreated
after this bootstrap ticket so `FA-1` names the bootstrap itself, matching every sibling's own
numbering.

**`kontor milestone new` auto-creates an `M0` "Foundation" milestone at `kontor init` time**,
seeded with the same Goal/Scope/Exit-criteria prose every sibling's own `M0` carries, at `backlog`
status — `kontor milestone status M0 todo` moved it to `todo` once `FA-1` itself was ready to
claim, mirroring `kontor ticket status`'s own backlog→todo requirement one level up.

`just check`: lint (`verifyPurePackage`) ok, map-check ok (5 files current), test-java ok (3
tests: networking scan, translation-key scan, PUBG/branding scan), test-tools ok (4 tests),
gametest ok (2/2 game tests, including `SmokeGameTest` asserting both `create` and `firearms` are
loaded).
`just doctor-toolchain`: fully ok (java 25, gradle wrapper 9.5.1, just 1.58.0, python3 3.14.7,
kontor present, map current, spec copy identical).
`kontor doctor`: 10 checks all passed except the merge-template check, skipped as expected (no
remote default branch known yet before the first push).

**Repo, second pass, after the first push.** `https://git.cubealgos.de/cubealgos/create_firearms`,
id `19`, public, default branch `development` (auto-set by Forgejo on the first push, no separate
`PATCH` needed for that field — confirmed again here). `PATCH` turned wiki/projects/packages/actions
off, issues stayed on. Branch protection (`enable_push: false`) added on both `development` and
`production`. GitHub mirror `https://github.com/cubealgos/create_firearms` created via `gh repo
create --public --disable-wiki`, then `gh repo edit --enable-issues --enable-projects=false`; the
push-mirror wiring itself and Woodpecker's enablement are Kevin's own, per this ticket's brief, not
set up here.

**PR #1 merge, a second data point on the vault's own "a merge the forge records but never lands"
finding.** The bootstrap PR merged with `merged: true` and a `merge_commit_sha`, but the merge
call itself first answered `405 {"message":"Please try again later"}` — polled `GET .../pulls/1`
every two seconds; by the third poll `state` had flipped to `closed` and `merged: true` with the
same commit sha the first (rejected) call would have produced, and `git ls-remote
.../development` confirmed the same sha as the branch tip. Unlike the vault note's own worked
example, no second PR was needed here — the 405 this time really was transient re-check latency,
not the "recorded but never lands" failure mode; verified with `git ls-remote`, not the API alone,
per the note's own rule.

**`kontor doctor`'s merge-template check, closing the loop**: skipped before the first push (no
`origin/HEAD`), then `git remote set-head origin -a` after the merge made it check for real —
`all 3 present on origin/development (the default)`, all 10 checks passing.

**gitkontor project id** `01M2YVXBNZJ54X3593TPSYJ0DJ`; milestone ids: M0 `01M2YVXBP0XP4SE92H939B8G3T`,
M1 `01M2YW8TR56XZK7DF5KHHHABPJ`, M2 `01M2YW8TSYQ18TTW0MBH4S1AH1`, M3 `01M2YW8TVQXXXFV9YV2TQSB58X`,
M4 `01M2YW8TXHNBNF2S5T8P0PVMW3`, M5 `01M2YW8TZB5YDG9EXPZYMVTBD9`, M6 `01M2YW8V18FXWSWBPMK20S6HX5`,
M7 `01M2YW8V313F63ES9EJN2T2YWT`.

**Local `chore/fa-1-bootstrap` needed no recreation at `kontor finish`**, unlike `SD-1`'s own
experience: the worktree branch was still present locally (not yet deleted) when `kontor finish
FA-1` ran, so containment verified against it directly; the worktree and branch were removed only
after `finish` succeeded.

**A repo-shape note for whoever bootstraps the sixth sibling**: the bootstrap files were authored
directly in the main checkout (parked on `development` per the vault's own rule), then `rsync`'d
into the `kontor branch new`-created worktree before the first commit, since `kontor branch new`
always starts a worktree from a clean base branch rather than picking up the main checkout's own
uncommitted tree. Worth doing the reverse next time — author directly inside the worktree once it
exists — to skip the extra copy step.
