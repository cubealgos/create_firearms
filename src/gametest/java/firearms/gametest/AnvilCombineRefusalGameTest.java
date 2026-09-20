package firearms.gametest;

import firearms.Firearms;
import firearms.component.Base;
import firearms.component.ComponentRegistration;
import firearms.item.ItemRegistration;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/**
 * `FA-4`: settles `WEAPON-FAIL-006` (`docs/spec/domains/weapon.md` §7's own open question, closed
 * by Kevin's ruling recorded at `WEAPON-DEC-005`) — the anvil's same-item combine-repair path,
 * independent of `DataComponents.REPAIRABLE`, closed for `firearms:weapon` specifically by
 * `firearms.mixin.AnvilMenuMixin`, not by component omission. Also proves the mixin's own scope
 * (an unrelated vanilla item still combines normally) and restates `WEAPON-REQ-014`/`015`'s
 * material-repair and enchanting refusals against this ticket's own real menus, so this one file is
 * a complete, self-contained proof of every acceptance criterion `FA-4` names.
 */
public final class AnvilCombineRefusalGameTest {

    @GameTest
    public void twoDamagedWeaponStacksNeverCombineAtAnAnvil(GameTestHelper helper) {
        Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        Inventory inventory = player.getInventory();

        ItemStack left = m1911(250);
        left.setDamageValue(200);
        ItemStack right = m1911(250);
        right.setDamageValue(10);

        AnvilMenu menu = new AnvilMenu(0, inventory);
        menu.getSlot(AnvilMenu.INPUT_SLOT).set(left);
        menu.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(right);
        menu.createResult();

        ItemStack result = menu.getSlot(AnvilMenu.RESULT_SLOT).getItem();
        helper.assertTrue(result.isEmpty(),
            "two firearms:weapon stacks must never combine at an anvil (WEAPON-FAIL-006, AnvilMenuMixin)");
        helper.assertValueEqual(menu.getCost(), 0, "the anvil's own repair cost for a refused weapon combine");
        helper.succeed();
    }

    @GameTest
    public void twoDamagedIronPickaxesStillCombineAtAnAnvil(GameTestHelper helper) {
        // Scoping proof: AnvilMenuMixin must touch nothing outside firearms:weapon. Vanilla's own
        // same-item combine-repair path — the exact mechanism WEAPON-FAIL-006 names — still fires
        // for an ordinary vanilla item pair.
        Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        Inventory inventory = player.getInventory();

        ItemStack left = new ItemStack(Items.IRON_PICKAXE);
        left.setDamageValue(left.getMaxDamage() - 1);
        ItemStack right = new ItemStack(Items.IRON_PICKAXE);
        right.setDamageValue(right.getMaxDamage() / 2);

        AnvilMenu menu = new AnvilMenu(0, inventory);
        menu.getSlot(AnvilMenu.INPUT_SLOT).set(left);
        menu.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(right);
        menu.createResult();

        ItemStack result = menu.getSlot(AnvilMenu.RESULT_SLOT).getItem();
        helper.assertFalse(result.isEmpty(),
            "AnvilMenuMixin must not touch vanilla's own same-item combine-repair path for a non-weapon item");
        helper.succeed();
    }

    @GameTest
    public void aWeaponAndAnIronIngotYieldNoAnvilResult(GameTestHelper helper) {
        Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        Inventory inventory = player.getInventory();

        ItemStack damaged = m1911(250);
        damaged.setDamageValue(50);

        AnvilMenu menu = new AnvilMenu(0, inventory);
        menu.getSlot(AnvilMenu.INPUT_SLOT).set(damaged);
        menu.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(new ItemStack(Items.IRON_INGOT));
        menu.createResult();

        ItemStack result = menu.getSlot(AnvilMenu.RESULT_SLOT).getItem();
        helper.assertTrue(result.isEmpty(),
            "an anvil must offer no material-repair result for a weapon, by REPAIRABLE omission alone (WEAPON-REQ-014)");
        helper.succeed();
    }

    @GameTest
    public void aRealEnchantingTableOffersNoEnchantmentForAWeapon(GameTestHelper helper) {
        Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        Inventory inventory = player.getInventory();

        EnchantmentMenu menu = new EnchantmentMenu(0, inventory);
        Slot itemSlot = menu.getSlot(0);
        itemSlot.set(m1911(250));
        // EnchantmentMenu.slotsChanged() itself gates every enchantment roll behind
        // ItemStack.isEnchantable(): absent that component, it zeroes every cost/clue slot without
        // ever touching bookshelf count or the player's XP level, so this is exactly "the table
        // offers nothing," not a roundabout isEnchantable() re-check.
        menu.slotsChanged(itemSlot.container);

        helper.assertValueEqual(menu.costs[0], 0, "enchanting-table slot 0 cost for an unenchantable weapon");
        helper.assertValueEqual(menu.costs[1], 0, "enchanting-table slot 1 cost for an unenchantable weapon");
        helper.assertValueEqual(menu.costs[2], 0, "enchanting-table slot 2 cost for an unenchantable weapon");
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
