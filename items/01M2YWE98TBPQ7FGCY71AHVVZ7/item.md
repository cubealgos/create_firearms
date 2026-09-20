---
schema_version: 1
id: 01M2YWE98TBPQ7FGCY71AHVVZ7
key: FA-10
type: feat
title: "The scope mixins: isScoping widening, per-optic zoom, overlay"
created_by: kevin
created_at: 2026-09-20T07:45:40Z
---

## Scope

The three client-only mixins this mod's `04-architecture.md` `ARCH-DEC-001` names as its entire
mixin surface: widen `Player.isScoping()` to also return true while a player uses a firearm whose
attached optic's zoom factor is greater than 1.0 (`COMBAT-REQ-006`); read that optic's own zoom
factor in `AbstractClientPlayer.getFieldOfViewModifier`, in place of the spyglass's hardcoded
`0.1f` (`COMBAT-REQ-007`); swap the HUD's spyglass-overlay draw to the optic's own texture, in
place of `SPYGLASS_SCOPE_LOCATION` (`COMBAT-REQ-008`). Red dot and holo never satisfy the widened
gate — no FOV change, no overlay, no held-item suppression for them (`COMBAT-DEC-004`). Populates
`firearms.mixins.json`'s `client` array for the first time. Not the item model layers (`FA-9`,
already landed) and not the spread-while-aiming numeric effect, which is `WEAPON-REQ-005`'s own
territory in the stat function (`FA-2`, already landed).

## Approach

All three mixins gate on the same widened boolean check, so this is "one mixin family," not three
independent decisions (`decisions/DEC-009-scope-mechanic.md`). `Player.isScoping()` is an exact
`Items.SPYGLASS` identity check today, never keyed on use-animation (research
`smithing-and-item-model-layers-26-2.md` §C.1) — the widening mixin adds the firearm+zoom-optic
condition alongside the existing spyglass check, not in place of it. `compatibilityLevel: JAVA_25`
is already set in `firearms.mixins.json` from `FA-1`'s bootstrap.

## Acceptance criteria

- [x] A firearm with a magnified optic (2x–15x) attached zooms the FOV by that optic's own factor
      while the aim control is held, and shows that optic's own overlay texture.
- [x] A firearm with red dot or holo attached shows no FOV change, no overlay, and does not suppress
      the held-item render while aiming (`COMBAT-DEC-004`).
- [x] Releasing the aim control returns the view, overlay and held-item render to normal exactly as
      releasing a vanilla spyglass does.
- [x] `just check` green, including a `just client` manual confirmation on the checklist.

## Constraints and prior findings

`docs/spec/04-architecture.md` `ARCH-DEC-001`, `docs/spec/decisions/DEC-009-scope-mechanic.md`,
`docs/spec/domains/combat.md` `COMBAT-REQ-006`–`008`, `COMBAT-DEC-004`, `COMBAT-FAIL-003` (a second
mod also widening `isScoping()` coexists only if both widen additively — a mixin-ordering problem,
not this mod's to solve alone), `docs/spec/contracts/platform-matrix.md` `PLATFORM-REQ-003` (build
fails at compile time if a future Minecraft release moves `isScoping()`'s call sites). Blocked by
`FA-7` (needs a real optic-bearing weapon to test against).

## Findings

`javap -p -c -constants` against `minecraft-merged-deobf-26.2.jar` confirmed all three targets
exactly as the research note predicted, with no signature surprises:

- **`Player.isScoping()`** (`net/minecraft/world/entity/player/Player`): `isUsingItem() &&
  getUseItem().is(Items.SPYGLASS)` — bytecode is the literal `invokevirtual isUsingItem`, `ifeq`,
  `invokevirtual getUseItem`, `getstatic Items.SPYGLASS`, `invokevirtual ItemStack.is(Object)`,
  `ifeq`/`iconst_1`/`iconst_0`/`ireturn`. `firearms.mixin.client.PlayerScopingMixin` injects
  `@Inject(method = "isScoping", at = @At("RETURN"), cancellable = true)`, flipping only a `false`
  return to `true` when `isUsingItem()` and `ScopedWeapon.hasZoomingOptic(getUseItem())` — never the
  reverse, so it stays additive alongside a second mod's own widening (`COMBAT-FAIL-003`).
- **`AbstractClientPlayer.getFieldOfViewModifier(boolean, float)`**: the `0.1f` sits behind
  `iload_1 (isFirstPerson); ifeq 119; invokevirtual isScoping; ifeq 119; ldc #167 (float 0.1f);
  freturn` — a second, independent literal from `SpyglassItem.ZOOM_FOV_MODIFIER`, and the only
  `0.1f` in the method (no ordinal ambiguity). `firearms.mixin.client.FieldOfViewMixin` uses
  `@ModifyConstant(constant = @Constant(floatValue = 0.1f))`, returning `1 / zoom` when the scoping
  item resolves to a firearm with a magnifying optic, else the original constant unchanged.
- **`Hud.extractSpyglassOverlay(GuiGraphicsExtractor, float)`** (renamed from `Gui` in 26.2 — `Gui`
  itself carries no spyglass code at all, confirmed by a `-i spyglass` grep over its own
  disassembly): reads `private static final Identifier SPYGLASS_SCOPE_LOCATION` via one
  `getstatic` immediately consumed by the `blit(...)` call — no separate accessor, no second read.
  `firearms.mixin.client.ScopeOverlayMixin` uses `@Redirect` on that one `FIELD` `GETSTATIC`,
  returning the resolved optic's texture (`firearms.client.scope.ScopeOverlay`) when the local
  player's used item is a firearm with a magnifying optic, else the vanilla field's own value
  (shadowed, not re-typed as a literal). The call site itself, `extractCameraOverlays`, already
  gates the whole call on `LocalPlayer.isScoping()` — confirmed by the same disassembly pass — so
  red dot/holo weapons never reach this redirect at all (`COMBAT-DEC-004`), and no fourth mixin was
  needed there.
- **`ItemInHandRenderer.submitArmWithItem(...)`**: first two instructions are `aload_1;
  invokevirtual isScoping; ifeq 8; return` — confirmed the held-item/arm suppression needs no mixin
  of its own; it already reads the same widened gate `PlayerScopingMixin` patches.

**`WeaponItem` needed one change**, exactly the one the ticket anticipated: `getUseAnimation`
returned `ItemUseAnimation.NONE` unconditionally (FA-6 never set it). It now returns `SPYGLASS`
while `ScopedWeapon.hasZoomingOptic(stack)`, else `NONE` — arm pose only; none of the zoom/overlay/
suppression trio depends on it, since all three gate on `isScoping()`'s item-identity check, never
the use-animation. No other line in `WeaponItem` was touched.

**Design note**: `firearms.client.scope.ScopedWeapon` deliberately does not resolve the optic
through `firearms.data.AttachmentRegistry` (the path `firearms.fire.WeaponLoadouts` uses
server-side) — that registry is a `PackType.SERVER_DATA` reload listener a remote client never has
loaded (`firearms.item.WeaponItem`'s own doc). Instead it matches the `firearms:attachment_optic`
component's id directly against `firearms.model.Attachment#ALL`'s 22 hardcoded constants, which
`firearms.data.AttachmentDataLoader`'s own doc confirms mirror the shipped datapack files exactly.
Accepted limitation: a third-party datapack's own custom optic (`docs/spec/domains/attach.md` §2,
"a datapack may add more per slot with zero new Java") is not resolvable by this lookup and simply
never zooms — a client-only, server-data-free design cannot do otherwise without a new sync packet,
out of this ticket's scope.

**Overlay art**: one shared reticle (`assets/firearms/textures/gui/scope_reticle.png`, drawn by
`tools/scope_overlays.py` with Pillow — a black vignette, a soft circular cutout, a tube-shadow
ring, and a mil-dot-style crosshair, our own art) rather than one texture per optic tier — `docs/
spec/domains/attach.md` §3 gives each optic a zoom factor but no per-tier art of its own, and
`ScopeOverlay.texture(Attachment)` still takes the optic parameter so a future per-tier texture is
an additive change inside that one method, not a new mixin.

**Verification**: `just check` green — `lint` (incl. `verifyPurePackage`), `map-check`, `test-java`
(all suites, incl. 5 new `ScopeZoomTest` cases), `test-tools` (4), `gametest` (32/32, unrelated to
this ticket — no server-visible piece exists here, confirmed below). Additionally ran a real `just
client` launch (`./gradlew runClient`) to LWJGL window + title screen with no Mixin apply error and
no exception outside two pre-existing, unrelated warnings (Create's own missing dev config file,
also present in the gametest run; a Realms auth failure expected offline in a dev environment) —
the mixin bytecode transforms load and apply cleanly against the real merged jar. This is not a
substitute for playing a scoped shot in-world.

**No game test**: nothing here is server-visible. `Player.isScoping()`, the FOV modifier and the
HUD overlay are all client-rendering-only; the widened gate, the zoom math and the texture swap
carry no authority over any hit, damage, ammo, or synced state (`ARCH-DEC-001`'s own "client only"
framing). `docs/spec/operations/testing.md` already says as much for this exact ticket ("whether the
three scope mixins actually produce a visible zoom/overlay/suppression against a real client
renderer... need[s] a running client... to confirm").

**Client checklist additions** (for the release checklist / a human `just client` pass, `docs/spec/
operations/testing.md` "Client / Kevin's checklist" row): attach a 2x–15x optic to a weapon and
confirm the FOV actually narrows by that optic's own factor while aiming, and that the reticle
overlay appears; attach a red dot or holo and confirm no FOV change, no overlay, and the held item
still renders while aiming; release the aim control and confirm view/overlay/held-item return to
normal exactly like releasing a vanilla spyglass; fire while scoped through a zooming optic and
confirm the arm/use animation matches the spyglass pose (`WeaponItem.getUseAnimation`); check a
second mod that also widens `isScoping()` (if any is installed) still gets a spyglass zoom
(`COMBAT-FAIL-003`, not directly testable without such a mod present).
