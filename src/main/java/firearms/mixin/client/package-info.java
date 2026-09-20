/**
 * This mod's client mixin surface (`docs/spec/04-architecture.md` {@code ARCH-DEC-001}): three
 * small mixins, all gated on one widened check, that let a scoped weapon reuse the vanilla
 * spyglass mechanic exactly as `docs/spec/decisions/DEC-009-scope-mechanic.md` decided —
 * {@link firearms.mixin.client.PlayerScopingMixin} widens the shared gate itself, {@link
 * firearms.mixin.client.FieldOfViewMixin} reads a per-optic FOV factor in place of the spyglass's
 * hardcoded {@code 0.1f}, and {@link firearms.mixin.client.ScopeOverlayMixin} swaps the overlay
 * texture — plus one unrelated fourth, {@link firearms.mixin.client.AttackControlMixin}
 * (`FA-24`, `docs/spec/decisions/DEC-019-controls.md`), cancelling vanilla's own attack-swing
 * handling while a firearm is held, now that the attack control fires rather than swings. A
 * separate server-side mixin also exists ({@code firearms.mixin.AnvilMenuMixin}), registered in
 * {@code firearms.mixins.json}'s own {@code server} array rather than this package's {@code
 * client} one. Registered in {@code firearms.mixins.json}'s {@code client} array.
 */
package firearms.mixin.client;
