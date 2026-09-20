package firearms.client.scope;

import firearms.Firearms;
import firearms.component.ComponentRegistration;
import firearms.item.ItemRegistration;
import firearms.model.Attachment;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Resolves the optic the three scope mixins care about straight off a held {@code ItemStack} — a
 * {@code firearms:weapon} stack's own {@code firearms:attachment_optic} component — matched
 * against {@link Attachment#ALL}'s 22 hardcoded constants rather than {@code
 * firearms.data.AttachmentRegistry}. That registry only ever holds what {@code
 * firearms.data.AttachmentDataLoader} reloaded from server-side datapacks, which, per {@code
 * firearms.item.WeaponItem}'s own doc, "a remote client never has loaded" — a client mixin has to
 * run correctly on exactly that remote client, not only in singleplayer where the reload listener
 * happens to share the same JVM. {@link Attachment#ALL} ships the identical 22 numbers the mod's
 * own default datapack files mirror (`firearms.data.AttachmentDataLoader`'s own doc), so this
 * lookup is correct for every attachment this mod ships; a third-party datapack's own custom optic
 * (`docs/spec/domains/attach.md` §2 "a datapack may add more per slot with zero new Java") is not
 * resolvable here and simply never zooms — an accepted limitation of a client-only, server-data-free
 * lookup, noted in FA-10's Findings.
 */
public final class ScopedWeapon {

    private ScopedWeapon() {
    }

    /** The attachment in {@code stack}'s optic slot, if {@code stack} is a weapon, the slot is filled, and this mod recognizes the id. */
    public static Optional<Attachment> optic(ItemStack stack) {
        if (!stack.is(ItemRegistration.WEAPON)) {
            return Optional.empty();
        }
        Identifier id = stack.get(ComponentRegistration.ATTACHMENT_OPTIC);
        if (id == null) {
            return Optional.empty();
        }
        return byId(id);
    }

    /** {@code stack}'s optic's own zoom factor, if it magnifies (`ScopeZoom#of`). */
    public static OptionalDouble zoom(ItemStack stack) {
        return ScopeZoom.of(optic(stack).orElse(null));
    }

    /**
     * Whether the widened `Player.isScoping()` gate (`COMBAT-REQ-006`) is satisfied by {@code
     * stack} alone — a weapon with a magnifying optic attached; the caller still has to confirm
     * the player is actually using it.
     */
    public static boolean hasZoomingOptic(ItemStack stack) {
        return zoom(stack).isPresent();
    }

    private static Optional<Attachment> byId(Identifier id) {
        for (Attachment attachment : Attachment.ALL) {
            if (Firearms.id(attachment.id()).equals(id)) {
                return Optional.of(attachment);
            }
        }
        return Optional.empty();
    }
}
