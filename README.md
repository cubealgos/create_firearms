# Create: Firearms

A roster of modern firearms, each under its own real-world designation, built from a base weapon
plus attachments permanently attached at the vanilla smithing table by a player or by a Create Fly
deployer on a contraption — no new block anywhere, and no detaching once an attachment is on.
Combat runs under vanilla damage rules, exactly as an arrow's does; final stats are derived at
runtime from whichever attachments are present, never baked onto the item; a weapon loses ordinary
vanilla durability per shot and is neither repairable at an anvil nor enchantable. Weapons and
attachments sell through the existing weaponsmith, ammunition through the existing fletcher, with a
master buy-back trade wanting a fully-configured weapon for many emeralds.

1.0 ships six weapons, one per class, six calibres, none repeated — M1911, Micro Uzi, AKM, Ruger
Mini-14, AWM, Winchester Model 1897 — alongside the full 22-attachment set. Every base weapon,
attachment and cartridge also has a plain crafting-table recipe.

Requires Minecraft 26.2, Fabric Loader, Fabric API and Create Fly. MIT (LICENSE); credits in
NOTICE.  Releases carry the jar and its SHA-256 in the notes; see CHANGELOG.md for what each
version holds.

Support and security reports go through the issue tracker only (SUPPORT.md):
https://github.com/cubealgos/create_firearms/issues.

Source: https://git.cubealgos.de/cubealgos/create_firearms (Forgejo, the home of this repository).
Mirror: https://github.com/cubealgos/create_firearms, read-only code, and the issue tracker.
Releases: https://modrinth.com/mod/firearms.

Development: `just --list`. The specification is `docs/spec/`.
