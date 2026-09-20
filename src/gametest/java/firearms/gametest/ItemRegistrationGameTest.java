package firearms.gametest;

import firearms.Firearms;
import firearms.model.Caliber;
import firearms.model.Slot;
import firearms.support.Ids;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

/**
 * Every item `FA-3` registers resolves at its own id: the one weapon item, the five attachment
 * items, and the six cartridge items (`docs/spec/contracts/public-surface.md`).
 */
public final class ItemRegistrationGameTest {

    @GameTest
    public void everyRegisteredItemResolvesAtItsId(GameTestHelper helper) {
        assertRegistered(helper, "weapon");
        for (Slot slot : Slot.values()) {
            assertRegistered(helper, "attachment_" + Ids.slug(slot));
        }
        for (Caliber caliber : Caliber.values()) {
            assertRegistered(helper, "cartridge_" + Ids.slug(caliber));
        }
        helper.succeed();
    }

    private static void assertRegistered(GameTestHelper helper, String path) {
        Identifier id = Firearms.id(path);
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(id), id + " must resolve to a registered item");
    }
}
