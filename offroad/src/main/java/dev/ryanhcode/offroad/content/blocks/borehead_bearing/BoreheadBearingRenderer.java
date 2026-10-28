package dev.ryanhcode.offroad.content.blocks.borehead_bearing;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zurrtum.create.catnip.data.Iterate;
import com.zurrtum.create.catnip.math.AngleHelper;
import com.zurrtum.create.client.AllPartialModels;
import com.zurrtum.create.client.catnip.animation.AnimationTickHolder;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import com.zurrtum.create.client.content.kinetics.base.KineticBlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Fallback renderer when Flywheel is off ({@link BoreheadBearingVisual} otherwise): the two shaft halves along the
 * rotation axis and the bearing top turning with the drill. PORT(1.21.11): split into extract (render state) and draw,
 * like Create Fly's {@code BearingRenderer}.
 */
public class BoreheadBearingRenderer extends KineticBlockEntityRenderer<BoreheadBearingBlockEntity, BoreheadBearingRenderer.BoreheadBearingRenderState> {
    public BoreheadBearingRenderer(final BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public BoreheadBearingRenderState createRenderState() {
        return new BoreheadBearingRenderState();
    }

    @Override
    public void extractRenderState(final BoreheadBearingBlockEntity be, final BoreheadBearingRenderState state, final float partialTicks, final Vec3 cameraPos,
                                   @Nullable final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        super.extractRenderState(be, state, partialTicks, cameraPos, crumblingOverlay);
        if (state.support) {
            return;
        }

        final BlockState blockState = be.getBlockState();
        final float time = AnimationTickHolder.getRenderTime(be.getLevel());
        final Direction.Axis rotationAxis = getRotationAxisOf(be);
        final float offset = getRotationOffsetForPosition(be, be.getBlockPos(), rotationAxis);

        int i = 0;
        for (final Direction direction : Iterate.directionsInAxis(rotationAxis)) {
            float angle = 0;

            if (be.getSpeed() != 0) {
                angle = direction.getAxisDirection().getStep() * (time * be.getSpeed() * 3f / 10) % 360;
            }

            angle += offset;
            state.shafts[i] = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, blockState, direction);
            state.shaftAngles[i] = angle / 180f * (float) Math.PI;
            i++;
        }

        final Direction facing = blockState.getValue(BlockStateProperties.FACING);
        state.top = CachedBuffers.partial(AllPartialModels.BEARING_TOP, blockState);
        state.topDirection = Direction.fromAxisAndDirection(facing.getAxis(), Direction.AxisDirection.POSITIVE);
        state.topAngle = (float) (be.getInterpolatedAngle(partialTicks - 1) / 180 * Math.PI);
        state.upAngle = facing.getAxis().isHorizontal() ? AngleHelper.rad(AngleHelper.horizontalAngle(facing.getOpposite())) : Float.NaN;
        state.eastAngle = AngleHelper.rad(-90 - AngleHelper.verticalAngle(facing));
    }

    @Override
    protected RenderType getRenderType(final BoreheadBearingBlockEntity be, final BlockState state) {
        return RenderTypes.solidMovingBlock();
    }

    @Override
    protected BlockState getRenderedBlockState(final BoreheadBearingBlockEntity be) {
        return shaft(getRotationAxisOf(be));
    }

    public static class BoreheadBearingRenderState extends KineticRenderState {
        public final SuperByteBuffer[] shafts = new SuperByteBuffer[2];
        public final float[] shaftAngles = new float[2];
        public SuperByteBuffer top;
        public Direction topDirection;
        public float topAngle;
        public float upAngle;
        public float eastAngle;

        @Override
        public void render(final PoseStack.Pose matricesEntry, final VertexConsumer vertexConsumer) {
            for (int i = 0; i < this.shafts.length; i++) {
                final SuperByteBuffer shaft = this.shafts[i];
                shaft.light(this.lightCoords);
                shaft.rotateCentered(this.shaftAngles[i], this.direction);
                shaft.color(this.color);
                shaft.renderInto(matricesEntry, vertexConsumer);
            }

            this.top.light(this.lightCoords);
            this.top.rotateCentered(this.topAngle, this.topDirection);
            this.top.color(this.color);
            if (!Float.isNaN(this.upAngle)) {
                this.top.rotateCentered(this.upAngle, Direction.UP);
            }
            this.top.rotateCentered(this.eastAngle, Direction.EAST);
            this.top.renderInto(matricesEntry, vertexConsumer);
        }
    }
}
