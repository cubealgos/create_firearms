package firearms.item;

import firearms.component.Base;
import firearms.component.ComponentRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The one weapon item, {@code firearms:weapon}: every 1.0 base weapon is a stack of this same
 * item, distinguished only by its own {@code firearms:base.weapon_id} component
 * (`docs/spec/04-architecture.md` `ARCH-DEC-005`, `docs/spec/contracts/public-surface.md`) — the
 * mechanism `docs/spec/domains/weapon.md` {@code WEAPON-DEC-003} relies on: a new base weapon in
 * an existing class is one data file plus one crafting recipe, never a new {@code Item}
 * registration. Carries no {@code repairable}/{@code enchantable} component
 * (`docs/spec/decisions/DEC-017-no-detach-durability.md`), and no fixed {@code durability(int)}
 * either — {@code max_damage} differs per base, so each base's own crafting recipe sets it via a
 * {@code components} output patch instead of one shared {@link Item.Properties} value
 * (`firearms.item.ItemRegistration`).
 */
public final class WeaponItem extends Item {
    private static final String NAME_PREFIX = "item.firearms.weapon.";

    public WeaponItem(Properties properties) {
        super(properties);
    }

    /**
     * "M1911", "AKM", and so on, read off this stack's own {@code firearms:base} component; falls
     * back to the item's own default name (`item.firearms.weapon` in {@code en_us.json}) for a
     * bare stack carrying no component, e.g. a creative-mode {@code /give} with none given.
     */
    @Override
    public Component getName(ItemStack stack) {
        Base base = stack.get(ComponentRegistration.BASE);
        if (base == null) {
            return super.getName(stack);
        }
        return Component.translatable(NAME_PREFIX + base.weaponId().getPath());
    }
}
