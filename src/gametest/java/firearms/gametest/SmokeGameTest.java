package firearms.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** M0: the mod loads beside Create Fly; everything else follows. */
public final class SmokeGameTest {
    @GameTest
    public void theModLoadsBesideCreateFly(GameTestHelper helper) {
        helper.assertTrue(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("create"), "Create Fly is loaded");
        helper.assertTrue(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("firearms"), "the mod is loaded");
        helper.succeed();
    }
}
