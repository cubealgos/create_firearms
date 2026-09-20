package firearms;

import firearms.attach.AttachRegistration;
import firearms.component.ComponentRegistration;
import firearms.data.DataRegistration;
import firearms.item.ItemRegistration;
import firearms.combat.CombatRegistration;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The mod's server-and-common entrypoint: registers the components, the items, the data loaders
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
        AttachRegistration.register();
        LOGGER.info("Firearms ready beside Create Fly");
    }
}
