/**
 * This mod's entire client mixin surface (`docs/spec/04-architecture.md` {@code ARCH-DEC-001}):
 * three small mixins, all gated on one widened check, that let a scoped weapon reuse the vanilla
 * spyglass mechanic exactly as `docs/spec/decisions/DEC-009-scope-mechanic.md` decided —
 * {@link firearms.mixin.client.PlayerScopingMixin} widens the shared gate itself, {@link
 * firearms.mixin.client.FieldOfViewMixin} reads a per-optic FOV factor in place of the spyglass's
 * hardcoded {@code 0.1f}, and {@link firearms.mixin.client.ScopeOverlayMixin} swaps the overlay
 * texture. No server-side mixin exists anywhere in this mod. Registered in {@code
 * firearms.mixins.json}'s {@code client} array.
 */
package firearms.mixin.client;
