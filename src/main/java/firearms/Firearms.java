package firearms;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The mod's server-and-common entrypoint. */
public final class Firearms implements ModInitializer {
    public static final String MOD_ID = "firearms";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Firearms ready beside Create Fly");
    }
}
