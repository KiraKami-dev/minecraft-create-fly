package dev.eriksonn.aeronautics.content.blocks.hot_air.hot_air_burner;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import com.zurrtum.create.client.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.zurrtum.create.client.ponder.api.level.PonderLevel;
import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.index.AeroPartialModels;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.util.SimColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public class HotAirBurnerRenderer extends SmartBlockEntityRenderer<HotAirBurnerBlockEntity, HotAirBurnerRenderer.HotAirBurnerRenderState> {

    /**
     * Upstream: the Veil program {@code aeronautics:burner_flame} drawn immediately with the {@code FlameRenderTime},
     * {@code Intensity} and {@code Palette} uniforms. Now a core shader; the three values travel in the vertices so the
     * flames of all burners can share one batch: colour red = intensity, green = palette, UV2.x = render time modulo
     * {@link #FLAME_TIME_PERIOD} (the shader is periodic in it: circles move with time * 0.3 mod 1 and time * 1.2 mod 2).
     */
    public static final RenderPipeline FLAME_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(Aeronautics.path("pipeline/burner_flame"))
            .withVertexShader(Aeronautics.path("core/burner_flame"))
            .withFragmentShader(Aeronautics.path("core/burner_flame"))
            .withSampler("Sampler0")
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS)
            .withCull(false)
            .build();

    public static final RenderType FLAME_RENDER_TYPE = RenderType.create(
            Aeronautics.MOD_ID + ":burner_flame",
            RenderSetup.builder(FLAME_PIPELINE)
                    .withTexture("Sampler0", Aeronautics.path("textures/effects/fire_palette.png"))
                    .createRenderSetup());

    private static final float FLAME_TIME_PERIOD = 10.0f / 3.0f;

    public HotAirBurnerRenderer(final BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public HotAirBurnerRenderState createRenderState() {
        return new HotAirBurnerRenderState();
    }

    @Override
    public void extractRenderState(final HotAirBurnerBlockEntity be, final HotAirBurnerRenderState state, final float partialTicks, final Vec3 cameraPos,
                                   @Nullable final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        super.extractRenderState(be, state, partialTicks, cameraPos, crumblingOverlay);

        final float signalStrength = Math.max(0, be.getSignalStrength() / 15F);
        state.indicator = CachedBuffers.partial(AeroPartialModels.HOT_AIR_BURNER_INDICATOR, be.getBlockState());
        state.indicatorColor = SimColors.redstone(signalStrength);
        state.flame = signalStrength > 0.0;

        if (!state.flame) {
            return;
        }

        final BlockPos pos = be.getBlockPos();
        final Vec3 center = pos.getCenter();

        final Minecraft minecraft = Minecraft.getInstance();
        Vec3 camera = minecraft.gameRenderer.getMainCamera().position();

        if (be.getLevel() instanceof PonderLevel) {
            camera = minecraft.getCameraEntity().getPosition(partialTicks);
        }

        final SubLevel sublevel = Sable.HELPER.getContaining(be);
        if (sublevel != null) {
            camera = sublevel.logicalPose().transformPositionInverse(camera);
        }

        state.flameAngle = (float) Math.atan2(camera.z() - center.z(), camera.x() - center.x());

        final HotAirBurnerBlock.Variant variant = be.getBlockState().getValue(HotAirBurnerBlock.VARIANT);
        state.palette = variant == HotAirBurnerBlock.Variant.FIRE ? 0.25f : 0.75f;
        state.flameRenderTime = (float) Mth.lerp(partialTicks, be.lastRenderTime, be.renderTime) + be.getTimeOffset();
        state.intensity = be.getFlameIntensity(partialTicks);
    }

    @Override
    public void submit(final HotAirBurnerRenderState state, final PoseStack ms, final SubmitNodeCollector queue, final CameraRenderState cameraState) {
        queue.submitCustomGeometry(ms, RenderTypes.cutoutMovingBlock(), state::renderIndicator);

        if (state.flame) {
            ms.pushPose();
            ms.translate(-0.5, 0.35, 0.5);
            ms.rotateAround(Axis.YP.rotation((float) (-state.flameAngle + Math.PI * 0.5f)), 1.0f, 0.0f, 0.0f);
            queue.submitCustomGeometry(ms, FLAME_RENDER_TYPE, state::renderFlame);
            ms.popPose();
        }

        super.submit(state, ms, queue, cameraState);
    }

    public static class HotAirBurnerRenderState extends SmartRenderState {
        public SuperByteBuffer indicator;
        public int indicatorColor;
        public boolean flame;
        public float flameAngle;
        public float flameRenderTime;
        public float intensity;
        public float palette;

        public void renderIndicator(final PoseStack.Pose pose, final VertexConsumer vb) {
            this.indicator.light(this.lightCoords)
                    .color(this.indicatorColor)
                    .renderInto(pose, vb);
        }

        /**
         * Upstream {@code renderFlame}: a 2x2 quad from the local origin
         */
        public void renderFlame(final PoseStack.Pose pose, final VertexConsumer builder) {
            final float size = 2.0f;

            final float time = Mth.positiveModulo(this.flameRenderTime, FLAME_TIME_PERIOD) / FLAME_TIME_PERIOD;
            final int encodedTime = Mth.clamp(Math.round(time * 32767), 0, 32767);
            final int color = ARGB.colorFromFloat(1.0f, Mth.clamp(this.intensity, 0.0f, 1.0f), this.palette, 0.0f);

            final Matrix4f matrix = pose.pose();
            builder.addVertex(matrix, 0.0f, 0.0f, 0.0f).setColor(color).setUv(0.0f, 1.0f).setUv2(encodedTime, 0);
            builder.addVertex(matrix, size, 0.0f, 0.0f).setColor(color).setUv(1.0f, 1.0f).setUv2(encodedTime, 0);
            builder.addVertex(matrix, size, size, 0.0f).setColor(color).setUv(1.0f, 0.0f).setUv2(encodedTime, 0);
            builder.addVertex(matrix, 0.0f, size, 0.0f).setColor(color).setUv(0.0f, 0.0f).setUv2(encodedTime, 0);
        }
    }
}
