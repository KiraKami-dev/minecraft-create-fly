package dev.simulated_team.simulated.content.blocks.analog_transmission;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zurrtum.create.catnip.theme.Color;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import com.zurrtum.create.client.content.kinetics.base.KineticBlockEntityRenderer;
import com.zurrtum.create.content.kinetics.base.KineticBlockEntity;
import dev.simulated_team.simulated.index.SimPartialModels;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class AnalogTransmissionRenderer extends KineticBlockEntityRenderer<AnalogTransmissionBlockEntity, AnalogTransmissionRenderer.AnalogTransmissionRenderState> {

    public AnalogTransmissionRenderer(final BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public AnalogTransmissionRenderState createRenderState() {
        return new AnalogTransmissionRenderState();
    }

    @Override
    public void extractRenderState(final AnalogTransmissionBlockEntity be, final AnalogTransmissionRenderState state, final float partialTicks,
                                   final Vec3 cameraPos, @Nullable final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        // the shaft (rotating with the transmission itself) is the base kinetic model
        super.extractRenderState(be, state, partialTicks, cameraPos, crumblingOverlay);
        if (state.support) {
            return;
        }

        final BlockState blockState = be.getBlockState();
        final KineticBlockEntity cogwheel = be.getExtraKinetics();

        state.cog = CachedBuffers.partialFacingVertical(SimPartialModels.ANALOG_TRANSMISSION_COG, blockState, Direction.fromAxisAndDirection(blockState.getValue(AnalogTransmissionBlock.AXIS), Direction.AxisDirection.POSITIVE));
        state.cogAngle = getAngleForBe(cogwheel, state.blockPos, state.axis);
        state.cogColor = getColor(cogwheel);
    }

    @Override
    protected RenderType getRenderType(final AnalogTransmissionBlockEntity be, final BlockState state) {
        return RenderTypes.solidMovingBlock();
    }

    @Override
    protected BlockState getRenderedBlockState(final AnalogTransmissionBlockEntity be) {
        return shaft(getRotationAxisOf(be));
    }

    public static class AnalogTransmissionRenderState extends KineticRenderState {
        public SuperByteBuffer cog;
        public float cogAngle;
        public Color cogColor;

        @Override
        public void render(final PoseStack.Pose matricesEntry, final VertexConsumer vertexConsumer) {
            super.render(matricesEntry, vertexConsumer);

            this.cog.light(this.lightCoords);
            this.cog.rotateCentered(this.cogAngle, this.direction);
            this.cog.color(this.cogColor);
            this.cog.renderInto(matricesEntry, vertexConsumer);
        }
    }
}
