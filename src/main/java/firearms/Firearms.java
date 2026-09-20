package firearms;

import firearms.combat.CombatRegistration;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The mod's server-and-common entrypoint. */
public final class Firearms implements ModInitializer {
    public static final String MOD_ID = "firearms";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        CombatRegistration.register();
        LOGGER.info("Firearms ready beside Create Fly");
    }
}
