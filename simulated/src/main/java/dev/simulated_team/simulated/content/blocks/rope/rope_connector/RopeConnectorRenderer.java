package dev.simulated_team.simulated.content.blocks.rope.rope_connector;


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zurrtum.create.AllBlocks;
import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.RopeStrandRenderer;
import dev.simulated_team.simulated.index.SimPartialModels;
import com.zurrtum.create.catnip.math.AngleHelper;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class RopeConnectorRenderer implements BlockEntityRenderer<RopeConnectorBlockEntity, RopeConnectorRenderer.RopeConnectorRenderState> {

    public RopeConnectorRenderer(final BlockEntityRendererProvider.Context context) {
    }

    @Override
    public RopeConnectorRenderState createRenderState() {
        return new RopeConnectorRenderState();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public boolean shouldRender(final RopeConnectorBlockEntity blockEntity, final Vec3 cameraPos) {
        return true;
    }

    @Override
    public void extractRenderState(final RopeConnectorBlockEntity be, final RopeConnectorRenderState state, final float partialTicks, final Vec3 cameraPos, @Nullable final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderState.extractBase(be, state, crumblingOverlay);

        state.rope = RopeStrandRenderer.extract(be, be.getRopeHolder(), partialTicks);
        state.knot = null;

        final RopeStrandHolderBehavior holder = be.getRopeHolder();

        if ((!holder.isAttached()) && (!be.isVirtual() || !be.getRopeHolder().renderAttached)) {
            return;
        }
        state.knot = CachedBuffers.partialFacing(SimPartialModels.ROPE_CONNECTOR_KNOT, AllBlocks.ROPE.defaultBlockState(), Direction.NORTH);

        final BlockPos blockPos = be.getBlockPos();
        final BlockState blockState = be.getBlockState();

        final Vec3 attachmentPoint = be.getVisualAttachmentPoint(blockPos, blockState);
        final Direction facing = blockState.getValue(RopeConnectorBlock.FACING);

        final boolean axisAlongFirstCoordinate = blockState.getValue(RopeConnectorBlock.AXIS_ALONG_FIRST_COORDINATE);

        state.zRotLast = (axisAlongFirstCoordinate ^ facing.getAxis() == Direction.Axis.Z) ? 90 : 0;
        state.yRot = AngleHelper.horizontalAngle(facing) + (axisAlongFirstCoordinate || facing.getAxis() != Direction.Axis.Y ? 0.0f : 90.0f);
        state.zRot = facing == Direction.UP ? 270 : facing == Direction.DOWN ? 90 : 0;
        state.knotOffset = attachmentPoint.subtract(blockPos.getCenter());
    }

    @Override
    public void submit(final RopeConnectorRenderState state, final PoseStack ms, final SubmitNodeCollector queue, final CameraRenderState cameraState) {
        RopeStrandRenderer.submit(state.rope, ms, queue);

        if (state.knot != null) {
            queue.submitCustomGeometry(ms, RenderTypes.solidMovingBlock(), state);
        }
    }

    public static class RopeConnectorRenderState extends BlockEntityRenderState implements SubmitNodeCollector.CustomGeometryRenderer {
        @Nullable
        public RopeStrandRenderer.RopeRenderState rope;
        @Nullable
        public SuperByteBuffer knot;
        public Vec3 knotOffset;
        public float zRotLast;
        public float yRot;
        public float zRot;

        @Override
        public void render(final PoseStack.Pose pose, final VertexConsumer vb) {
            final SuperByteBuffer knotBuffer = this.knot.light(this.lightCoords);

            knotBuffer.translate(this.knotOffset);
            knotBuffer.rotateCentered((float) ((this.zRot) / 180 * Math.PI), Direction.SOUTH);
            knotBuffer.rotateCentered((float) ((this.yRot) / 180 * Math.PI), Direction.UP);
            knotBuffer.rotateCentered((float) ((this.zRotLast) / 180 * Math.PI), Direction.SOUTH);

            knotBuffer.rotateCentered((float) (Math.PI / 2.0), Direction.UP);
            knotBuffer.renderInto(pose, vb);
        }
    }
}
