package dev.simulated_team.simulated.content.blocks.auger_shaft;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import com.zurrtum.create.client.content.kinetics.base.KineticBlockEntityRenderer;
import com.zurrtum.create.client.flywheel.api.visualization.VisualizationManager;
import com.zurrtum.create.client.flywheel.lib.transform.TransformStack;
import dev.simulated_team.simulated.index.SimPartialModels;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class AugerShaftRenderer extends KineticBlockEntityRenderer<AugerShaftBlockEntity, AugerShaftRenderer.AugerShaftRenderState> {

    public AugerShaftRenderer(final BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public AugerShaftRenderState createRenderState() {
        return new AugerShaftRenderState();
    }

    @Override
    public void extractRenderState(final AugerShaftBlockEntity be, final AugerShaftRenderState state, final float partialTicks,
                                   final Vec3 cameraPos, @Nullable final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        final Level level = be.getLevel();
        state.support = VisualizationManager.supportsVisualization(level);
        this.updateBaseRenderState(be, state, level, crumblingOverlay);

        if (!state.support) {
            state.model = this.getRotatedModel(be, state);
            state.angle = getAngleForBe(be, state.blockPos, state.axis);
        } else {
            state.model = null;
        }

        // the redstone indicators of the auger cog are drawn here even with flywheel
        if (be.getBlockState().getBlock() instanceof AugerCogBlock) {
            final Direction facing = Direction.get(Direction.AxisDirection.POSITIVE, state.blockState.getValue(AugerShaftBlock.AXIS));
            state.facing = facing;
            state.redstone = new SuperByteBuffer[2];
            for (int i = 0; i < 2; i++) {
                state.redstone[i] = CachedBuffers.partialFacing(be.flowDirection == (i == 1 ? facing.getOpposite() : facing) && be.getSpeed() != 0 ? SimPartialModels.AUGER_REDSTONE_ON : SimPartialModels.AUGER_REDSTONE_OFF, state.blockState, facing);
            }
        } else {
            state.redstone = null;
        }
    }

    @Override
    public void submit(final AugerShaftRenderState state, final PoseStack matrices, final SubmitNodeCollector queue, final CameraRenderState cameraState) {
        if (state.model == null && state.redstone == null) {
            return;
        }
        queue.submitCustomGeometry(matrices, state.layer, state);
    }

    @Override
    protected RenderType getRenderType(final AugerShaftBlockEntity be, final BlockState state) {
        return RenderTypes.solidMovingBlock();
    }

    @Override
    protected SuperByteBuffer getRotatedModel(final AugerShaftBlockEntity be, final AugerShaftRenderState state) {
        if (!(be.getBlockState().getBlock() instanceof AugerCogBlock)) {
            return super.getRotatedModel(be, state);
        }
        final Direction facing = Direction.get(Direction.AxisDirection.POSITIVE, state.blockState.getValue(AugerShaftBlock.AXIS));
        return CachedBuffers.partialDirectional(
                SimPartialModels.AUGER_COG, state.blockState,
                facing, () -> {
                    final PoseStack poseStack = new PoseStack();
                    TransformStack.of(poseStack)
                            .center()
                            .rotateToFace(facing)
                            .rotate(Axis.XN.rotationDegrees(90))
                            .uncenter();
                    return poseStack;
                });
    }

    @Override
    protected BlockState getRenderedBlockState(final AugerShaftBlockEntity be) {
        return shaft(getRotationAxisOf(be));
    }

    public static class AugerShaftRenderState extends KineticRenderState {
        public SuperByteBuffer @Nullable [] redstone;
        public Direction facing;

        @Override
        public void render(final PoseStack.Pose matricesEntry, final VertexConsumer vertexConsumer) {
            if (this.model != null) {
                super.render(matricesEntry, vertexConsumer);
            }

            if (this.redstone != null) {
                for (int i = 0; i < 2; i++) {
                    final SuperByteBuffer redstone = this.redstone[i];
                    redstone.center()
                            .rotateToFace(this.facing)
                            .rotate(Axis.XN.rotationDegrees((this.facing.getAxis().isHorizontal() ? 90 : 0) + i * 180))
                            .uncenter();

                    redstone.light(this.lightCoords)
                            .renderInto(matricesEntry, vertexConsumer);
                }
            }
        }
    }
}
