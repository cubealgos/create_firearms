package firearms.item;

import firearms.Firearms;
import firearms.attach.Attach;
import firearms.component.Ammo;
import firearms.component.Base;
import firearms.component.ComponentRegistration;
import firearms.model.Attachment;
import firearms.model.Loadout;
import firearms.model.Stats;
import firearms.model.StatDerivation;
import firearms.model.WeaponBase;
import firearms.support.Ids;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Builds a {@code firearms:weapon} stack the exact way {@code /firearms debug give} does — a bare
 * stack, attached through the real {@link Attach} function slot by slot, loaded to capacity — so
 * every caller that needs one (the debug command itself, `firearms.item.CreativeTabRegistration`'s
 * creative tab, and any future one) shares this one construction path rather than each
 * reimplementing it (`docs/spec/domains/ui.md` `UI-REQ-007`).
 */
public final class WeaponStacks {

    /**
     * A bare stack of {@code weaponBase}, named by {@code weaponId} (its full id, since a
     * datapack-supplied base does not necessarily live under this mod's own namespace): the {@code
     * firearms:base} component, and {@code max_damage}/{@code damage} set from the base's own
     * durability, matching {@code ItemRegistration}'s own reasoning for why neither is a fixed
     * {@link Item.Properties} value.
     */
    public static ItemStack bare(Identifier weaponId, WeaponBase weaponBase) {
        ItemStack stack = new ItemStack(ItemRegistration.WEAPON);
        stack.set(ComponentRegistration.BASE, Base.of(weaponId));
        stack.set(DataComponents.MAX_DAMAGE, weaponBase.baseStats().durability());
        stack.set(DataComponents.DAMAGE, 0);
        return stack;
    }

    /**
     * A copy of {@code base} with {@code attachment} (named by {@code attachmentId}, its full id)
     * attached through the real {@link Attach#matches}/{@link Attach#assemble} pair, or empty if
     * {@link Attach#matches} refuses it (the slot is missing from {@code base}'s class, or already
     * filled) — the same refusal {@code /firearms debug give} surfaces as {@code
     * command.firearms.debug.give.cannot_attach}.
     */
    public static Optional<ItemStack> attach(ItemStack base, Identifier attachmentId, Attachment attachment) {
        Item attachmentItem = ItemRegistration.attachment(attachment.slot());
        ItemStack addition = new ItemStack(attachmentItem);
        addition.set(ComponentRegistration.attachmentComponent(attachment.slot()), attachmentId);
        if (!Attach.matches(base, addition)) {
            return Optional.empty();
        }
        return Optional.of(Attach.assemble(base, addition));
    }

    /**
     * A copy of {@code stack} with {@code firearms:ammo} set to {@code loadout}'s own derived
     * magazine size at {@code weaponBase}'s own calibre — "loaded to capacity."
     */
    public static ItemStack loaded(ItemStack stack, WeaponBase weaponBase, Loadout loadout) {
        Stats stats = StatDerivation.stats(loadout, ignored -> { });
        Identifier caliberId = Firearms.id(Ids.slug(weaponBase.caliber()));
        ItemStack result = stack.copy();
        result.set(ComponentRegistration.AMMO, new Ammo(caliberId, stats.magazineSize()));
        return result;
    }

    private WeaponStacks() {
    }
}
