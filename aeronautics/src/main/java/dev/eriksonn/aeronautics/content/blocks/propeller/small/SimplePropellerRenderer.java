package dev.eriksonn.aeronautics.content.blocks.propeller.small;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zurrtum.create.catnip.math.AngleHelper;
import com.zurrtum.create.catnip.theme.Color;
import com.zurrtum.create.client.AllPartialModels;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import com.zurrtum.create.client.content.kinetics.base.KineticBlockEntityRenderer;
import com.zurrtum.create.client.flywheel.lib.model.baked.PartialModel;
import com.zurrtum.create.content.contraptions.bearing.BearingBlock;
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

public abstract class SimplePropellerRenderer<T extends BasePropellerBlockEntity, S extends SimplePropellerRenderer.PropellerRenderState> extends KineticBlockEntityRenderer<T, S> {

    public SimplePropellerRenderer(final BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    @SuppressWarnings("unchecked")
    public S createRenderState() {
        return (S) new PropellerRenderState();
    }

    @Override
    public void extractRenderState(final T be, final S state, final float partialTicks, final Vec3 cameraPos,
                                   @Nullable final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        // also skips everything when Flywheel visualizes the block entity
        super.extractRenderState(be, state, partialTicks, cameraPos, crumblingOverlay);
        if (state.support) {
            return;
        }

        final Direction dir = state.blockState.getValue(BlockStateProperties.FACING);
        state.facing = dir;
        state.propeller = CachedBuffers.partialFacing(this.getCurrentModel(be), state.blockState);
        state.propellerAngle = this.getAngle(partialTicks, dir, be);
        state.propellerColor = getColor(be);
    }

    @Override
    public void submit(final S state, final PoseStack matrices, final SubmitNodeCollector queue, final CameraRenderState cameraState) {
        super.submit(state, matrices, queue, cameraState);
        if (state.support) {
            return;
        }

        queue.submitCustomGeometry(matrices, RenderTypes.solidMovingBlock(), state::renderPropeller);
    }

    public abstract PartialModel getCurrentModel(T be);

    public float getAngle(final float partialTicks, final Direction dir, final T be) {
        float angle = be.getPreviousAngle() * (1f - partialTicks) + be.getAngle() * partialTicks;

        angle = angle / 180f * (float) Math.PI;

        angle *= 2;

        return angle;
    }

    @Override
    protected SuperByteBuffer getRotatedModel(final T be, final S state) {
        final BlockState blockState = state.blockState;
        return CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, blockState, blockState
                .getValue(BearingBlock.FACING)
                .getOpposite());
    }

    public static class PropellerRenderState extends KineticRenderState {
        public Direction facing;
        public SuperByteBuffer propeller;
        public float propellerAngle;
        public Color propellerColor;

        /**
         * Upstream {@code kineticRotationTransform} with the propeller's own angle, then the facing transforms
         */
        public void renderPropeller(final PoseStack.Pose pose, final VertexConsumer vb) {
            final Direction dir = this.facing;
            final SuperByteBuffer propeller = this.propeller;

            propeller.light(this.lightCoords);
            propeller.rotateCentered(this.propellerAngle, Direction.fromAxisAndDirection(dir.getAxis(), Direction.AxisDirection.POSITIVE));
            propeller.color(this.propellerColor);

            if (dir.getAxis().isHorizontal()) {
                propeller.rotateCentered(AngleHelper.rad(AngleHelper.horizontalAngle(dir.getOpposite())), Direction.UP);
            }
            if (dir.getAxis().isVertical()) {
                propeller.rotateCentered(AngleHelper.rad(AngleHelper.verticalAngle(dir.getOpposite())), Direction.EAST);
            }

            propeller.translate(0, 0, -3 / 16f).rotateCentered(AngleHelper.rad(-90 - AngleHelper.verticalAngle(dir)), Direction.EAST);

            propeller.renderInto(pose, vb);
        }
    }
}
