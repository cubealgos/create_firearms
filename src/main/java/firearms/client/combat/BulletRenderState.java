package firearms.client.combat;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * The bullet's per-frame render inputs, extracted once per frame from the entity
 * (`COMBAT-REQ-011`: cosmetic only, no authority of its own): facing, and whether it is a
 * shotgun pellet, drawn as a shorter tracer.
 */
public final class BulletRenderState extends EntityRenderState {
    public float yRot;
    public float xRot;
    public boolean pellet;
}
