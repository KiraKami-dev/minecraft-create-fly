package dev.simulated_team.simulated.content.blocks.portable_engine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zurrtum.create.catnip.math.AngleHelper;
import com.zurrtum.create.client.AllPartialModels;
import com.zurrtum.create.client.catnip.animation.AnimationTickHolder;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import com.zurrtum.create.client.content.kinetics.base.KineticBlockEntityRenderer;
import dev.simulated_team.simulated.index.SimPartialModels;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * The portable engine has no Flywheel visual: everything (including the half shaft) is drawn here.
 */
public class PortableEngineRenderer extends KineticBlockEntityRenderer<PortableEngineBlockEntity, PortableEngineRenderer.PortableEngineRenderState> {

    public PortableEngineRenderer(final BlockEntityRendererProvider.Context context) {
        super(context);
    }

    protected static float getHatchOpenProgress(final PortableEngineBlockEntity engine, final float partialTicks) {
        return Mth.sin(engine.getHatchOpenTime(partialTicks) / 10 * Mth.HALF_PI);
    }

    @Override
    public PortableEngineRenderState createRenderState() {
        return new PortableEngineRenderState();
    }

    @Override
    public void extractRenderState(final PortableEngineBlockEntity be, final PortableEngineRenderState state, final float partialTicks,
                                   final Vec3 cameraPos, @Nullable final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        final Level level = be.getLevel();
        state.support = false;
        this.updateBaseRenderState(be, state, level, crumblingOverlay);
        state.model = this.getRotatedModel(be, state);
        state.angle = getAngleForBe(be, state.blockPos, state.axis);

        state.virtual = be.isVirtual();
        state.facing = state.blockState.getValue(BlockStateProperties.HORIZONTAL_FACING);
        state.lit = state.blockState.getValue(RedstoneTorchBlock.LIT);
        state.superHeated = be.isSuperHeated();
        state.visualStrength = be.visualStrength.getValue(partialTicks);
        state.hatchOpenProgress = getHatchOpenProgress(be, partialTicks);
        state.renderTime = AnimationTickHolder.getRenderTime(level) / 20;
    }

    @Override
    public void submit(final PortableEngineRenderState state, final PoseStack ms, final SubmitNodeCollector queue, final CameraRenderState cameraState) {
        queue.submitCustomGeometry(ms, state.layer, state);

        final int light = state.lightCoords;
        SimPartialModels.EngineParts engineParts = SimPartialModels.ENGINE_PARTS;
        final SimPartialModels.EngineParts baseParts = engineParts;

        queue.submitCustomGeometry(ms, RenderTypes.cutoutMovingBlock(), (pose, cutout) -> {
            renderHatch(state, pose, light, cutout, 255, baseParts, !state.lit, false);
            renderPipes(state, pose, light, cutout, 255, baseParts, false);
        });

        final float hatchOpenProgress = 1.0f - state.hatchOpenProgress;
        if (state.visualStrength > 0) {
            engineParts = state.superHeated ? SimPartialModels.ENGINE_PARTS_SUPERHEATED : SimPartialModels.ENGINE_PARTS_HEATED;

            final SimPartialModels.EngineParts heatedParts = engineParts;
            queue.submitCustomGeometry(ms, RenderTypes.translucentMovingBlock(), (pose, translucent) ->
                    renderPipes(state, pose, LightTexture.FULL_BRIGHT, translucent, (int) (state.visualStrength * 255), heatedParts, true));
        }

        if (state.lit) {
            final SimPartialModels.EngineParts hatchParts = engineParts;
            queue.submitCustomGeometry(ms, RenderTypes.translucentMovingBlock(), (pose, translucent) ->
                    renderHatch(state, pose, LightTexture.FULL_BRIGHT, translucent, (int) (hatchOpenProgress * 255), hatchParts, true, true));
        }
    }

    private static void renderHatch(final PortableEngineRenderState state, final PoseStack.Pose ms, final int light, final VertexConsumer consumer, final int alpha, final SimPartialModels.EngineParts parts, final boolean renderInner, boolean lit) {
        if (state.virtual) lit = false;

        final BlockState blockState = state.blockState;
        final Direction direction = state.facing;

        final double hatchPivotY = 4.9f / 16.0f;
        final double hatchPivotZ = 3.7f / 16.0f;

        final float hatchOpenAmount = state.hatchOpenProgress * 0.65f;

        final SuperByteBuffer hatchBottom = rotateToFacing(CachedBuffers.partial(parts.hatchBottom, blockState), direction);
        if (lit) hatchBottom.disableDiffuse();
        hatchBottom
                .translate(0.0f, hatchPivotY, hatchPivotZ)
                .rotate(-hatchOpenAmount, Direction.EAST)
                .translate(-0.0f, -hatchPivotY, -hatchPivotZ)
                .light(light)
                .color(255, 255, 255, alpha)
                .renderInto(ms, consumer);

        final SuperByteBuffer hatchTop = rotateToFacing(CachedBuffers.partial(parts.hatchTop, blockState), direction);
        if (lit) hatchTop.disableDiffuse();
        hatchTop
                .light(light)
                .color(255, 255, 255, alpha)
                .renderInto(ms, consumer);

        if (renderInner) {
            final SuperByteBuffer mouth = rotateToFacing(CachedBuffers.partial(parts.mouth, blockState), direction.getOpposite());
            if (lit) mouth.disableDiffuse();
            mouth
                    .light(light)
                    .renderInto(ms, consumer);
        }
    }

    private static void renderPipes(final PortableEngineRenderState state, final PoseStack.Pose ms, final int light, final VertexConsumer consumer, final int alpha, final SimPartialModels.EngineParts parts, boolean lit) {
        final BlockState blockState = state.blockState;
        final Direction direction = state.facing;
        final float renderTime = state.renderTime;

        final double pulseTime = renderTime * 7.0;
        final double clipHeight = 0.65;
        final float pulseStrength = 0.03f * state.visualStrength;
        final float pipePulseStrength = pulseStrength * 1.1f;

        final float pipeScale = (float) (Math.max(Math.sin(pulseTime) + clipHeight, 0.0) - clipHeight) * pipePulseStrength + 1.0f;
        final float outletScale = (float) (Math.max(Math.sin(pulseTime - 1.15) + clipHeight, 0.0) - clipHeight) * pulseStrength + 1.0f;

        final Vector3f outletRotationPointLeft = new Vector3f(2.2f, 10.2f, 11.0f).div(16.0f);
        final Vector3f outletRotationPointRight = new Vector3f(13.6f, 10.2f, 11.0f).div(16.0f);

        final float outletRotation = (float) Math.toRadians(7.5);

        final Vector3f pipeCenterRight = new Vector3f(14.0f, 10.0f, 8.0f).div(16.0f);
        final Vector3f pipeCenterLeft = new Vector3f(16.0f - 14.0f, 10.0f, 8.0f).div(16.0f);

        if (state.virtual) lit = false;

        final SuperByteBuffer pipeRight = rotateToFacing(CachedBuffers.partial(parts.pipeRight, blockState), direction);
        if (lit) pipeRight.disableDiffuse();
        pipeRight
                .translate(pipeCenterRight)
                .scale(pipeScale)
                .translateBack(pipeCenterRight)
                .light(light)
                .color(255, 255, 255, alpha)
                .renderInto(ms, consumer);

        final SuperByteBuffer outletRight = rotateToFacing(CachedBuffers.partial(parts.outletRight, blockState), direction);
        if (lit) outletRight.disableDiffuse();
        outletRight
                .translate(pipeCenterRight)
                .scale(outletScale)
                .translateBack(pipeCenterRight)
                .translate(outletRotationPointRight)
                .rotateY(-outletRotation)
                .translateBack(outletRotationPointRight)
                .light(light)
                .color(255, 255, 255, alpha)
                .renderInto(ms, consumer);

        final SuperByteBuffer pipeLeft = rotateToFacing(CachedBuffers.partial(parts.pipeLeft, blockState), direction);
        if (lit) pipeLeft.disableDiffuse();
        pipeLeft
                .translate(pipeCenterLeft)
                .scale(pipeScale)
                .translateBack(pipeCenterLeft)
                .light(light)
                .color(255, 255, 255, alpha)
                .renderInto(ms, consumer);

        final SuperByteBuffer outletLeft = rotateToFacing(CachedBuffers.partial(parts.outletLeft, blockState), direction);
        if (lit) outletLeft.disableDiffuse();
        outletLeft
                .translate(pipeCenterLeft)
                .scale(outletScale)
                .translateBack(pipeCenterLeft)
                .translate(outletRotationPointLeft)
                .rotateY(outletRotation)
                .translateBack(outletRotationPointLeft)
                .light(light)
                .color(255, 255, 255, alpha)
                .renderInto(ms, consumer);
    }

    @Override
    protected SuperByteBuffer getRotatedModel(final PortableEngineBlockEntity te, final PortableEngineRenderState state) {
        return CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, te.getBlockState(), te.getBlockState()
                .getValue(BlockStateProperties.HORIZONTAL_FACING));
    }

    protected static SuperByteBuffer rotateToFacing(final SuperByteBuffer buffer, final Direction facing) {
        buffer.rotateCentered(AngleHelper.rad(AngleHelper.horizontalAngle(facing)), Direction.UP);
        return buffer;
    }

    public static class PortableEngineRenderState extends KineticRenderState {
        public boolean virtual;
        public Direction facing;
        public boolean lit;
        public boolean superHeated;
        public float visualStrength;
        public float hatchOpenProgress;
        public float renderTime;
    }
}
