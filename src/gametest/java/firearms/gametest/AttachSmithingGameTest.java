package firearms.gametest;

import firearms.Firearms;
import firearms.component.Base;
import firearms.component.ComponentRegistration;
import firearms.item.ItemRegistration;
import firearms.model.Slot;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/**
 * A real {@link SmithingMenu} runs {@link firearms.attach.AttachSmithingRecipe} exactly as the
 * vanilla smithing table would, proving the recipe is actually found by
 * {@code RecipeManager.getRecipeFor(RecipeType.SMITHING, ...)} through a real data-loaded
 * {@code data/firearms/recipe/attach.json} — zero mixin (`docs/spec/domains/attach.md`
 * `ATTACH-REQ-001`, `002`, `003`; `docs/spec/04-architecture.md` `ARCH-DEC-002`).
 */
public final class AttachSmithingGameTest {

    @GameTest
    public void aSuppressorAttachesToAMicroUziIntoTheMuzzleSlot(GameTestHelper helper) {
        SmithingMenu menu = smithingMenu(helper);
        ItemStack uzi = microUzi();
        ItemStack suppressor = attachment(Slot.MUZZLE, "suppressor");

        menu.getSlot(SmithingMenu.BASE_SLOT).set(uzi.copy());
        menu.getSlot(SmithingMenu.ADDITIONAL_SLOT).set(suppressor.copy());
        menu.createResult();

        ItemStack result = menu.getSlot(SmithingMenu.RESULT_SLOT).getItem();
        helper.assertFalse(result.isEmpty(), "a suppressor must attach into a Micro Uzi's empty muzzle slot (ATTACH-REQ-001)");
        helper.assertTrue(result.getItem() == ItemRegistration.WEAPON, "the result must still be firearms:weapon");

        Identifier muzzle = result.get(ComponentRegistration.attachmentComponent(Slot.MUZZLE));
        helper.assertTrue(muzzle != null && muzzle.equals(Firearms.id("suppressor")),
            "the result must carry firearms:attachment_muzzle = firearms:suppressor, was " + muzzle);

        Base resultBase = result.get(ComponentRegistration.BASE);
        Base uziBase = uzi.get(ComponentRegistration.BASE);
        helper.assertTrue(resultBase != null && resultBase.equals(uziBase), "every other component, including firearms:base, must be unchanged (ATTACH-REQ-003)");

        helper.assertValueEqual(result.getDamageValue(), uzi.getDamageValue(), "assemble must leave durability untouched");
        helper.assertValueEqual(result.getMaxDamage(), uzi.getMaxDamage(), "assemble must leave max damage untouched");

        helper.succeed();
    }

    @GameTest
    public void aSecondSuppressorOnAnAlreadySuppressedUziMatchesNothing(GameTestHelper helper) {
        SmithingMenu menu = smithingMenu(helper);
        ItemStack alreadySuppressed = microUzi();
        alreadySuppressed.set(ComponentRegistration.attachmentComponent(Slot.MUZZLE), Firearms.id("suppressor"));

        menu.getSlot(SmithingMenu.BASE_SLOT).set(alreadySuppressed.copy());
        menu.getSlot(SmithingMenu.ADDITIONAL_SLOT).set(attachment(Slot.MUZZLE, "suppressor").copy());
        menu.createResult();

        ItemStack result = menu.getSlot(SmithingMenu.RESULT_SLOT).getItem();
        helper.assertTrue(result.isEmpty(), "the recipe must never match an already-occupied slot (ATTACH-REQ-002)");
        helper.succeed();
    }

    @GameTest
    public void aGripOnAWinchester1897MatchesNothingSinceTheShotgunClassHasNoGripSlot(GameTestHelper helper) {
        SmithingMenu menu = smithingMenu(helper);
        menu.getSlot(SmithingMenu.BASE_SLOT).set(winchester1897().copy());
        menu.getSlot(SmithingMenu.ADDITIONAL_SLOT).set(attachment(Slot.GRIP, "vertical_grip").copy());
        menu.createResult();

        ItemStack result = menu.getSlot(SmithingMenu.RESULT_SLOT).getItem();
        helper.assertTrue(result.isEmpty(), "a shotgun has no grip slot at all (ATTACH-FAIL-001)");
        helper.succeed();
    }

    @GameTest
    public void aStockOnAnM1911MatchesNothingSinceThePistolClassHasNoStockSlot(GameTestHelper helper) {
        SmithingMenu menu = smithingMenu(helper);
        menu.getSlot(SmithingMenu.BASE_SLOT).set(m1911().copy());
        menu.getSlot(SmithingMenu.ADDITIONAL_SLOT).set(attachment(Slot.STOCK, "tactical_stock").copy());
        menu.createResult();

        ItemStack result = menu.getSlot(SmithingMenu.RESULT_SLOT).getItem();
        helper.assertTrue(result.isEmpty(), "a pistol has no stock slot at all (ATTACH-FAIL-001)");
        helper.succeed();
    }

    private static SmithingMenu smithingMenu(GameTestHelper helper) {
        Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        Inventory inventory = player.getInventory();
        return new SmithingMenu(0, inventory);
    }

    private static ItemStack microUzi() {
        ItemStack stack = new ItemStack(ItemRegistration.WEAPON);
        stack.set(ComponentRegistration.BASE, Base.of(Firearms.id("micro_uzi")));
        stack.set(DataComponents.MAX_DAMAGE, 300);
        stack.set(DataComponents.DAMAGE, 0);
        return stack;
    }

    private static ItemStack winchester1897() {
        ItemStack stack = new ItemStack(ItemRegistration.WEAPON);
        stack.set(ComponentRegistration.BASE, Base.of(Firearms.id("winchester_model_1897")));
        stack.set(DataComponents.MAX_DAMAGE, 200);
        stack.set(DataComponents.DAMAGE, 0);
        return stack;
    }

    private static ItemStack m1911() {
        ItemStack stack = new ItemStack(ItemRegistration.WEAPON);
        stack.set(ComponentRegistration.BASE, Base.of(Firearms.id("m1911")));
        stack.set(DataComponents.MAX_DAMAGE, 250);
        stack.set(DataComponents.DAMAGE, 0);
        return stack;
    }

    private static ItemStack attachment(Slot slot, String attachmentId) {
        ItemStack stack = new ItemStack(ItemRegistration.attachment(slot));
        stack.set(ComponentRegistration.attachmentComponent(slot), Firearms.id(attachmentId));
        return stack;
    }
}
