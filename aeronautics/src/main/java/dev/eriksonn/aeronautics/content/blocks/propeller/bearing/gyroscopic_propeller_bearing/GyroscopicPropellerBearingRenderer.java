package dev.eriksonn.aeronautics.content.blocks.propeller.bearing.gyroscopic_propeller_bearing;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zurrtum.create.catnip.math.AngleHelper;
import com.zurrtum.create.catnip.math.VecHelper;
import com.zurrtum.create.client.AllPartialModels;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import com.zurrtum.create.client.content.kinetics.base.KineticBlockEntityRenderer;
import com.zurrtum.create.content.contraptions.bearing.BearingBlock;
import dev.eriksonn.aeronautics.index.AeroPartialModels;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

public class GyroscopicPropellerBearingRenderer extends KineticBlockEntityRenderer<GyroscopicPropellerBearingBlockEntity, GyroscopicPropellerBearingRenderer.GyroBearingRenderState> {

    public GyroscopicPropellerBearingRenderer(final BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public GyroBearingRenderState createRenderState() {
        return new GyroBearingRenderState();
    }

    @Override
    public void extractRenderState(final GyroscopicPropellerBearingBlockEntity be, final GyroBearingRenderState state, final float partialTicks, final Vec3 cameraPos,
                                   @Nullable final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        // also skips everything when Flywheel visualizes the block entity
        super.extractRenderState(be, state, partialTicks, cameraPos, crumblingOverlay);
        if (state.support) {
            return;
        }

        final BlockState blockState = state.blockState;
        state.facing = blockState.getValue(BlockStateProperties.FACING);
        state.tiltQuat.set(be.previousTiltQuat).slerp(be.tiltQuat, partialTicks);
        state.top = CachedBuffers.partial(AeroPartialModels.BEARING_PLATE_METAL, blockState);
        state.topAngle = (float) (be.getInterpolatedAngle(partialTicks - 1) / 180 * Math.PI);
        state.pistonHead = CachedBuffers.partial(AeroPartialModels.GYRO_BEARING_PISTON_HEAD, blockState);
        state.pistonPole = CachedBuffers.partial(AeroPartialModels.GYRO_BEARING_PISTON_POLE, blockState);
    }

    @Override
    public void submit(final GyroBearingRenderState state, final PoseStack matrices, final SubmitNodeCollector queue, final CameraRenderState cameraState) {
        super.submit(state, matrices, queue, cameraState);
        if (state.support) {
            return;
        }

        // the plate and the pistons always went into RenderType.solid() upstream
        queue.submitCustomGeometry(matrices, RenderTypes.solidMovingBlock(), state::renderTopAndPistons);
    }

    @Override
    protected SuperByteBuffer getRotatedModel(final GyroscopicPropellerBearingBlockEntity be, final GyroBearingRenderState state) {
        return CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state.blockState, state.blockState
                .getValue(BearingBlock.FACING)
                .getOpposite());
    }

    public static class GyroBearingRenderState extends KineticRenderState {
        public Direction facing;
        public final Quaternionf tiltQuat = new Quaternionf();
        public SuperByteBuffer top;
        public float topAngle;
        public SuperByteBuffer pistonHead;
        public SuperByteBuffer pistonPole;

        /**
         * Upstream {@code renderSafe} after the shaft: the tilted plate and the four pistons
         */
        public void renderTopAndPistons(final PoseStack.Pose pose, final VertexConsumer vb) {
            final Direction facing = this.facing;
            final Vec3 normal = new Vec3(facing.getStepX(), facing.getStepY(), facing.getStepZ());
            final Quaternionf tiltQuat = this.tiltQuat;
            final Quaternionf Q = new Quaternionf(tiltQuat);
            Q.conjugate();
            Q.mul(new Quaternionf((float) normal.x, (float) normal.y, (float) normal.z, 0f));
            Q.mul(tiltQuat);
            final Vec3 contraptionNormal = new Vec3(Q.x(), Q.y(), Q.z());

            final SuperByteBuffer superBuffer = this.top;

            superBuffer.translate(normal.scale(4 / 16f));
            superBuffer.rotateCentered(tiltQuat);
            superBuffer.translate(normal.scale(-4 / 16f));

            // upstream kineticRotationTransform(superBuffer, be, facing.getAxis(), angle, light)
            superBuffer.light(this.lightCoords);
            superBuffer.rotateCentered(this.topAngle, Direction.fromAxisAndDirection(facing.getAxis(), Direction.AxisDirection.POSITIVE));
            superBuffer.color(this.color);

            if (facing.getAxis()
                    .isHorizontal()) {
                superBuffer.rotateCentered(
                        AngleHelper.rad(AngleHelper.horizontalAngle(facing.getOpposite())), Direction.UP);
            }

            superBuffer.rotateCentered(AngleHelper.rad(-90 - AngleHelper.verticalAngle(facing)), Direction.EAST);
            superBuffer.renderInto(pose, vb);

            for (int i = 0; i < 4; i++) {

                final SuperByteBuffer headBuffer = this.pistonHead;
                final SuperByteBuffer poleBuffer = this.pistonPole;
                final Vec3 originalPos = VecHelper.rotate(new Vec3(5.9 / 16.0, 0, 0), -90 * i, Direction.Axis.Y);
                Vec3 translatedPos = originalPos;

                if (facing.getAxis().isHorizontal()) {

                    translatedPos = VecHelper.rotate(translatedPos, AngleHelper.horizontalAngle(facing), Direction.Axis.Z);
                    translatedPos = VecHelper.rotate(translatedPos, -90 + AngleHelper.verticalAngle(facing), Direction.Axis.X);
                }

                final double translateDistance = translatedPos.dot(contraptionNormal) / normal.dot(contraptionNormal);
                translatedPos = translatedPos.add(normal.scale(translateDistance + 3 / 16.0));

                headBuffer.translate(translatedPos);
                headBuffer.translate(0.5f, 0.5f, 0.5f);

                poleBuffer.translate(translatedPos);
                poleBuffer.translate(0.5f, 0.5f, 0.5f);

                headBuffer.rotate(tiltQuat);
                int j = i;
                if (facing == Direction.DOWN) {
                    if (i % 2 == 0) {
                        headBuffer.rotate(AngleHelper.rad(180), Direction.EAST);
                        poleBuffer.rotate(AngleHelper.rad(180), Direction.EAST);
                    } else {
                        headBuffer.rotate(AngleHelper.rad(180), Direction.SOUTH);
                        poleBuffer.rotate(AngleHelper.rad(180), Direction.SOUTH);
                    }
                }
                if (facing.getAxis().isHorizontal()) {

                    headBuffer.rotate(AngleHelper.rad(AngleHelper.horizontalAngle(facing.getOpposite())), Direction.UP);
                    poleBuffer.rotate(AngleHelper.rad(AngleHelper.horizontalAngle(facing.getOpposite())), Direction.UP);
                    headBuffer.rotate(AngleHelper.rad(-90 + AngleHelper.verticalAngle(facing)), Direction.EAST);
                    poleBuffer.rotate(AngleHelper.rad(-90 + AngleHelper.verticalAngle(facing)), Direction.EAST);
                    j = 2 - j;
                }

                poleBuffer.translate(0, 0.5f / 16.0, 0);

                headBuffer.rotate(AngleHelper.rad(-90 * j), Direction.UP);
                poleBuffer.rotate(AngleHelper.rad(-90 * j), Direction.UP);

                // renderInto resets the (shared) buffers' transforms, so each piston starts from scratch as upstream
                headBuffer.light(this.lightCoords).renderInto(pose, vb);
                poleBuffer.light(this.lightCoords).renderInto(pose, vb);
            }
        }
    }
}
