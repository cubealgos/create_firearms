package firearms.gametest;

import firearms.Firearms;
import firearms.component.Base;
import firearms.component.ComponentRegistration;
import firearms.item.ItemRegistration;
import firearms.model.Slot;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * Every one of the six base weapons, 22 attachments and six cartridges crafts at a real crafting
 * table via its own data file: a real {@code RecipeManager} lookup finds the recipe and {@code
 * assemble()} produces the expected item, carrying the expected components
 * (`docs/spec/domains/weapon.md` `WEAPON-REQ-006`, `docs/spec/domains/attach.md` `ATTACH-REQ-007`,
 * `docs/spec/domains/ammo.md` `AMMO-REQ-001`).
 */
public final class CraftingRecipeGameTest {
    private static final Item IRON_INGOT = Items.IRON_INGOT;
    private static final Item STICK = Items.STICK;
    private static final Item REDSTONE = Items.REDSTONE;
    private static final Item IRON_NUGGET = Items.IRON_NUGGET;
    private static final Item COPPER_INGOT = Items.COPPER_INGOT;
    private static final Item GUNPOWDER = Items.GUNPOWDER;

    @GameTest
    public void everyBaseWeaponCraftsWithItsOwnGridAndMaxDamage(GameTestHelper helper) {
        assertWeapon(helper, "m1911", 250, grid3x3(
            IRON_INGOT, IRON_INGOT, IRON_INGOT,
            STICK, null, REDSTONE,
            null, null, null));
        assertWeapon(helper, "micro_uzi", 300, grid3x3(
            IRON_INGOT, IRON_INGOT, IRON_INGOT,
            IRON_INGOT, null, REDSTONE,
            null, STICK, STICK));
        assertWeapon(helper, "akm", 400, grid3x3(
            IRON_INGOT, IRON_INGOT, IRON_INGOT,
            IRON_INGOT, IRON_INGOT, STICK,
            STICK, REDSTONE, REDSTONE));
        assertWeapon(helper, "ruger_mini_14", 350, grid3x3(
            IRON_INGOT, IRON_INGOT, IRON_INGOT,
            IRON_INGOT, IRON_INGOT, STICK,
            STICK, STICK, REDSTONE));
        assertWeapon(helper, "awm", 300, grid3x3(
            IRON_INGOT, IRON_INGOT, IRON_INGOT,
            IRON_INGOT, IRON_INGOT, IRON_INGOT,
            STICK, STICK, REDSTONE));
        assertWeapon(helper, "winchester_model_1897", 200, grid3x3(
            IRON_INGOT, IRON_INGOT, IRON_INGOT,
            IRON_INGOT, STICK, REDSTONE,
            REDSTONE, null, null));
        helper.succeed();
    }

    @GameTest
    public void everyAttachmentCraftsWithItsOwnIngredients(GameTestHelper helper) {
        assertAttachment(helper, Slot.MUZZLE, "suppressor", IRON_NUGGET, IRON_NUGGET, IRON_NUGGET, IRON_INGOT);
        assertAttachment(helper, Slot.MUZZLE, "compensator", IRON_NUGGET, IRON_NUGGET, IRON_INGOT);
        assertAttachment(helper, Slot.MUZZLE, "flash_hider", IRON_NUGGET, IRON_INGOT);

        assertAttachment(helper, Slot.OPTIC, "red_dot", IRON_NUGGET, REDSTONE);
        assertAttachment(helper, Slot.OPTIC, "holo", IRON_NUGGET, IRON_NUGGET, REDSTONE);
        assertAttachment(helper, Slot.OPTIC, "scope_2x", COPPER_INGOT, IRON_NUGGET);
        assertAttachment(helper, Slot.OPTIC, "scope_3x", COPPER_INGOT, IRON_NUGGET, IRON_NUGGET);
        assertAttachment(helper, Slot.OPTIC, "scope_4x", COPPER_INGOT, COPPER_INGOT, IRON_NUGGET);
        assertAttachment(helper, Slot.OPTIC, "scope_6x", COPPER_INGOT, COPPER_INGOT, IRON_NUGGET, IRON_NUGGET);
        assertAttachment(helper, Slot.OPTIC, "scope_8x", COPPER_INGOT, COPPER_INGOT, COPPER_INGOT, IRON_NUGGET);
        assertAttachment(helper, Slot.OPTIC, "scope_15x", COPPER_INGOT, COPPER_INGOT, COPPER_INGOT, IRON_NUGGET, IRON_NUGGET);

        assertAttachment(helper, Slot.MAGAZINE, "extended_magazine", IRON_INGOT, IRON_INGOT);
        assertAttachment(helper, Slot.MAGAZINE, "quickdraw_magazine", IRON_INGOT, REDSTONE);
        assertAttachment(helper, Slot.MAGAZINE, "extended_quickdraw_magazine", IRON_INGOT, IRON_INGOT, REDSTONE);

        assertAttachment(helper, Slot.GRIP, "vertical_grip", STICK, IRON_NUGGET);
        assertAttachment(helper, Slot.GRIP, "angled_grip", STICK, IRON_NUGGET, IRON_NUGGET);
        assertAttachment(helper, Slot.GRIP, "half_grip", STICK, STICK, IRON_NUGGET);
        assertAttachment(helper, Slot.GRIP, "light_grip", STICK);
        assertAttachment(helper, Slot.GRIP, "thumb_grip", STICK, REDSTONE);

        assertAttachment(helper, Slot.STOCK, "tactical_stock", STICK, STICK, IRON_INGOT);
        assertAttachment(helper, Slot.STOCK, "cheek_pad", STICK, IRON_INGOT);
        assertAttachment(helper, Slot.STOCK, "bullet_loops", STICK, STICK, REDSTONE);
        helper.succeed();
    }

    @GameTest
    public void everyCartridgeCraftsFourAtATime(GameTestHelper helper) {
        Item brassSheet = BuiltInRegistries.ITEM.getOptional(Identifier.fromNamespaceAndPath("create", "brass_sheet"))
            .orElseThrow(() -> new AssertionError("create:brass_sheet must be registered (AMMO-DEC-001)"));

        assertCartridge(helper, brassSheet, "acp_45", 1);
        assertCartridge(helper, brassSheet, "mm_9", 2);
        assertCartridge(helper, brassSheet, "mm_7_62", 3);
        assertCartridge(helper, brassSheet, "mm_5_56", 4);
        assertCartridge(helper, brassSheet, "magnum_300", 5);
        assertCartridge(helper, brassSheet, "gauge_12", 6);
        helper.succeed();
    }

    private static void assertWeapon(GameTestHelper helper, String weaponId, int maxDamage, CraftingInput input) {
        ItemStack result = assemble(helper, input, weaponId + "'s own crafting-table recipe must match its grid");

        helper.assertTrue(result.getItem() == ItemRegistration.WEAPON, weaponId + " must produce firearms:weapon");
        Base base = result.get(ComponentRegistration.BASE);
        helper.assertTrue(base != null, weaponId + "'s crafted stack must carry firearms:base");
        helper.assertTrue(base.weaponId().equals(Firearms.id(weaponId)), weaponId + "'s own weapon_id must be set");
        helper.assertValueEqual(result.getMaxDamage(), maxDamage, weaponId + "'s own max_damage");
    }

    private static void assertAttachment(GameTestHelper helper, Slot slot, String attachmentId, Item... ingredients) {
        CraftingInput input = row(ingredients);
        ItemStack result = assemble(helper, input, attachmentId + "'s own crafting-table recipe must match its ingredients");

        helper.assertTrue(result.getItem() == ItemRegistration.attachment(slot), attachmentId + " must produce its own slot item");
        Identifier attachmentComponent = result.get(ComponentRegistration.attachmentComponent(slot));
        helper.assertTrue(attachmentComponent != null && attachmentComponent.equals(Firearms.id(attachmentId)),
            attachmentId + "'s own slot component must be set to its own id, was " + attachmentComponent);
    }

    private static void assertCartridge(GameTestHelper helper, Item brassSheet, String caliberPath, int nuggets) {
        List<Item> ingredients = new ArrayList<>(List.of(brassSheet, GUNPOWDER));
        for (int i = 0; i < nuggets; i++) {
            ingredients.add(IRON_NUGGET);
        }
        CraftingInput input = row(ingredients.toArray(new Item[0]));
        ItemStack result = assemble(helper, input, caliberPath + "'s own cartridge recipe must match its ingredients");

        Item expected = BuiltInRegistries.ITEM.getOptional(Firearms.id("cartridge_" + caliberPath))
            .orElseThrow(() -> new AssertionError("firearms:cartridge_" + caliberPath + " must be registered"));
        helper.assertTrue(result.getItem() == expected, caliberPath + " must produce its own cartridge item");
        helper.assertValueEqual(result.getCount(), 4, caliberPath + "'s own recipe must output 4 (AMMO-REQ-001)");
    }

    /** Looks up {@code input} against a real {@code RecipeManager} and assembles the match, failing with {@code message} if none matches. */
    private static ItemStack assemble(GameTestHelper helper, CraftingInput input, String message) {
        ServerLevel level = helper.getLevel();
        RecipeHolder<CraftingRecipe> holder = level.recipeAccess()
            .getRecipeFor(RecipeType.CRAFTING, input, level)
            .orElseThrow(() -> new AssertionError(message));
        return holder.value().assemble(input);
    }

    private static CraftingInput grid3x3(Item... items) {
        if (items.length != 9) {
            throw new IllegalArgumentException("a 3x3 grid needs exactly 9 cells");
        }
        List<ItemStack> stacks = new ArrayList<>(9);
        for (Item item : items) {
            stacks.add(item == null ? ItemStack.EMPTY : new ItemStack(item));
        }
        return CraftingInput.of(3, 3, stacks);
    }

    private static CraftingInput row(Item... items) {
        List<ItemStack> stacks = new ArrayList<>(items.length);
        for (Item item : items) {
            stacks.add(new ItemStack(item));
        }
        return CraftingInput.of(items.length, 1, stacks);
    }
}
