package firearms.data;

import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.packs.PackType;

/**
 * Registers this mod's two data reload listeners (`FA-3`): {@link WeaponDataLoader} and {@link
 * AttachmentDataLoader}, both server-data listeners since neither needs anything client-only.
 */
public final class DataRegistration {

    public static void register() {
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new WeaponDataLoader());
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new AttachmentDataLoader());
    }

    private DataRegistration() {
    }
}
