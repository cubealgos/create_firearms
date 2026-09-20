package firearms.gametest;

import firearms.component.ComponentRegistration;
import firearms.item.ItemRegistration;
import firearms.model.Caliber;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

/**
 * `docs/spec/domains/ammo.md` `AMMO-REQ-002`, `AMMO-REQ-003`: every one of the six cartridge items
 * stacks to 64 (matching vanilla arrows), and a cartridge stack carries none of this mod's own
 * seven data components — it is identified purely by its own item id, one per calibre, never by a
 * component the way a weapon or attachment item is (`docs/spec/contracts/data-contract.md`).
 */
public final class AmmoCartridgeGameTest {

    @GameTest
    public void everyCartridgeStacksToSixtyFour(GameTestHelper helper) {
        for (Caliber caliber : Caliber.values()) {
            ItemStack stack = new ItemStack(ItemRegistration.cartridge(caliber));
            helper.assertValueEqual(stack.getMaxStackSize(), 64, caliber + "'s own cartridge must stack to 64 (AMMO-REQ-002)");
        }
        helper.succeed();
    }

    @GameTest
    public void aCartridgeCarriesNoneOfThisModsOwnComponents(GameTestHelper helper) {
        for (Caliber caliber : Caliber.values()) {
            ItemStack stack = new ItemStack(ItemRegistration.cartridge(caliber));
            helper.assertTrue(stack.get(ComponentRegistration.BASE) == null, caliber + "'s cartridge must carry no firearms:base (AMMO-REQ-003)");
            helper.assertTrue(stack.get(ComponentRegistration.AMMO) == null, caliber + "'s cartridge must carry no firearms:ammo (AMMO-REQ-003)");
            helper.assertTrue(stack.get(ComponentRegistration.ATTACHMENT_MUZZLE) == null, caliber + "'s cartridge must carry no attachment_muzzle");
            helper.assertTrue(stack.get(ComponentRegistration.ATTACHMENT_OPTIC) == null, caliber + "'s cartridge must carry no attachment_optic");
            helper.assertTrue(stack.get(ComponentRegistration.ATTACHMENT_MAGAZINE) == null, caliber + "'s cartridge must carry no attachment_magazine");
            helper.assertTrue(stack.get(ComponentRegistration.ATTACHMENT_GRIP) == null, caliber + "'s cartridge must carry no attachment_grip");
            helper.assertTrue(stack.get(ComponentRegistration.ATTACHMENT_STOCK) == null, caliber + "'s cartridge must carry no attachment_stock");
        }
        helper.succeed();
    }
}
