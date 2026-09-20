package firearms.gametest;

import firearms.Firearms;
import firearms.client.ui.WeaponTooltip;
import firearms.component.Ammo;
import firearms.component.Base;
import firearms.component.ComponentRegistration;
import firearms.item.ItemRegistration;
import firearms.model.Caliber;
import firearms.model.Slot;
import firearms.model.WeaponBase;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;

/**
 * `FA-11`'s acceptance criteria for {@code firearms.client.ui.WeaponTooltip}
 * (`docs/spec/domains/ui.md` `UI-REQ-001`): an AKM with a suppressor and a 4x scope lists its
 * name/class, nine derived stat lines, every one of the assault rifle's five slots (occupied or
 * "unequipped"), and its ammo line, in that fixed order; an attachment stack lists its own slot
 * and "+/-" modifier lines; a cartridge stack lists its own calibre. {@link
 * WeaponTooltip#lines(ItemStack)} is pure of the client-only Fabric callback, so this
 * dedicated-server game test calls it directly, mirroring {@code create_metered_motor}'s own
 * {@code TooltipGameTest}. Assertions check translation keys, not rendered strings — a game test
 * environment has no client lang file loaded to resolve one.
 */
public final class WeaponTooltipGameTest {

    @GameTest
    public void akmWithSuppressorAndScope4xListsTheExpectedKeysInOrder(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ItemRegistration.WEAPON);
        stack.set(ComponentRegistration.BASE, Base.of(Firearms.id("akm")));
        stack.set(ComponentRegistration.attachmentComponent(Slot.MUZZLE), Firearms.id("suppressor"));
        stack.set(ComponentRegistration.attachmentComponent(Slot.OPTIC), Firearms.id("scope_4x"));
        stack.set(DataComponents.MAX_DAMAGE, WeaponBase.AKM.baseStats().durability());
        stack.set(DataComponents.DAMAGE, 0);
        stack.set(ComponentRegistration.AMMO, new Ammo(Firearms.id("mm_7_62"), 12));

        List<Component> lines = WeaponTooltip.lines(stack);

        List<String> expectedKeys = List.of(
            "tooltip.firearms.header",
            "tooltip.firearms.stat.damage",
            "tooltip.firearms.stat.muzzle_velocity",
            "tooltip.firearms.stat.spread",
            "tooltip.firearms.stat.fire_rate",
            "tooltip.firearms.stat.fire_mode",
            "tooltip.firearms.stat.magazine_size",
            "tooltip.firearms.stat.reload_ticks",
            "tooltip.firearms.stat.recoil",
            "tooltip.firearms.stat.durability",
            "tooltip.firearms.slot.muzzle",
            "tooltip.firearms.slot.optic",
            "tooltip.firearms.slot.magazine",
            "tooltip.firearms.slot.grip",
            "tooltip.firearms.slot.stock",
            "tooltip.firearms.ammo"
        );
        List<String> actualKeys = lines.stream().map(WeaponTooltipGameTest::key).toList();
        helper.assertTrue(expectedKeys.equals(actualKeys), "the expected keys in order, was " + actualKeys);

        // The occupied slots name their attachment; the empty ones name "unequipped" (UI-REQ-001).
        helper.assertTrue(
            firstArgKey(lines.get(10)).equals("item.firearms.attachment.suppressor"), "muzzle names the suppressor: " + describe(lines.get(10)));
        helper.assertTrue(
            firstArgKey(lines.get(11)).equals("item.firearms.attachment.scope_4x"), "optic names the 4x scope: " + describe(lines.get(11)));
        helper.assertTrue(
            firstArgKey(lines.get(12)).equals("tooltip.firearms.unequipped"), "magazine is unequipped: " + describe(lines.get(12)));
        helper.assertTrue(
            firstArgKey(lines.get(13)).equals("tooltip.firearms.unequipped"), "grip is unequipped: " + describe(lines.get(13)));
        helper.assertTrue(
            firstArgKey(lines.get(14)).equals("tooltip.firearms.unequipped"), "stock is unequipped: " + describe(lines.get(14)));

        // "Ammo: 12 / 30 (7.62mm)" -- loaded, derived magazine size, calibre.
        Object[] ammoArgs = args(lines.get(15));
        helper.assertTrue(ammoArgs.length == 3, "three ammo args: " + describe(lines.get(15)));
        helper.assertTrue(ammoArgs[0].equals("12"), "loaded rounds: " + describe(lines.get(15)));
        helper.assertTrue(ammoArgs[1].equals("30"), "derived magazine size, unaffected by these two attachments: " + describe(lines.get(15)));
        helper.assertTrue(ammoArgs[2].equals("7.62mm"), "the AKM's own calibre: " + describe(lines.get(15)));

        helper.succeed();
    }

    @GameTest
    public void aSuppressorAttachmentListsItsSlotAndModifiersAsPlusMinusLines(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ItemRegistration.attachment(Slot.MUZZLE));
        stack.set(ComponentRegistration.attachmentComponent(Slot.MUZZLE), Firearms.id("suppressor"));

        List<Component> lines = WeaponTooltip.lines(stack);

        helper.assertTrue(lines.size() == 3, "the slot line plus the suppressor's own two modifiers: " + describe(lines));
        helper.assertTrue(key(lines.get(0)).equals("tooltip.firearms.attachment.slot"), "the slot line: " + describe(lines));
        helper.assertTrue(firstArgKey(lines.get(0)).equals("tooltip.firearms.slot_name.muzzle"), "names the muzzle slot: " + describe(lines));

        helper.assertTrue(key(lines.get(1)).equals("tooltip.firearms.modifier"), "the spread modifier line: " + describe(lines));
        helper.assertTrue(firstArgKey(lines.get(1)).equals("tooltip.firearms.stat_name.spread"), "names spread: " + describe(lines));
        helper.assertTrue(args(lines.get(1))[1].equals("-10%"), "spread ×0.90 reads as -10%: " + describe(lines));

        helper.assertTrue(key(lines.get(2)).equals("tooltip.firearms.modifier"), "the muzzle velocity modifier line: " + describe(lines));
        helper.assertTrue(firstArgKey(lines.get(2)).equals("tooltip.firearms.stat_name.muzzle_velocity"), "names muzzle velocity: " + describe(lines));
        helper.assertTrue(args(lines.get(2))[1].equals("-3%"), "muzzle velocity ×0.97 reads as -3%: " + describe(lines));

        helper.succeed();
    }

    @GameTest
    public void aCartridgeListsItsCalibre(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ItemRegistration.cartridge(Caliber.ACP_45));

        List<Component> lines = WeaponTooltip.lines(stack);

        helper.assertTrue(lines.size() == 1, "one calibre line: " + describe(lines));
        helper.assertTrue(key(lines.get(0)).equals("tooltip.firearms.cartridge.caliber"), "the calibre key: " + describe(lines));
        helper.assertTrue(args(lines.get(0))[0].equals(".45 ACP"), "the real-world calibre designation: " + describe(lines));

        helper.succeed();
    }

    @GameTest
    public void anUnrelatedVanillaItemHasNoLines(GameTestHelper helper) {
        List<Component> lines = WeaponTooltip.lines(new ItemStack(net.minecraft.world.item.Items.STICK));
        helper.assertTrue(lines.isEmpty(), "no tooltip lines for an item this mod never touches: " + describe(lines));
        helper.succeed();
    }

    private static String key(Component component) {
        return component.getContents() instanceof TranslatableContents t ? t.getKey() : component.getString();
    }

    private static Object[] args(Component component) {
        return component.getContents() instanceof TranslatableContents t ? t.getArgs() : new Object[0];
    }

    /** The first arg's own translation key, when that arg is itself a translatable {@link Component}. */
    private static String firstArgKey(Component component) {
        Object[] args = args(component);
        return args.length > 0 && args[0] instanceof Component c ? key(c) : String.valueOf(args.length > 0 ? args[0] : null);
    }

    private static String describe(Component component) {
        return key(component) + java.util.Arrays.toString(args(component));
    }

    private static String describe(List<Component> lines) {
        return lines.stream().map(WeaponTooltipGameTest::describe).toList().toString();
    }
}
