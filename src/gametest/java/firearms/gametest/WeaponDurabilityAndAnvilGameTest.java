package firearms.gametest;

import firearms.Firearms;
import firearms.component.Base;
import firearms.component.ComponentRegistration;
import firearms.item.ItemRegistration;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * A weapon stack carries the {@code max_damage} its own base's recipe gives it
 * (`docs/spec/domains/weapon.md` §3), is never enchantable and never accepts a material repair —
 * closed by omitting {@code repairable(...)}/{@code enchantable(...)} at registration, not by a
 * mixin (`WEAPON-REQ-014`, `015`; `decisions/DEC-017-no-detach-durability.md`) — proven against a
 * real {@link AnvilMenu#createResult()}, the same menu class the smithing table's own repair path
 * runs through.
 */
public final class WeaponDurabilityAndAnvilGameTest {

    @GameTest
    public void anM1911StackHasTheRecipesOwnMaxDamage(GameTestHelper helper) {
        ItemStack stack = m1911(250);
        helper.assertValueEqual(stack.getMaxDamage(), 250, "the M1911's own max_damage");
        helper.succeed();
    }

    @GameTest
    public void aWeaponStackIsNeverEnchantableAndNeverAValidRepairTarget(GameTestHelper helper) {
        ItemStack stack = m1911(250);
        helper.assertFalse(stack.isEnchantable(), "a weapon must never be enchantable (WEAPON-REQ-015)");
        helper.assertFalse(stack.isValidRepairItem(new ItemStack(Items.IRON_INGOT)),
            "a weapon must never accept an iron ingot as a repair material (WEAPON-REQ-014)");
        helper.succeed();
    }

    @GameTest
    public void aRealAnvilOffersNoMaterialRepairAndNoEnchantForADamagedWeapon(GameTestHelper helper) {
        // Survival, not the default creative-leaning mock: a creative player's
        // hasInfiniteMaterials() short-circuits the anvil's own enchanted-book compatibility check
        // (AnvilMenu.createResult(), a real vanilla creative-mode convenience), which would hide
        // exactly the tag-membership gap WEAPON-REQ-015 relies on.
        Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        Inventory inventory = player.getInventory();

        ItemStack damaged = m1911(250);
        damaged.setDamageValue(50);

        AnvilMenu materialRepair = new AnvilMenu(0, inventory);
        materialRepair.getSlot(AnvilMenu.INPUT_SLOT).set(damaged.copy());
        materialRepair.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(new ItemStack(Items.IRON_INGOT));
        materialRepair.createResult();
        ItemStack materialResult = materialRepair.getSlot(AnvilMenu.RESULT_SLOT).getItem();
        helper.assertTrue(materialResult.isEmpty(), "an anvil must offer no material-repair result for a weapon (WEAPON-REQ-014)");

        var sharpness = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        mutable.set(sharpness, 1);
        ItemStack enchantedBook = new ItemStack(Items.ENCHANTED_BOOK);
        enchantedBook.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());

        AnvilMenu enchantAttempt = new AnvilMenu(0, inventory);
        enchantAttempt.getSlot(AnvilMenu.INPUT_SLOT).set(damaged.copy());
        enchantAttempt.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(enchantedBook);
        enchantAttempt.createResult();
        ItemStack enchantResult = enchantAttempt.getSlot(AnvilMenu.RESULT_SLOT).getItem();
        helper.assertTrue(enchantResult.isEmpty(), "an anvil must transfer no enchantment onto a weapon (WEAPON-REQ-015)");

        helper.succeed();
    }

    private static ItemStack m1911(int maxDamage) {
        ItemStack stack = new ItemStack(ItemRegistration.WEAPON);
        stack.set(ComponentRegistration.BASE, Base.of(Firearms.id("m1911")));
        stack.set(DataComponents.MAX_DAMAGE, maxDamage);
        stack.set(DataComponents.DAMAGE, 0);
        return stack;
    }
}
