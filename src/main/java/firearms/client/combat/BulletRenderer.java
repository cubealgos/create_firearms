package firearms.client.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import firearms.combat.BulletEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;

/**
 * The bullet's tracer: a small stretched-quad cross along its facing, drawn with the same
 * untextured additive-glow pipeline a lightning bolt uses ({@link RenderTypes#lightning()}) —
 * no item, block or entity model asset needed, matching the ticket's "no model file needed."
 * Registered for {@code firearms:bullet} by {@link CombatClient}. Purely cosmetic: this class
 * reads nothing but the entity's own facing and pellet flag and writes nothing back
 * (`COMBAT-REQ-011`).
 */
public final class BulletRenderer extends EntityRenderer<BulletEntity, BulletRenderState> {
    private static final float TRACER_LENGTH = 0.6f;
    private static final float PELLET_TRACER_LENGTH = 0.35f;
    private static final float TRACER_WIDTH = 0.02f;
    private static final float COLOR_R = 1.0f;
    private static final float COLOR_G = 0.85f;
    private static final float COLOR_B = 0.5f;
    private static final float COLOR_A = 0.9f;

    public BulletRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public BulletRenderState createRenderState() {
        return new BulletRenderState();
    }

    @Override
    public void extractRenderState(BulletEntity entity, BulletRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.pellet = entity.isPellet();
    }

    @Override
    public void submit(BulletRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float length = state.pellet ? PELLET_TRACER_LENGTH : TRACER_LENGTH;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180f - state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));
        // Copied out now, not read lazily from poseStack, since submission and actual drawing
        // happen at different points in the new deferred submit pipeline.
        Matrix4f matrix = new Matrix4f(poseStack.last().pose());
        poseStack.popPose();

        collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, consumer) -> drawTracer(matrix, consumer, length));
    }

    private static void drawTracer(Matrix4f matrix, VertexConsumer consumer, float length) {
        float w = TRACER_WIDTH;
        // Two quads crossed at a right angle around the local -Z (forward) axis, so the tracer
        // reads from any horizontal viewing angle without a texture.
        quad(matrix, consumer, -w, 0, w, 0, length);
        quad(matrix, consumer, 0, -w, 0, w, length);
    }

    private static void quad(Matrix4f matrix, VertexConsumer consumer, float x0, float y0, float x1, float y1, float length) {
        consumer.addVertex(matrix, x0, y0, 0).setColor(COLOR_R, COLOR_G, COLOR_B, 0f);
        consumer.addVertex(matrix, x1, y1, 0).setColor(COLOR_R, COLOR_G, COLOR_B, 0f);
        consumer.addVertex(matrix, x1, y1, -length).setColor(COLOR_R, COLOR_G, COLOR_B, COLOR_A);
        consumer.addVertex(matrix, x0, y0, -length).setColor(COLOR_R, COLOR_G, COLOR_B, COLOR_A);
    }
}
