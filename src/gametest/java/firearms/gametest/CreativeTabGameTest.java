package firearms.gametest;

import firearms.Firearms;
import firearms.component.Ammo;
import firearms.component.Base;
import firearms.component.ComponentRegistration;
import firearms.data.WeaponRegistry;
import firearms.item.AttachmentItem;
import firearms.item.ItemRegistration;
import firearms.item.WeaponItem;
import firearms.model.Caliber;
import firearms.model.Slot;
import firearms.model.WeaponBase;
import firearms.model.WeaponClass;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * `FA-25`'s acceptance criteria for the one creative-mode tab {@code firearms:firearms}
 * (`docs/spec/domains/ui.md` `UI-REQ-007`, `docs/spec/decisions/DEC-019-controls.md` §Creative
 * tab): registered, iconed with a bare M1911, and its display list — {@link #ownedByFirearms}
 * filters out the gametest source set's own {@code data/datapack_test/} weapon and attachment
 * (`DataLoaderGameTest`'s own namespace, loaded into the same catalogues this tab reads and so
 * unavoidably present in the tab's live contents during this test run) — holds, in order, the six
 * bare weapons, one fully loaded example per class with every one of that class's own slots filled
 * and {@code firearms:ammo.loaded} above zero, the 22 attachments each with its own slot component
 * set, and the six cartridges.
 */
public final class CreativeTabGameTest {

    private static final List<String> WEAPON_IDS = List.of(
        "akm", "awm", "m1911", "micro_uzi", "ruger_mini_14", "winchester_model_1897");

    /** {@code docs/spec/domains/attach.md} §3's 22 attachments, sorted the same way the tab itself sorts its catalogue (by id path). */
    private static final List<String> ATTACHMENT_IDS = List.of(
        "angled_grip", "bullet_loops", "cheek_pad", "compensator", "extended_magazine",
        "extended_quickdraw_magazine", "flash_hider", "half_grip", "holo", "light_grip",
        "quickdraw_magazine", "red_dot", "scope_15x", "scope_2x", "scope_3x", "scope_4x",
        "scope_6x", "scope_8x", "suppressor", "tactical_stock", "thumb_grip", "vertical_grip");

    private static final int WEAPON_COUNT = WEAPON_IDS.size();
    private static final int ATTACHMENT_COUNT = ATTACHMENT_IDS.size();
    private static final int CARTRIDGE_COUNT = Caliber.values().length;
    private static final int TOTAL = WEAPON_COUNT + WEAPON_COUNT + ATTACHMENT_COUNT + CARTRIDGE_COUNT;

    @GameTest
    public void theTabIsRegisteredWithTheM1911Icon(GameTestHelper helper) {
        CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.getValue(Firearms.id("firearms"));
        helper.assertTrue(tab != null, "firearms:firearms must be registered as a creative-mode tab");

        ItemStack icon = tab.getIconItem();
        helper.assertTrue(icon.getItem() instanceof WeaponItem, "the icon must be a firearms:weapon stack");
        Base base = icon.get(ComponentRegistration.BASE);
        helper.assertTrue(base != null && base.weaponId().equals(Firearms.id("m1911")), "the icon must be the M1911: " + base);

        helper.succeed();
    }

    @GameTest
    public void theDisplayListHoldsEveryItemInOrder(GameTestHelper helper) {
        List<ItemStack> ours = ownDisplayItems(helper);
        helper.assertValueEqual(ours.size(), TOTAL,
            "6 bare weapons + 6 loaded + 22 attachments + 6 cartridges = " + TOTAL);

        int index = 0;
        for (String weaponId : WEAPON_IDS) {
            assertBareWeapon(helper, ours.get(index), weaponId);
            index++;
        }
        for (WeaponClass weaponClass : WeaponClass.values()) {
            assertLoadedExample(helper, ours.get(index), weaponClass);
            index++;
        }
        for (String attachmentId : ATTACHMENT_IDS) {
            assertAttachment(helper, ours.get(index), attachmentId);
            index++;
        }
        for (Caliber caliber : Caliber.values()) {
            ItemStack stack = ours.get(index);
            helper.assertTrue(stack.is(ItemRegistration.cartridge(caliber)), "cartridge " + index + " must be " + caliber + ", was " + stack);
            index++;
        }

        helper.succeed();
    }

    private static List<ItemStack> ownDisplayItems(GameTestHelper helper) {
        CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.getValue(Firearms.id("firearms"));
        helper.assertTrue(tab != null, "firearms:firearms must be registered as a creative-mode tab");

        CreativeModeTab.ItemDisplayParameters parameters = new CreativeModeTab.ItemDisplayParameters(
            helper.getLevel().enabledFeatures(), true, helper.getLevel().registryAccess());
        tab.buildContents(parameters);

        List<ItemStack> ours = new ArrayList<>();
        for (ItemStack stack : tab.getDisplayItems()) {
            if (ownedByFirearms(stack)) {
                ours.add(stack);
            }
        }
        return ours;
    }

    /**
     * Excludes the gametest source set's own {@code datapack_test:example_smg}/{@code
     * example_gadget} (`DataLoaderGameTest`), which the tab's own catalogue-driven segments load
     * and display right alongside this mod's own entries during this test run, exactly as a real
     * third-party datapack's would in production (`UI-REQ-007`'s own "a datapack-added weapon or
     * attachment appears too").
     */
    private static boolean ownedByFirearms(ItemStack stack) {
        if (stack.getItem() instanceof WeaponItem) {
            Base base = stack.get(ComponentRegistration.BASE);
            return base != null && base.weaponId().getNamespace().equals(Firearms.MOD_ID);
        }
        if (stack.getItem() instanceof AttachmentItem attachmentItem) {
            Identifier attachmentId = stack.get(ComponentRegistration.attachmentComponent(attachmentItem.slot()));
            return attachmentId != null && attachmentId.getNamespace().equals(Firearms.MOD_ID);
        }
        return true; // the six cartridges: registered directly under firearms:, never datapack-extensible
    }

    private static void assertBareWeapon(GameTestHelper helper, ItemStack stack, String weaponId) {
        helper.assertTrue(stack.getItem() instanceof WeaponItem, weaponId + ": must be a firearms:weapon stack");
        Base base = stack.get(ComponentRegistration.BASE);
        helper.assertTrue(base != null && base.weaponId().equals(Firearms.id(weaponId)), weaponId + ": firearms:base must name it, was " + base);
        helper.assertTrue(stack.get(ComponentRegistration.AMMO) == null, weaponId + ": a bare stack must carry no firearms:ammo");
        for (Slot slot : Slot.values()) {
            helper.assertTrue(stack.get(ComponentRegistration.attachmentComponent(slot)) == null,
                weaponId + ": a bare stack must carry no " + slot + " attachment");
        }
    }

    private static void assertLoadedExample(GameTestHelper helper, ItemStack stack, WeaponClass weaponClass) {
        helper.assertTrue(stack.getItem() instanceof WeaponItem, weaponClass + ": must be a firearms:weapon stack");
        Base base = stack.get(ComponentRegistration.BASE);
        helper.assertTrue(base != null, weaponClass + ": must carry firearms:base");
        WeaponBase weaponBase = WeaponRegistry.get(base.weaponId()).orElse(null);
        helper.assertTrue(weaponBase != null && weaponBase.weaponClass() == weaponClass,
            weaponClass + ": firearms:base must name a weapon of this class, was " + base);

        Ammo ammo = stack.get(ComponentRegistration.AMMO);
        helper.assertTrue(ammo != null && ammo.loaded() > 0, weaponClass + ": must be loaded to capacity, was " + ammo);

        for (Slot slot : Slot.values()) {
            Identifier attachmentId = stack.get(ComponentRegistration.attachmentComponent(slot));
            if (weaponClass.hasSlot(slot)) {
                Identifier expected = Firearms.id(expectedAttachment(slot, weaponClass));
                helper.assertTrue(expected.equals(attachmentId),
                    weaponClass + " " + slot + ": expected " + expected + ", was " + attachmentId);
            } else {
                helper.assertTrue(attachmentId == null, weaponClass + " " + slot + ": this class has no such slot, must stay empty");
            }
        }
    }

    /** `UI-REQ-007`'s own naming: suppressor, the class's own optic, extended magazine, vertical grip, tactical stock. */
    private static String expectedAttachment(Slot slot, WeaponClass weaponClass) {
        return switch (slot) {
            case MUZZLE -> "suppressor";
            case OPTIC -> switch (weaponClass) {
                case PISTOL, SMG -> "red_dot";
                case ASSAULT_RIFLE, DMR -> "scope_4x";
                case SNIPER_RIFLE -> "scope_8x";
                case SHOTGUN -> throw new IllegalStateException("the shotgun has no optic slot");
            };
            case MAGAZINE -> "extended_magazine";
            case GRIP -> "vertical_grip";
            case STOCK -> "tactical_stock";
        };
    }

    private static void assertAttachment(GameTestHelper helper, ItemStack stack, String attachmentId) {
        helper.assertTrue(stack.getItem() instanceof AttachmentItem, attachmentId + ": must be a firearms:attachment_<slot> stack");
        AttachmentItem attachmentItem = (AttachmentItem) stack.getItem();
        Identifier actual = stack.get(ComponentRegistration.attachmentComponent(attachmentItem.slot()));
        helper.assertTrue(Firearms.id(attachmentId).equals(actual), attachmentId + ": its own slot component must carry its id, was " + actual);
    }
}
