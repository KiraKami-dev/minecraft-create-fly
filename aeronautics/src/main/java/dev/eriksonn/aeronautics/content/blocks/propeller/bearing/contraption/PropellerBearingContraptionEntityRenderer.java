package dev.eriksonn.aeronautics.content.blocks.propeller.bearing.contraption;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zurrtum.create.client.content.contraptions.render.ContraptionEntityRenderer;
import com.zurrtum.create.client.flywheel.lib.transform.TransformStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * Client-only. Renders the propeller bearing contraption without Flywheel (Create Fly moved the per-entity
 * {@code applyLocalTransforms} of upstream Create into the contraption renderers and visuals).
 */
public class PropellerBearingContraptionEntityRenderer extends ContraptionEntityRenderer<PropellerBearingContraptionEntity, PropellerBearingContraptionEntityRenderer.PropellerContraptionState> {

    public PropellerBearingContraptionEntityRenderer(final EntityRendererProvider.Context context) {
        super(context);
    }

    /**
     * Upstream {@code PropellerBearingContraptionEntity#applyLocalTransforms}
     */
    public static void applyLocalTransforms(final PropellerBearingContraptionEntity entity, final PoseStack poseStack, final float partialTicks) {
        applyLocalTransforms(poseStack, entity.getId(), entity.getAngle(partialTicks), entity.getRotationAxis(), entity.direction,
                entity.getInterpolatedQuat(partialTicks));
    }

    private static void applyLocalTransforms(final PoseStack poseStack, final int seed, final float angle, final Direction.Axis axis,
                                             final Direction direction, final Quaternionf tilt) {
        Vec3 normal = new Vec3(direction.getStepX(), direction.getStepY(), direction.getStepZ());
        normal = normal.scale(12 / 16.0);
        TransformStack.of(poseStack)
                .nudge(seed)
                .center()
                .translate(normal.scale(-1))
                .rotate(tilt)
                .translate(normal)
                .rotateDegrees(angle, axis)
                .uncenter();
    }

    @Override
    public PropellerContraptionState createRenderState() {
        return new PropellerContraptionState();
    }

    @Override
    public void extractRenderState(final PropellerBearingContraptionEntity entity, final PropellerContraptionState state, final float tickProgress) {
        state.seed = entity.getId();
        state.angle = entity.getAngle(tickProgress);
        state.axis = entity.getRotationAxis();
        state.direction = entity.direction;
        state.tilt.set(entity.getInterpolatedQuat(tickProgress));
        super.extractRenderState(entity, state, tickProgress);
    }

    @Override
    public void transform(final PropellerContraptionState state, final PoseStack matrixStack) {
        applyLocalTransforms(matrixStack, state.seed, state.angle, state.axis, state.direction, state.tilt);
    }

    public static class PropellerContraptionState extends AbstractContraptionState {
        int seed;
        float angle;
        Direction.Axis axis;
        Direction direction = Direction.UP;
        final Quaternionf tilt = new Quaternionf();
    }
}
