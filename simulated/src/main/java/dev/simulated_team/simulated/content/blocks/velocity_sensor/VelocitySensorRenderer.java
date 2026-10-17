package dev.simulated_team.simulated.content.blocks.velocity_sensor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import dev.simulated_team.simulated.content.blocks.util.AbstractDirectionalAxisBlock;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.util.SimColors;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class VelocitySensorRenderer implements BlockEntityRenderer<VelocitySensorBlockEntity, VelocitySensorRenderer.VelocitySensorRenderState> {
    public VelocitySensorRenderer(final BlockEntityRendererProvider.Context context) {
    }

    @Override
    public VelocitySensorRenderState createRenderState() {
        return new VelocitySensorRenderState();
    }

    @Override
    public void extractRenderState(final VelocitySensorBlockEntity be, final VelocitySensorRenderState state, final float partialTicks,
                                   final Vec3 cameraPos, @Nullable final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
        final BlockState blockState = state.blockState;
        state.diode = CachedBuffers.partial(SimPartialModels.VELOCITY_SENSOR_DIODE, blockState);
        state.fan = CachedBuffers.partial(SimPartialModels.VELOCITY_SENSOR_FAN, blockState);

        boolean front = (blockState.getValue(VelocitySensorBlock.POWERED) == 1);
        // north + east always opposite
        // south + down second axis opposite, west first axis opposite
        final boolean axis = blockState.getValue(VelocitySensorBlock.AXIS_ALONG_FIRST_COORDINATE);
        front = switch (blockState.getValue(VelocitySensorBlock.FACING)) {
            case NORTH, EAST -> !front;
            case SOUTH, DOWN -> axis == front;
            case WEST -> axis != front;
            case UP -> front;
        };
        final float signalStrength = be.getRedstoneStrength() / 15F;
        final int color = SimColors.redstone(signalStrength);
        state.frontColor = front ? color : SimColors.REDSTONE_OFF;
        state.backColor = front ? SimColors.REDSTONE_OFF : color;
        state.fanAngle = be.getFanAngle(partialTicks);
        state.axisDirection = AbstractDirectionalAxisBlock.getDirectionOfAxis(blockState);
    }

    @Override
    public void submit(final VelocitySensorRenderState state, final PoseStack ms, final SubmitNodeCollector queue, final CameraRenderState cameraState) {
        queue.submitCustomGeometry(ms, RenderTypes.cutoutMovingBlock(), state);
    }

    private static void transform(final SuperByteBuffer diode, final BlockState state) {
        final Direction dir = state.getValue(VelocitySensorBlock.FACING);
        final boolean axis = state.getValue(VelocitySensorBlock.AXIS_ALONG_FIRST_COORDINATE);
        if (axis == (dir.getStepX() == 0)) {
            diode.rotateCenteredDegrees(90, state.getValue(VelocitySensorBlock.FACING));
        }

        diode.rotateCentered(dir.getRotation());
    }

    public static class VelocitySensorRenderState extends BlockEntityRenderState implements SubmitNodeCollector.CustomGeometryRenderer {
        public SuperByteBuffer diode;
        public SuperByteBuffer fan;
        public int frontColor;
        public int backColor;
        public float fanAngle;
        public Direction axisDirection;

        @Override
        public void render(final PoseStack.Pose pose, final VertexConsumer vb) {
            transform(this.diode, this.blockState);
            this.diode.light(this.lightCoords).color(this.frontColor).renderInto(pose, vb);

            transform(this.diode, this.blockState);
            this.diode.rotateCenteredDegrees(180, Direction.Axis.Y);
            this.diode.light(this.lightCoords).color(this.backColor).renderInto(pose, vb);

            transform(this.fan.rotateCentered(this.fanAngle, this.axisDirection), this.blockState);
            this.fan.light(this.lightCoords).renderInto(pose, vb);
        }
    }
}
