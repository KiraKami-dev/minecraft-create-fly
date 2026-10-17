package dev.simulated_team.simulated.content.blocks.rope.rope_winch;


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zurrtum.create.catnip.theme.Color;
import com.zurrtum.create.client.content.contraptions.pulley.AbstractPulleyRenderer;
import com.zurrtum.create.client.content.kinetics.base.KineticBlockEntityRenderer;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.RopeStrandRenderer;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.index.SimSpriteShifts;
import com.zurrtum.create.catnip.math.AngleHelper;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SpriteShiftEntry;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
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

import static com.zurrtum.create.content.kinetics.base.DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE;
import static com.zurrtum.create.content.kinetics.base.DirectionalKineticBlock.FACING;

/**
 * Upstream also called {@code FilteringRenderer.renderOnBlockEntity}; the winch has no filtering behaviour, so that was a
 * no-op and is gone.
 */
public class RopeWinchRenderer implements BlockEntityRenderer<RopeWinchBlockEntity, RopeWinchRenderer.RopeWinchRenderState> {

    public RopeWinchRenderer(final BlockEntityRendererProvider.Context context) {

    }

    private static SuperByteBuffer transform(final SuperByteBuffer buffer, final BlockState state, final boolean axisDirectionMatters) {
        final Direction facing = state.getValue(FACING);

        final float zRotLast =
                axisDirectionMatters && (state.getValue(AXIS_ALONG_FIRST_COORDINATE) ^ facing.getAxis() == Direction.Axis.Z) ? 90
                        : 0;
        final float yRot = AngleHelper.horizontalAngle(facing) + (state.getValue(AXIS_ALONG_FIRST_COORDINATE) || facing.getAxis()
                != Direction.Axis.Y ? 0.0f : 90.0f);
        final float zRot = facing == Direction.UP ? 270 : facing == Direction.DOWN ? 90 : 0;

        buffer.rotateCentered((float) ((zRot) / 180 * Math.PI), Direction.SOUTH);
        buffer.rotateCentered((float) ((yRot) / 180 * Math.PI), Direction.UP);
        buffer.rotateCentered((float) ((zRotLast) / 180 * Math.PI), Direction.SOUTH);
        return buffer;
    }

    @Override
    public RopeWinchRenderState createRenderState() {
        return new RopeWinchRenderState();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public boolean shouldRender(final RopeWinchBlockEntity pBlockEntity, final Vec3 pCameraPos) {
        return true;
    }

    @Override
    public void extractRenderState(final RopeWinchBlockEntity be, final RopeWinchRenderState state, final float partialTicks, final Vec3 cameraPos, @Nullable final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderState.extractBase(be, state, crumblingOverlay);

        final BlockState blockState = be.getBlockState();
        state.shaft = CachedBuffers.partial(SimPartialModels.ROPE_WINCH_SHAFT, blockState);

        // upstream KineticBlockEntityRenderer.kineticRotationTransform
        final Direction.Axis axis = KineticBlockEntityRenderer.getRotationAxisOf(be);
        state.shaftAngle = KineticBlockEntityRenderer.getAngleForBe(be, be.getBlockPos(), axis);
        state.shaftDirection = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
        state.shaftColor = KineticBlockEntityRenderer.getColor(be);

        state.ropeCoil = null;
        if (be.getRopeHolder().isAttached() || (be.isVirtual() && be.getRopeHolder().renderAttached)) {
            state.ropeCoil = CachedBuffers.partial(SimPartialModels.ROPE_WINCH_ROPE_COIL, blockState);

            final Direction facing = blockState.getValue(FACING);
            final float speed;

            if (facing == Direction.DOWN) {
                speed = facing.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? 1.0f : -1.0f;
            } else {
                speed = facing.getAxisDirection() == Direction.AxisDirection.NEGATIVE == (blockState.getValue(AXIS_ALONG_FIRST_COORDINATE)) ? 1.0f : -1.0f;
            }

            // upstream AbstractPulleyRenderer.scrollCoil, which skipped the shift entirely for a zero offset
            final float offset = be.clientAngle.getValue(partialTicks);
            state.coilShift = offset != 0 ? this.getCoilShift() : null;
            state.coilScroll = offset != 0 ? AbstractPulleyRenderer.getCoilVScroll(state.coilShift, offset, speed) : 0;
        }

        state.rope = RopeStrandRenderer.extract(be, be.getRopeHolder(), partialTicks);
    }

    @Override
    public void submit(final RopeWinchRenderState state, final PoseStack ms, final SubmitNodeCollector queue, final CameraRenderState cameraState) {
        queue.submitCustomGeometry(ms, RenderTypes.solidMovingBlock(), state);
        RopeStrandRenderer.submit(state.rope, ms, queue);
    }

    protected SpriteShiftEntry getCoilShift() {
        return SimSpriteShifts.ROPE_WINCH_COIL;
    }

    public static class RopeWinchRenderState extends BlockEntityRenderState implements SubmitNodeCollector.CustomGeometryRenderer {
        public SuperByteBuffer shaft;
        public float shaftAngle;
        public Direction shaftDirection;
        public Color shaftColor;
        @Nullable
        public SuperByteBuffer ropeCoil;
        @Nullable
        public SpriteShiftEntry coilShift;
        public float coilScroll;
        @Nullable
        public RopeStrandRenderer.RopeRenderState rope;

        @Override
        public void render(final PoseStack.Pose pose, final VertexConsumer vb) {
            this.shaft.light(this.lightCoords);
            this.shaft.rotateCentered(this.shaftAngle, this.shaftDirection);
            this.shaft.color(this.shaftColor);
            transform(this.shaft, this.blockState, true).renderInto(pose, vb);

            if (this.ropeCoil != null) {
                this.ropeCoil.light(this.lightCoords);

                if (this.coilShift != null) {
                    this.ropeCoil.shiftUVScrolling(this.coilShift, this.coilScroll);
                }

                transform(this.ropeCoil, this.blockState, true).renderInto(pose, vb);
            }
        }
    }
}
