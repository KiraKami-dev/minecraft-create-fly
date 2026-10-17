package dev.simulated_team.simulated.content.blocks.directional_gearshift;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zurrtum.create.client.catnip.animation.AnimationTickHolder;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import com.zurrtum.create.client.content.kinetics.base.KineticBlockEntityRenderer;
import com.zurrtum.create.client.content.kinetics.transmission.SplitShaftRenderer;
import com.zurrtum.create.client.flywheel.api.visualization.VisualizationManager;
import com.zurrtum.create.content.kinetics.transmission.SplitShaftBlockEntity;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.index.SimPartialModels;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Draws the barrel of the directional gearshift (always) and, without Flywheel, the two half shafts
 * (with Flywheel those come from SplitShaftVisual, so this renderer must not skip vanilla rendering).
 */
public class DirectionalGearshiftRenderer extends SplitShaftRenderer {
    public DirectionalGearshiftRenderer(final BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public SplitShaftRenderState createRenderState() {
        return new DirectionalGearshiftRenderState();
    }

    @Override
    public void extractRenderState(final SplitShaftBlockEntity be, final SplitShaftRenderState splitState, final float partialTicks,
                                   final Vec3 cameraPos, @Nullable final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        final DirectionalGearshiftRenderState state = (DirectionalGearshiftRenderState) splitState;

        state.renderShafts = !VisualizationManager.supportsVisualization(be.getLevel());
        if (state.renderShafts) {
            super.extractRenderState(be, state, partialTicks, cameraPos, crumblingOverlay);
        } else {
            BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
            state.layer = RenderTypes.solidMovingBlock();
            state.color = KineticBlockEntityRenderer.getColor(be);
        }

        final BlockState blockState = be.getBlockState();
        final Direction.Axis axis = SimBlocks.DIRECTIONAL_GEARSHIFT.getRotationAxis(blockState);

        final float time = AnimationTickHolder.getRenderTime(be.getLevel());
        float angle = (time * be.getSpeed() * 3f / 10) % 360;
        float shaftAngle = 0.0f;
        float modifier = 0.0f;
        float offset = 0.0f;

        if (be.hasSource() && !blockState.getValue(DirectionalGearshiftBlock.LEFT_POWERED) && blockState.getValue(DirectionalGearshiftBlock.RIGHT_POWERED)) {
            shaftAngle = angle;
        }

        if (be.hasSource() && blockState.getValue(DirectionalGearshiftBlock.LEFT_POWERED) && !blockState.getValue(DirectionalGearshiftBlock.RIGHT_POWERED)) {
            modifier = be.getRotationSpeedModifier(be.getSourceFacing().getOpposite());
            offset = KineticBlockEntityRenderer.getRotationOffsetForPosition(be, be.getBlockPos(), axis);
        }

        angle *= modifier;
        angle += offset;
        angle = angle / 180f * (float) Math.PI;
        shaftAngle = shaftAngle / 180f * (float) Math.PI;

        final Direction direction = blockState.getValue(DirectionalGearshiftBlock.FACING);

        state.barrelAngle = angle;
        state.barrelShaftAngle = shaftAngle;
        state.barrelAxisDirection = Direction.get(Direction.AxisDirection.POSITIVE, axis);
        state.facing = direction;
        state.vertical = axis.isVertical() || (direction.getAxis().isVertical() && !blockState.getValue(DirectionalGearshiftBlock.AXIS_ALONG_FIRST_COORDINATE));
        state.barrel = CachedBuffers.partial(SimPartialModels.DIRECTIONAL_GEARSHIFT_CENTER, blockState);
        state.barrelShaft = CachedBuffers.partial(SimPartialModels.DIRECTIONAL_GEARSHIFT_BARREL_SHAFT, blockState);
    }

    public static class DirectionalGearshiftRenderState extends SplitShaftRenderState {
        public boolean renderShafts;
        public SuperByteBuffer barrel;
        public SuperByteBuffer barrelShaft;
        public float barrelAngle;
        public float barrelShaftAngle;
        public Direction barrelAxisDirection;
        public Direction facing;
        public boolean vertical;

        private void kineticRotationTransform(final SuperByteBuffer buffer) {
            buffer.light(this.lightCoords);
            buffer.rotateCentered(this.barrelAngle, this.barrelAxisDirection);
            buffer.color(this.color);
        }

        @Override
        public void render(final PoseStack.Pose matricesEntry, final VertexConsumer vertexConsumer) {
            // the same cached buffer is used for both barrel shafts; renderInto resets its transforms
            this.kineticRotationTransform(this.barrelShaft);
            this.barrelShaft.center().rotateToFace(this.facing).uncenter();
            if (this.vertical) {
                this.barrelShaft.rotateZCenteredDegrees(90);
            }
            this.barrelShaft.rotateZCentered((float) Math.PI);
            this.barrelShaft.rotateYCentered(this.barrelShaftAngle);
            this.barrelShaft.light(this.lightCoords).renderInto(matricesEntry, vertexConsumer);

            this.kineticRotationTransform(this.barrelShaft);
            this.barrelShaft.center().rotateToFace(this.facing).uncenter();
            if (this.vertical) {
                this.barrelShaft.rotateZCenteredDegrees(90);
            }
            this.barrelShaft.rotateYCentered(this.barrelShaftAngle);
            this.barrelShaft.light(this.lightCoords).renderInto(matricesEntry, vertexConsumer);

            this.kineticRotationTransform(this.barrel);
            this.barrel.center().rotateToFace(this.facing).uncenter();

            if (this.vertical) {
                this.barrel.rotateZCenteredDegrees(90);
            }

            this.barrel.light(this.lightCoords).renderInto(matricesEntry, vertexConsumer);

            if (this.renderShafts) {
                super.render(matricesEntry, vertexConsumer);
            }
        }
    }
}
