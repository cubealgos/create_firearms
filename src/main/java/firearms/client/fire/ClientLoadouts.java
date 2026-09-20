package firearms.client.fire;

import firearms.Firearms;
import firearms.client.scope.ScopedWeapon;
import firearms.component.Base;
import firearms.component.ComponentRegistration;
import firearms.item.ItemRegistration;
import firearms.model.Attachment;
import firearms.model.Loadout;
import firearms.model.Slot;
import firearms.model.StatDerivation;
import firearms.model.Stats;
import firearms.model.WeaponBase;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Resolves a held weapon stack's real, attachment-adjusted {@link Stats} client-side, the same
 * technique {@code firearms.client.scope.ScopedWeapon} already uses and for the identical reason
 * that class's own Javadoc explains: {@code firearms.fire.WeaponLoadouts} — the server-side
 * resolver {@code firearms.fire.FiringLogic} itself calls — matches a stack's components against
 * {@code firearms.data.WeaponRegistry}/{@code AttachmentRegistry}, both {@code
 * PackType.SERVER_DATA} reload listeners a remote client never has loaded; a client-only caller
 * (`firearms.client.fire.FireInputHandler`, deciding an {@code auto} weapon's own fire rate every
 * tick) needs a resolution path that works correctly on exactly that remote client, not only in
 * singleplayer where the reload listener happens to share the same JVM.
 *
 * <p>Matches {@code firearms:base}'s {@code weaponId} against {@link WeaponBase#ALL} and each
 * present {@code firearms:attachment_<slot>} against {@link Attachment#ALL} — the identical
 * hardcoded-constant-list technique {@link ScopedWeapon#optic} already uses for the optic slot
 * alone, generalized here to every slot so the full {@link Loadout} (and therefore the real,
 * attachment-adjusted {@link Stats}, not just the bare base stats) can be derived via {@link
 * StatDerivation#stats}. Carries the identical accepted limitation {@code ScopedWeapon}'s own doc
 * names: a third-party datapack's own new weapon or attachment (`docs/spec/domains/weapon.md`
 * `WEAPON-DEC-003`, `docs/spec/domains/attach.md` §2) never resolves here and simply falls back to
 * whatever default the caller supplies — this mod's own six weapons and 22 attachments, the only
 * ones {@code WeaponBase.ALL}/{@code Attachment.ALL} know about, always resolve correctly.
 */
public final class ClientLoadouts {

    private ClientLoadouts() {
    }

    /** {@code stack}'s real, attachment-adjusted stats, or empty if it is not a weapon this mod recognizes (`WeaponBase.ALL` has no match). */
    public static Optional<Stats> stats(ItemStack stack) {
        return loadout(stack).map(loadout -> StatDerivation.stats(loadout, message -> { }));
    }

    private static Optional<Loadout> loadout(ItemStack stack) {
        if (!stack.is(ItemRegistration.WEAPON)) {
            return Optional.empty();
        }
        Base base = stack.get(ComponentRegistration.BASE);
        if (base == null) {
            return Optional.empty();
        }
        Optional<WeaponBase> weaponBase = byId(base.weaponId());
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
            Optional<Attachment> attachment = attachmentById(attachmentId);
            if (attachment.isPresent()) {
                loadout = loadout.with(attachment.get());
            }
        }
        return Optional.of(loadout);
    }

    private static Optional<WeaponBase> byId(Identifier id) {
        for (WeaponBase base : WeaponBase.ALL) {
            if (Firearms.id(base.id()).equals(id)) {
                return Optional.of(base);
            }
        }
        return Optional.empty();
    }

    private static Optional<Attachment> attachmentById(Identifier id) {
        for (Attachment attachment : Attachment.ALL) {
            if (Firearms.id(attachment.id()).equals(id)) {
                return Optional.of(attachment);
            }
        }
        return Optional.empty();
    }
}
