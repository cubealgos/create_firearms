package firearms.client.scope;

import firearms.Firearms;
import firearms.model.Attachment;
import net.minecraft.resources.Identifier;

/**
 * The overlay texture `firearms.mixin.client.ScopeOverlayMixin` draws in place of vanilla's own
 * {@code SPYGLASS_SCOPE_LOCATION} (`COMBAT-REQ-008`). One shared reticle for every magnifying
 * optic, drawn by {@code tools/scope_overlays.py} to {@code
 * assets/firearms/textures/gui/scope_reticle.png} — `docs/spec/domains/attach.md` §3 gives each
 * optic its own zoom factor but no per-tier art of its own, and a single circular-vignette reticle
 * is the smallest correct surface (`docs/spec/04-architecture.md` "build nothing beyond the current
 * roadmap stage"). Takes the optic parameter for symmetry with {@link ScopedWeapon} and so a future
 * per-tier texture is an additive change inside this one method, not a new mixin.
 */
public final class ScopeOverlay {

    private static final Identifier RETICLE = Firearms.id("textures/gui/scope_reticle.png");

    private ScopeOverlay() {
    }

    /** The overlay texture to draw while scoped through {@code optic}. */
    public static Identifier texture(Attachment optic) {
        return RETICLE;
    }
}
