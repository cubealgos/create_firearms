package firearms.client.scope;

import firearms.model.Attachment;
import firearms.model.Slot;
import java.util.OptionalDouble;

/**
 * The pure "does this optic zoom" mapping (`docs/spec/domains/combat.md` {@code COMBAT-REQ-006},
 * {@code COMBAT-DEC-004}; `docs/spec/domains/attach.md` §3): an optic magnifies only when its own
 * {@link Attachment#zoom()} is strictly greater than {@code 1.0}. Red dot and holo both carry
 * {@code zoom() == 1.0} ("none") and so never satisfy the widened {@code Player.isScoping()} gate
 * the three client mixins share (`firearms.mixin.client.PlayerScopingMixin`, {@code
 * FieldOfViewMixin}, {@code ScopeOverlayMixin}); 2x through 15x each report their own factor,
 * unchanged. No Minecraft import — testable as a plain unit, independent of the mixins that call
 * it through {@link ScopedWeapon}.
 */
public final class ScopeZoom {

    private ScopeZoom() {
    }

    /**
     * {@code optic}'s own zoom factor, if it magnifies at all.
     *
     * @param optic the attachment present in a weapon's optic slot, or {@code null} for an empty
     *     slot
     * @return the optic's zoom factor when it is a magnifying optic (2x–15x); empty for {@code
     *     null}, a non-optic attachment, or a non-magnifying optic (red dot, holo)
     */
    public static OptionalDouble of(Attachment optic) {
        if (optic == null || optic.slot() != Slot.OPTIC || optic.zoom() <= 1.0) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(optic.zoom());
    }
}
