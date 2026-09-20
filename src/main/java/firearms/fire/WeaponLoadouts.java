package firearms.fire;

import firearms.component.Base;
import firearms.component.ComponentRegistration;
import firearms.data.AttachmentRegistry;
import firearms.data.WeaponRegistry;
import firearms.model.Attachment;
import firearms.model.Loadout;
import firearms.model.Slot;
import firearms.model.WeaponBase;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Resolves the {@link Loadout} a fire attempt needs from a weapon stack's own live components
 * (`docs/spec/domains/weapon.md` {@code WEAPON-REQ-003}): {@code firearms:base} names the base
 * weapon, and each present {@code firearms:attachment_<slot>} names one attachment. Nothing here is
 * baked onto the stack or cached — every call re-resolves against {@link WeaponRegistry} and {@link
 * AttachmentRegistry} at the moment it is needed, the same "nothing is baked" rule {@code
 * firearms.model.StatDerivation} itself follows. An attachment id a currently-loaded data set no
 * longer defines (a datapack removal) is treated as absent, matching {@code WEAPON-FAIL-004}: a
 * missing base weapon fails the whole resolution ({@code WEAPON-FAIL-001} is not reachable in
 * practice, since nothing exposes firing on a non-weapon item, but this stays defensive), while a
 * missing attachment simply leaves that one slot empty rather than failing the whole stack.
 */
public final class WeaponLoadouts {
    private WeaponLoadouts() {
    }

    /** The loadout {@code stack} currently carries, or empty if its {@code firearms:base} is missing or unresolved. */
    public static Optional<Loadout> of(ItemStack stack) {
        Base base = stack.get(ComponentRegistration.BASE);
        if (base == null) {
            return Optional.empty();
        }
        Optional<WeaponBase> weaponBase = WeaponRegistry.get(base.weaponId());
        if (weaponBase.isEmpty()) {
            return Optional.empty();
        }

        Loadout loadout = Loadout.bare(weaponBase.get());
        for (Slot slot : Slot.values()) {
            if (!weaponBase.get().weaponClass().hasSlot(slot)) {
                continue;
            }
            Identifier attachmentId = stack.get(ComponentRegistration.attachmentComponent(slot));
            if (attachmentId == null) {
                continue;
            }
            Optional<Attachment> attachment = AttachmentRegistry.get(attachmentId);
            if (attachment.isPresent()) {
                loadout = loadout.with(attachment.get());
            }
        }
        return Optional.of(loadout);
    }
}
