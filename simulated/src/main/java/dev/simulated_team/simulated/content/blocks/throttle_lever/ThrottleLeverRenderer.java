package dev.simulated_team.simulated.content.blocks.throttle_lever;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zurrtum.create.catnip.math.AngleHelper;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import com.zurrtum.create.client.flywheel.api.visualization.VisualizationManager;
import com.zurrtum.create.client.flywheel.lib.transform.PoseTransformStack;
import com.zurrtum.create.client.flywheel.lib.transform.TransformStack;
import com.zurrtum.create.content.redstone.analogLever.AnalogLeverBlock;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.util.SimColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * Draws the lever parts without Flywheel and, always, the outline of the handle when it is looked at
 * (so this renderer must not skip vanilla rendering when the visual is active).
 */
public class ThrottleLeverRenderer implements BlockEntityRenderer<ThrottleLeverBlockEntity, ThrottleLeverRenderer.ThrottleLeverRenderState> {

    protected static final double ANGLE_LIMIT = 40.0;
    // upstream drew the handle outline black at 40% alpha
    private static final int OUTLINE_COLOR = 0x66000000;

    public ThrottleLeverRenderer(final BlockEntityRendererProvider.Context context) {
    }

    public static void transformHandleExternal(final ThrottleLeverBlockEntity blockEntity, final float partialTicks, final PoseStack ms) {
        final float state = blockEntity.clientAngle.getValue(partialTicks);
        final AttachFace face = blockEntity.getBlockState().getValue(FaceAttachedHorizontalDirectionalBlock.FACE);
        float angle = (float) (((state / 15) * (ANGLE_LIMIT * 2) - ANGLE_LIMIT) / 180 * Math.PI);

        if (face == AttachFace.WALL) {
            angle = -angle;
        }

        final PoseTransformStack stack = TransformStack.of(ms);
        transform(stack, blockEntity.getBlockState());
        stack
                .translate(1 / 2f, 3.0 / 16.0, 1 / 2f)
                .rotateX(angle)
                .translateBack(1 / 2f, 3.0 / 16.0, 1 / 2f);
    }

    @Override
    public ThrottleLeverRenderState createRenderState() {
        return new ThrottleLeverRenderState();
    }

    @Override
    public void extractRenderState(final ThrottleLeverBlockEntity be, final ThrottleLeverRenderState state, final float partialTicks,
                                   final Vec3 cameraPos, @Nullable final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderState.extractBase(be, state, crumblingOverlay);

        final BlockState leverState = state.blockState;
        final float leverValue = be.clientAngle.getValue(partialTicks);
        final AttachFace face = leverState.getValue(FaceAttachedHorizontalDirectionalBlock.FACE);
        float angle = (float) (((leverValue / 15) * (ANGLE_LIMIT * 2) - ANGLE_LIMIT) / 180 * Math.PI);

        if (face == AttachFace.WALL) {
            angle = -angle;
        }

        state.angle = angle;
        state.face = face;

        state.renderParts = !VisualizationManager.supportsVisualization(be.getLevel());
        if (state.renderParts) {
            state.handle = CachedBuffers.partial(SimPartialModels.THROTTLE_LEVER_HANDLE, leverState);
            state.button = CachedBuffers.partial(SimPartialModels.THROTTLE_LEVER_BUTTON, leverState);
            state.diode = CachedBuffers.partial(SimPartialModels.THROTTLE_LEVER_DIODE, leverState);

            final float signalStrength = Math.max(0, be.state / 15F);
            state.diodeColor = SimColors.redstone(signalStrength);
            state.buttonAngle = be.clientPressedLerp.getValue(partialTicks) * -7f;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        state.outline = !be.isVirtual() && minecraft.hitResult instanceof final BlockHitResult hitResult && hitResult.getBlockPos().equals(be.getBlockPos());
        if (state.outline) {
            state.outlineShape = SimBlocks.THROTTLE_LEVER.getHandleShape(SimBlocks.THROTTLE_LEVER.defaultBlockState());
            state.outlineWidth = minecraft.getWindow().getAppropriateLineWidth();
        }
    }

    @Override
    public void submit(final ThrottleLeverRenderState state, final PoseStack ms, final SubmitNodeCollector queue, final CameraRenderState cameraState) {
        if (state.renderParts) {
            queue.submitCustomGeometry(ms, RenderTypes.cutoutMovingBlock(), state);
        }

        if (state.outline) {
            renderOutline(state, ms, queue);
        }
    }

    private static void renderOutline(final ThrottleLeverRenderState state, final PoseStack ms, final SubmitNodeCollector queue) {
        ms.pushPose();
        final PoseTransformStack stack = TransformStack.of(ms);
        transform(stack, state.blockState);
        stack
                .translate(1 / 2f, 3.0 / 16.0, 1 / 2f)
                .rotateX(state.angle)
                .translateBack(1 / 2f, 3.0 / 16.0, 1 / 2f);
        final VoxelShape leverShape = state.outlineShape;
        final float lineWidth = state.outlineWidth;
        // ShapeRenderer#renderShape, on the submitted pose (was LevelRenderer#renderShape via accessor)
        queue.submitCustomGeometry(ms, RenderTypes.lines(), (pose, consumer) -> leverShape.forAllEdges((x0, y0, z0, x1, y1, z1) -> {
            final Vector3f normal = new Vector3f((float) (x1 - x0), (float) (y1 - y0), (float) (z1 - z0)).normalize();
            consumer.addVertex(pose, (float) x0, (float) y0, (float) z0).setColor(OUTLINE_COLOR).setNormal(pose, normal).setLineWidth(lineWidth);
            consumer.addVertex(pose, (float) x1, (float) y1, (float) z1).setColor(OUTLINE_COLOR).setNormal(pose, normal).setLineWidth(lineWidth);
        }));
        ms.popPose();
    }

    private static <T extends TransformStack<T>> TransformStack<T> transformHandle(final TransformStack<T> buffer, final float angle, final AttachFace face) {
        return buffer
                .translate(1 / 2f, 3 / 16f, 1 / 2f)
                .rotateX(angle)
                .translateBack(1 / 2f, 3 / 16f, 1 / 2f)
                .rotateCentered(face == AttachFace.WALL ? (float) Math.PI : 0.0f, Direction.UP);
    }

    private static  <T extends TransformStack<T>> TransformStack<T> transform(final TransformStack<T> buffer, final BlockState leverState) {
        final AttachFace attached = leverState.getValue(AnalogLeverBlock.FACE);
        final Direction facing = leverState.getValue(AnalogLeverBlock.FACING);

        final float rX;
        switch (attached) {
            case FLOOR -> rX = 0;
            case WALL -> rX = 90;
            default -> rX = 180;
        }

        final float rY = AngleHelper.horizontalAngle(facing);
        buffer.rotateCentered((float) (rY / 180 * Math.PI), Direction.UP);
        buffer.rotateCentered((float) (rX / 180 * Math.PI), Direction.EAST);
        buffer.rotateCentered(attached == AttachFace.CEILING ? (float) Math.PI : 0.0f, Direction.UP);
        return buffer;
    }

    public static class ThrottleLeverRenderState extends BlockEntityRenderState implements SubmitNodeCollector.CustomGeometryRenderer {
        public float angle;
        public AttachFace face;
        public boolean renderParts;
        public SuperByteBuffer handle;
        public SuperByteBuffer button;
        public SuperByteBuffer diode;
        public int diodeColor;
        public float buttonAngle;
        public boolean outline;
        public VoxelShape outlineShape;
        public float outlineWidth;

        @Override
        public void render(final PoseStack.Pose matricesEntry, final VertexConsumer vb) {
            transform(this.handle, this.blockState);
            transform(this.button, this.blockState);
            transform(this.diode, this.blockState);

            transformHandle(this.handle, this.angle, this.face);
            this.handle
                    .light(this.lightCoords)
                    .renderInto(matricesEntry, vb);

            transformHandle(this.button, this.angle, this.face);
            this.button
                    .translate(0, 14 / 16f, 8 / 16f)
                    .rotateXDegrees(this.buttonAngle)
                    .translateBack(0, 14 / 16f, 8 / 16f)
                    .light(this.lightCoords)
                    .renderInto(matricesEntry, vb);

            this.diode.light(this.lightCoords)
                    .color(this.diodeColor)
                    .renderInto(matricesEntry, vb);
        }
    }
}
