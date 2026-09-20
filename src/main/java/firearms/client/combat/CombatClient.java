package firearms.client.combat;

import firearms.combat.CombatRegistration;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/** Registers the bullet's tracer renderer for {@code firearms:bullet} (`04-architecture.md`). */
public final class CombatClient {
    private CombatClient() {
    }

    public static void register() {
        EntityRendererRegistry.register(CombatRegistration.BULLET, BulletRenderer::new);
    }
}
