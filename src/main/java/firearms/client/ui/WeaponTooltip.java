package firearms.client.ui;

import firearms.component.Ammo;
import firearms.component.ComponentRegistration;
import firearms.data.AttachmentRegistry;
import firearms.fire.WeaponLoadouts;
import firearms.item.AttachmentItem;
import firearms.item.ItemRegistration;
import firearms.model.Attachment;
import firearms.model.Caliber;
import firearms.model.FireMode;
import firearms.model.Loadout;
import firearms.model.Modifier;
import firearms.model.Slot;
import firearms.model.StatDerivation;
import firearms.model.Stats;
import firearms.model.WeaponBase;
import firearms.support.Ids;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The weapon, attachment and cartridge tooltips (`docs/spec/domains/ui.md` `UI-REQ-001`, §3): a
 * weapon stack's own name and class, its nine live-derived stats, every slot its class shows
 * (occupied or "unequipped"), and its loaded ammo vs. derived magazine size; an attachment stack's
 * own slot and its "+/-" modifier lines; a cartridge stack's own calibre. Every number goes
 * through {@link StatsText}, and every stat line calls {@link StatDerivation#stats} — the exact
 * same pure function {@code firearms.fire.FiringLogic} fires with, via {@link
 * WeaponLoadouts#of(ItemStack)} — never a cached or duplicated computation, so a tooltip is
 * correct the instant an attachment lands with no stale read (`WEAPON-REQ-003`, `UI-DEC`
 * reasoning this ticket protects structurally).
 *
 * <p>{@link #lines(ItemStack)} is pure of the client-only Fabric callback, mirroring {@code
 * create_metered_motor}'s own {@code MeteredMotorTooltip#lines}, so a server-only game test calls
 * it directly (`firearms.gametest.WeaponTooltipGameTest`).
 */
public final class WeaponTooltip {
    private WeaponTooltip() {
    }

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, flag, tooltipLines) -> tooltipLines.addAll(lines(stack)));
    }

    /**
     * The tooltip lines for {@code stack}: an attachment item's own slot and modifiers, a
     * cartridge item's own calibre, or a weapon item's own name, class, nine derived stats, every
     * class slot's occupant or "unequipped," and its ammo line — recomputed fresh from {@code
     * stack}'s own current components every call, never cached (`UI-REQ-001`). Empty for any other
     * item, or for a weapon/attachment stack whose referenced base or attachment id no longer
     * resolves against the currently-loaded data (`WEAPON-FAIL-004`).
     */
    public static List<Component> lines(ItemStack stack) {
        if (stack.getItem() instanceof AttachmentItem attachmentItem) {
            return attachmentLines(stack, attachmentItem);
        }
        Caliber cartridgeCaliber = cartridgeCaliber(stack.getItem());
        if (cartridgeCaliber != null) {
            return List.of(Component.translatable("tooltip.firearms.cartridge.caliber", cartridgeCaliber.displayId()));
        }
        return WeaponLoadouts.of(stack).map(loadout -> weaponLines(stack, loadout)).orElse(List.of());
    }

    private static List<Component> weaponLines(ItemStack stack, Loadout loadout) {
        WeaponBase weaponBase = loadout.base();
        Stats stats = StatDerivation.stats(loadout, ignored -> { });

        List<Component> lines = new ArrayList<>();
        Component name = Component.translatable("item.firearms.weapon." + weaponBase.id());
        Component weaponClass = Component.translatable("tooltip.firearms.weapon_class." + Ids.slug(weaponBase.weaponClass()));
        lines.add(Component.translatable("tooltip.firearms.header", name, weaponClass));

        lines.add(Component.translatable("tooltip.firearms.stat.damage", StatsText.oneDecimal(stats.damage())));
        lines.add(Component.translatable("tooltip.firearms.stat.muzzle_velocity", StatsText.oneDecimal(stats.muzzleVelocity())));
        lines.add(Component.translatable("tooltip.firearms.stat.spread", StatsText.oneDecimal(stats.spread())));
        lines.add(Component.translatable("tooltip.firearms.stat.fire_rate", StatsText.whole(stats.fireRateTicks())));
        lines.add(Component.translatable("tooltip.firearms.stat.fire_mode", fireModeName(stats.fireMode())));
        lines.add(Component.translatable("tooltip.firearms.stat.magazine_size", StatsText.whole(stats.magazineSize())));
        lines.add(Component.translatable("tooltip.firearms.stat.reload_ticks", StatsText.whole(stats.reloadTicks())));
        lines.add(Component.translatable("tooltip.firearms.stat.recoil", StatsText.oneDecimal(stats.recoilVertical())));
        lines.add(Component.translatable("tooltip.firearms.stat.durability", StatsText.whole(durabilityRemaining(stack, stats))));

        for (Slot slot : Slot.values()) {
            if (!weaponBase.weaponClass().hasSlot(slot)) {
                continue;
            }
            Component occupant = loadout.attachment(slot)
                .<Component>map(a -> Component.translatable("item.firearms.attachment." + a.id()))
                .orElseGet(() -> Component.translatable("tooltip.firearms.unequipped"));
            lines.add(Component.translatable("tooltip.firearms.slot." + Ids.slug(slot), occupant));
        }

        Ammo ammo = stack.get(ComponentRegistration.AMMO);
        int loaded = ammo == null ? 0 : ammo.loaded();
        lines.add(Component.translatable(
            "tooltip.firearms.ammo", StatsText.whole(loaded), StatsText.whole(stats.magazineSize()), weaponBase.caliber().displayId()));

        return lines;
    }

    private static List<Component> attachmentLines(ItemStack stack, AttachmentItem attachmentItem) {
        Slot slot = attachmentItem.slot();
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("tooltip.firearms.attachment.slot", Component.translatable("tooltip.firearms.slot_name." + Ids.slug(slot))));

        Identifier attachmentId = stack.get(ComponentRegistration.attachmentComponent(slot));
        if (attachmentId == null) {
            return lines;
        }
        Attachment attachment = AttachmentRegistry.get(attachmentId).orElse(null);
        if (attachment == null) {
            return lines;
        }
        for (Modifier modifier : attachment.modifiers()) {
            lines.add(modifierLine(modifier));
        }
        return lines;
    }

    private static Component modifierLine(Modifier modifier) {
        String value = switch (modifier.op()) {
            case MULTIPLY -> StatsText.percentChange(modifier.value());
            case ADD -> StatsText.signed(modifier.value());
        };
        Component statName = Component.translatable("tooltip.firearms.stat_name." + Ids.slug(modifier.stat()));
        return Component.translatable("tooltip.firearms.modifier", statName, value);
    }

    /** {@code item}'s own calibre if it is one of the six cartridge items ({@code ItemRegistration#cartridge}), else {@code null}. */
    private static Caliber cartridgeCaliber(Item item) {
        for (Caliber caliber : Caliber.values()) {
            if (ItemRegistration.cartridge(caliber) == item) {
                return caliber;
            }
        }
        return null;
    }

    private static Component fireModeName(FireMode mode) {
        return Component.translatable("tooltip.firearms.fire_mode." + Ids.slug(mode));
    }

    /**
     * The derived durability minus shots already taken ({@code stack}'s own vanilla {@code
     * DataComponents.DAMAGE}), floored at zero. Reads the live-derived {@link Stats#durability()}
     * as the ceiling rather than the stack's own {@code DataComponents.MAX_DAMAGE} component
     * directly: no 1.0 attachment modifies {@code Stat.DURABILITY} so the two always agree today,
     * but a future one that did would make the derived value the correct ceiling, matching every
     * other line on this tooltip reading through {@link StatDerivation} rather than a raw
     * component.
     */
    private static int durabilityRemaining(ItemStack stack, Stats stats) {
        return Math.max(0, stats.durability() - stack.getDamageValue());
    }
}
