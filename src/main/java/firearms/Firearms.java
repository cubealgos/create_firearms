package firearms;

import firearms.attach.AttachRegistration;
import firearms.component.ComponentRegistration;
import firearms.data.DataRegistration;
import firearms.debug.DebugCommand;
import firearms.fire.FireNetworking;
import firearms.fire.FireSounds;
import firearms.fire.RecoilPacket;
import firearms.fire.ServerboundFirePayload;
import firearms.fire.ServerboundReloadPayload;
import firearms.item.ItemRegistration;
import firearms.combat.CombatRegistration;
import firearms.item.CreativeTabRegistration;
import firearms.trade.TradeRegistration;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The mod's server-and-common entrypoint: registers the components, the items, the data loaders
 * (FA-3), the bullet entity (FA-5), the attach smithing recipe serializer (FA-7), and the
 * villager trade path's merchant predicate (FA-13).
 * (FA-3), the bullet entity (FA-5), the attach smithing recipe serializer (FA-7) and, only in a
 * development environment, the {@code /firearms debug} command (FA-12).
 * (FA-3), the bullet entity (FA-5), and firing's own sounds and recoil packet type (FA-6).
 * (FA-3), the bullet entity (FA-5) and the attach smithing recipe serializer (FA-7).
 */
public final class Firearms implements ModInitializer {
    public static final String MOD_ID = "firearms";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ComponentRegistration.register();
        ItemRegistration.register();
        DataRegistration.register();
        CombatRegistration.register();
        FireSounds.register();
        RecoilPacket.register();
        ServerboundFirePayload.register();
        ServerboundReloadPayload.register();
        FireNetworking.register();
        AttachRegistration.register();
        TradeRegistration.register();
        CreativeTabRegistration.register();
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) DebugCommand.register();
        LOGGER.info("Firearms ready beside Create Fly");
    }
}
