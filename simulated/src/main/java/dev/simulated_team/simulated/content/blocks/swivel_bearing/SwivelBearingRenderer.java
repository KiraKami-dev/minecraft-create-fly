package dev.simulated_team.simulated.content.blocks.swivel_bearing;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zurrtum.create.catnip.theme.Color;
import com.zurrtum.create.client.content.kinetics.base.KineticBlockEntityRenderer;
import com.zurrtum.create.content.kinetics.base.KineticBlockEntity;
import dev.simulated_team.simulated.index.SimPartialModels;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class SwivelBearingRenderer extends KineticBlockEntityRenderer<SwivelBearingBlockEntity, SwivelBearingRenderer.SwivelBearingRenderState> {

    public SwivelBearingRenderer(final BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public SwivelBearingRenderState createRenderState() {
        return new SwivelBearingRenderState();
    }

    @Override
    public void extractRenderState(final SwivelBearingBlockEntity be, final SwivelBearingRenderState state, final float partialTicks, final Vec3 cameraPos, @Nullable final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        // also skips everything when Flywheel visualizes the block entity (SwivelBearingVisual)
        super.extractRenderState(be, state, partialTicks, cameraPos, crumblingOverlay);
        if (state.support) {
            return;
        }

        final BlockState blockState = state.blockState;
        final Direction.Axis axis = state.axis;
        final KineticBlockEntity cogwheelBE = be.getExtraKinetics();

        state.cogwheel = CachedBuffers.partialFacingVertical(SimPartialModels.SWIVEL_BEARING_COG, blockState, blockState.getValue(SwivelBearingBlock.FACING).getOpposite());
        state.cogwheelAngle = getAngleForBe(cogwheelBE, be.getBlockPos(), axis);
        state.cogwheelColor = getColor(cogwheelBE);

        state.topShaft = !be.isAssembled() ? CachedBuffers.partialFacing(SimPartialModels.SHAFT_SIXTEENTH, blockState, blockState.getValue(SwivelBearingBlock.FACING)) : null;
    }

    @Override
    public void submit(final SwivelBearingRenderState state, final PoseStack matrices, final SubmitNodeCollector queue, final CameraRenderState cameraState) {
        super.submit(state, matrices, queue, cameraState);
        if (state.support) {
            return;
        }

        // the cogwheel and the top shaft always went into RenderType.solid() upstream
        queue.submitCustomGeometry(matrices, RenderTypes.solidMovingBlock(), state::renderSolidParts);
    }

    @Override
    protected SuperByteBuffer getRotatedModel(final SwivelBearingBlockEntity be, final SwivelBearingRenderState state) {
        return CachedBuffers.partialFacing(SimPartialModels.SHAFT_SIXTEENTH, state.blockState, state.blockState.getValue(SwivelBearingBlock.FACING).getOpposite());
    }

    public static class SwivelBearingRenderState extends KineticRenderState {
        public SuperByteBuffer cogwheel;
        public float cogwheelAngle;
        public Color cogwheelColor;
        @Nullable
        public SuperByteBuffer topShaft;

        /**
         * Upstream {@code kineticRotationTransform} on the cogwheel (with the extra-kinetics cog's angle and colour), and
         * {@code renderRotatingBuffer} on the top shaft (with the bearing's own)
         */
        public void renderSolidParts(final PoseStack.Pose pose, final VertexConsumer vb) {
            this.cogwheel.light(this.lightCoords);
            this.cogwheel.rotateCentered(this.cogwheelAngle, this.direction);
            this.cogwheel.color(this.cogwheelColor);
            this.cogwheel.renderInto(pose, vb);

            if (this.topShaft != null) {
                this.topShaft.light(this.lightCoords);
                this.topShaft.rotateCentered(this.angle, this.direction);
                this.topShaft.color(this.color);
                this.topShaft.renderInto(pose, vb);
            }
        }
    }
}
