package dev.simulated_team.simulated.content.blocks.docking_connector;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zurrtum.create.client.flywheel.lib.transform.TransformStack;
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
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector2f;

public class DockingConnectorRenderer implements BlockEntityRenderer<DockingConnectorBlockEntity, DockingConnectorRenderer.DockingConnectorRenderState> {
    public DockingConnectorRenderer(final BlockEntityRendererProvider.Context context) {

    }

    @Override
    public DockingConnectorRenderState createRenderState() {
        return new DockingConnectorRenderState();
    }

    @Override
    public void extractRenderState(final DockingConnectorBlockEntity be, final DockingConnectorRenderState state, final float partialTicks, final Vec3 cameraPos, @Nullable final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderState.extractBase(be, state, crumblingOverlay);

        final BlockState blockState = be.getBlockState();
        state.direction = blockState.getValue(BlockStateProperties.FACING);
        state.extension = be.getExtensionDistance(partialTicks);
        state.rotation = be.getFeetRotation(partialTicks) * 90;

        state.piston1 = CachedBuffers.partial(SimPartialModels.DOCKING_CONNECTOR_MAIN_PISTON_BOTTOM, blockState);
        state.piston2 = CachedBuffers.partial(SimPartialModels.DOCKING_CONNECTOR_MAIN_PISTON_TOP, blockState);
        state.sidePiston1 = CachedBuffers.partial(SimPartialModels.DOCKING_CONNECTOR_SIDE_PISTON_BOTTOM, blockState);
        state.sidePiston2 = CachedBuffers.partial(SimPartialModels.DOCKING_CONNECTOR_SIDE_PISTON_TOP, blockState);
        state.foot = CachedBuffers.partial(SimPartialModels.DOCKING_CONNECTOR_FOOT, blockState);
    }

    @Override
    public void submit(final DockingConnectorRenderState state, final PoseStack ms, final SubmitNodeCollector queue, final CameraRenderState cameraState) {
        queue.submitCustomGeometry(ms, RenderTypes.cutoutMovingBlock(), state);
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    public static void rotateToFaceCentered(final PoseStack ms, final Direction facing) {
        TransformStack.of(ms)
                .center()
                .rotateYDegrees(AngleHelper.horizontalAngle(facing))
                .rotateXDegrees(AngleHelper.verticalAngle(facing) + 90)
                .uncenter();
    }

    private static Vector2f rotateVector2f(final Vector2f v, float angle) {
        angle = (float) Math.toRadians(angle);
        final float s = Mth.sin(angle);
        final float c = Mth.cos(angle);
        v.set(v.x * c + v.y * s, v.y * c - v.x * s);
        return v;
    }

    public static class DockingConnectorRenderState extends BlockEntityRenderState implements SubmitNodeCollector.CustomGeometryRenderer {
        public Direction direction;
        public float extension;
        public float rotation;
        public SuperByteBuffer piston1;
        public SuperByteBuffer piston2;
        public SuperByteBuffer sidePiston1;
        public SuperByteBuffer sidePiston2;
        public SuperByteBuffer foot;

        @Override
        public void render(final PoseStack.Pose pose, final VertexConsumer vb) {
            final int light = this.lightCoords;
            final float extension = this.extension;
            final float rotation = this.rotation;

            final PoseStack ms = new PoseStack();
            ms.last().set(pose);

            ms.pushPose();
            rotateToFaceCentered(ms, this.direction);
            this.piston1.translate(0, extension * 0.5, 0);
            this.piston2.translate(0, extension, 0);
            this.piston1.light(light).renderInto(ms.last(), vb);
            this.piston2.light(light).renderInto(ms.last(), vb);

            final Vector2f footAnchor = new Vector2f();
            final Vector2f sidePistonTopAnchor = new Vector2f();
            final Vector2f sidePistonBottomAnchor = new Vector2f();
            final Vector2f relativeAnchor = new Vector2f();

            footAnchor.set(-7.5f, 15.5f).div(16).add(0, extension);
            rotateVector2f(sidePistonTopAnchor.set(1.5f, -2.5f).div(16), rotation).add(footAnchor);
            sidePistonBottomAnchor.set(-6, 2).div(16).add(0, extension / 2);

            relativeAnchor.set(sidePistonTopAnchor).sub(sidePistonBottomAnchor);
            relativeAnchor.normalize();

            final Matrix4f rotationMatrix = new Matrix4f(
                    1, 0, 0, 0,
                    0, relativeAnchor.y, relativeAnchor.x, 0,
                    0, -relativeAnchor.x, relativeAnchor.y, 0,
                    0, 0, 0, 1
            );


            for (int i = 0; i < 4; i++) {
                ms.pushPose();
                ms.translate(0.5, 0, 0.5);
                TransformStack.of(ms).rotateYDegrees(i * 90);

                this.sidePiston1.translate(0, sidePistonBottomAnchor.y, sidePistonBottomAnchor.x);
                this.sidePiston2.translate(0, sidePistonTopAnchor.y, sidePistonTopAnchor.x);
                this.foot.translate(0, footAnchor.y, footAnchor.x);
                this.foot.rotateXDegrees(rotation);

                this.sidePiston1.mulPose(rotationMatrix);
                this.sidePiston2.mulPose(rotationMatrix);

                this.sidePiston1.light(light).renderInto(ms.last(), vb);
                this.sidePiston2.light(light).renderInto(ms.last(), vb);
                this.foot.light(light).renderInto(ms.last(), vb);
                ms.popPose();
            }

            ms.popPose();
        }
    }

}
