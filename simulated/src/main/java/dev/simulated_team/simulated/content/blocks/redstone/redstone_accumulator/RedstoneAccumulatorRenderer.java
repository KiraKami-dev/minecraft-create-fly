package dev.simulated_team.simulated.content.blocks.redstone.redstone_accumulator;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zurrtum.create.catnip.math.AngleHelper;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import com.zurrtum.create.client.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.index.SimPartialModels;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class RedstoneAccumulatorRenderer extends SmartBlockEntityRenderer<RedstoneAccumulatorBlockEntity, RedstoneAccumulatorRenderer.AccumulatorRenderState> {
    /**
     * Was a Veil shader ({@code pinwheel/shaders/program/redstone_accumulator/diode}), now a 1.21.11 core shader:
     * the vertex alpha blends between the world-lit left half of the diode sprite and its glowing right half.
     */
    public static final RenderPipeline DIODE_PIPELINE = RenderPipeline.builder(RenderPipelines.BLOCK_SNIPPET)
            .withLocation(Simulated.path("pipeline/redstone_accumulator_diode"))
            .withVertexShader(Simulated.path("core/redstone_accumulator_diode"))
            .withFragmentShader(Simulated.path("core/redstone_accumulator_diode"))
            .build();

    public static final RenderType DIODE_RENDER_TYPE = RenderType.create(
            Simulated.MOD_ID + ":redstone_accumulator_diode",
            RenderSetup.builder(DIODE_PIPELINE)
                    .useLightmap()
                    .withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS, RenderTypes.MOVING_BLOCK_SAMPLER)
                    .affectsCrumbling()
                    .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                    .createRenderSetup());

    public RedstoneAccumulatorRenderer(final BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public AccumulatorRenderState createRenderState() {
        return new AccumulatorRenderState();
    }

    @Override
    public void extractRenderState(final RedstoneAccumulatorBlockEntity be, final AccumulatorRenderState state, final float tickProgress,
                                   final Vec3 cameraPos, @Nullable final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        super.extractRenderState(be, state, tickProgress, cameraPos, crumblingOverlay);
        state.diode = CachedBuffers.partial(SimPartialModels.REDSTONE_ACCUMULATOR_DIODE, state.blockState);
        state.facing = state.blockState.getValue(RedstoneAccumulatorBlock.FACING);
        state.litAmount = this.getLitAmount(be, tickProgress);
    }

    @Override
    public void submit(final AccumulatorRenderState state, final PoseStack ms, final SubmitNodeCollector queue, final CameraRenderState cameraState) {
        super.submit(state, ms, queue, cameraState);
        queue.submitCustomGeometry(ms, DIODE_RENDER_TYPE, state);
    }

    private int getLitAmount(final RedstoneAccumulatorBlockEntity be, final float partialTicks) {
        float state = be.getLerpedState(partialTicks);
        // ^1.5 is for gamma correction, otherwise dark change is too quick and light change is barely noticeable
        state = 1 - (float) Math.pow(state / 15F, 1.5);
        return (int)Mth.clamp(state * 255, 0, 255);
    }

    public static class AccumulatorRenderState extends SmartRenderState implements SubmitNodeCollector.CustomGeometryRenderer {
        public SuperByteBuffer diode;
        public Direction facing;
        public int litAmount;

        @Override
        public void render(final PoseStack.Pose pose, final VertexConsumer vertexConsumer) {
            this.diode.color(255, 255, 255, this.litAmount);
            this.diode.light(this.lightCoords);
            this.diode.translate(0.5, 0, 0.5);
            this.diode.rotateYDegrees(AngleHelper.horizontalAngle(this.facing));
            this.diode.renderInto(pose, vertexConsumer);
        }
    }
}
