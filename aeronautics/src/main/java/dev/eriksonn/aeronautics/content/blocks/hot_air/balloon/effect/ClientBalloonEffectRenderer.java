package dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.effect;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.config.AeroConfig;
import dev.eriksonn.aeronautics.content.blocks.hot_air.BlockEntityLiftingGasProvider;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.Balloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.ClientBalloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.map.BalloonMap;
import dev.ryanhcode.sable.veillite.api.event.VeilRenderLevelStageEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DynamicUniformStorage;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Renders the heated air inside balloons: the inside faces of every balloon volume are drawn into an offscreen target
 * with scrolling heat textures, which is then soft-light blended onto the scene where it is not occluded.
 * <p>
 * Upstream: a Veil {@code AdvancedFbo} + {@code aeronautics:hot_air_overlay} shader at {@code AFTER_SOLID_BLOCKS}, and the
 * Veil post pipeline {@code aeronautics:soft_light}. Here: {@link TextureTarget}s and blaze3d pipelines, drawn right
 * after the opaque section layers (hooked through Sable's sub-level dispatcher, see
 * {@code mixin/levitite/VanillaSubLevelRenderDispatcherMixin}).
 */
public class ClientBalloonEffectRenderer {

    private static final Identifier SIDE_TEXTURE = Aeronautics.path("textures/special/heat_overlay.png");
    private static final Identifier TOP_TEXTURE = Aeronautics.path("textures/special/lava_still.png");

    private static final String HOT_AIR_UNIFORM = "HotAirInfo";
    private static final int HOT_AIR_UBO_SIZE = new Std140SizeCalculator().putVec4().get();

    /**
     * Inside faces of balloons (upstream culled front faces; the mesh is wound the other way round instead), pushed in
     * front of the blocks by a depth bias (upstream: polygon offset -0.5, -30).
     */
    public static final RenderPipeline HOT_AIR_OVERLAY = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withLocation(Aeronautics.path("pipeline/hot_air_overlay"))
            .withVertexShader(Aeronautics.path("core/hot_air_overlay"))
            .withFragmentShader(Aeronautics.path("core/hot_air_overlay"))
            .withUniform(HOT_AIR_UNIFORM, UniformType.UNIFORM_BUFFER)
            .withSampler("Sampler0")
            .withSampler("Sampler1")
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL, VertexFormat.Mode.QUADS)
            .withDepthBias(-0.5f, -30.0f)
            .build();

    public static final RenderPipeline SOFT_LIGHT = RenderPipeline.builder()
            .withLocation(Aeronautics.path("pipeline/soft_light"))
            .withVertexShader("core/screenquad")
            .withFragmentShader(Aeronautics.path("core/soft_light"))
            .withSampler("MainSampler")
            .withSampler("MainDepthSampler")
            .withSampler("OverlaySampler")
            .withSampler("OverlayDepthSampler")
            .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .build();

    @Nullable
    private static TextureTarget overlayTarget;
    @Nullable
    private static TextureTarget sceneCopy;
    @Nullable
    private static DynamicUniformStorage<HotAirUniform> uniforms;
    private static final Matrix4f modelView = new Matrix4f();

    /**
     * @deprecated veillite never fires {@code VeilRenderLevelStageEvent}: the effect is drawn from
     * {@code mixin/levitite/VanillaSubLevelRenderDispatcherMixin} ({@link #render}). Kept so the upstream event
     * registration in {@code AeronauticsClient} still compiles; it can be removed there.
     */
    @Deprecated
    public static void onRenderLevelStage(final VeilRenderLevelStageEvent.Stage stage,
                                          final Matrix4fc frustumMatrix,
                                          final Matrix4fc projectionMatrix,
                                          final int renderTick) {
    }

    /**
     * Stores the world model view matrix (camera rotation) for this frame.
     */
    public static void setModelView(final Matrix4fc matrix) {
        modelView.set(matrix);
    }

    /**
     * Renders the balloon effects, right after the opaque terrain (upstream {@code AFTER_SOLID_BLOCKS}).
     *
     * @param mainTarget The target the opaque terrain was drawn into
     */
    public static void render(final RenderTarget mainTarget) {
        if (!AeroConfig.client().enableHotAirRendering.get()) {
            free();
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        if (level == null) {
            free();
            return;
        }

        final BalloonMap ballonMap = BalloonMap.MAP.get(level);
        if (ballonMap.isEmpty()) {
            free();
            return;
        }

        final int width = mainTarget.width;
        final int height = mainTarget.height;
        if (overlayTarget == null || overlayTarget.width != width || overlayTarget.height != height) {
            freeTargets();
            overlayTarget = new TextureTarget("Aeronautics hot air overlay", width, height, true);
            sceneCopy = new TextureTarget("Aeronautics hot air scene copy", width, height, false);
        }

        if (uniforms == null) {
            uniforms = new DynamicUniformStorage<>("Aeronautics hot air UBO", HOT_AIR_UBO_SIZE, 8);
        } else {
            uniforms.endFrame();
        }

        renderBalloonEffects(ballonMap, mainTarget, level.getGameTime());
    }

    /**
     * Renders the balloon effects for a given balloon map
     *
     * @param balloonMap the balloon map to render effects for
     * @param mainTarget the target to blend the effects onto
     * @param renderTick the render-tick
     */
    private static void renderBalloonEffects(final BalloonMap balloonMap, final RenderTarget mainTarget, final long renderTick) {
        final Minecraft minecraft = Minecraft.getInstance();
        final float partialTicks = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        final CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();

        encoder.clearColorAndDepthTextures(overlayTarget.getColorTexture(), 0, overlayTarget.getDepthTexture(), 1.0);

        final float scrollAmount = ((renderTick % 100000L) + partialTicks) / -20.0f;
        final float scroll = (float) (Math.floor(scrollAmount * 16.0f) / 16.0f);

        final AbstractTexture side = minecraft.getTextureManager().getTexture(SIDE_TEXTURE);
        final AbstractTexture top = minecraft.getTextureManager().getTexture(TOP_TEXTURE);
        final RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        boolean drawn = false;

        try (final RenderPass renderPass = encoder.createRenderPass(() -> "Aeronautics hot air overlay",
                overlayTarget.getColorTextureView(), OptionalInt.empty(), overlayTarget.getDepthTextureView(), OptionalDouble.empty())) {
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setPipeline(HOT_AIR_OVERLAY);
            renderPass.bindTexture("Sampler0", side.getTextureView(), side.getSampler());
            renderPass.bindTexture("Sampler1", top.getTextureView(), top.getSampler());

            for (final Balloon balloon : balloonMap.getBalloons()) {
                final ClientBalloon clientBalloon = (ClientBalloon) balloon;

                final HeatedCulledRenderRegion renderRegion = clientBalloon.getRenderRegion();

                if (renderRegion == null) {
                    continue;
                }

                float filledPercent = 0.0f;

                // These should all have the same client predicted volume, but we pick the max
                // to be safe
                for (final BlockEntityLiftingGasProvider heater : balloon.getHeaters()) {
                    filledPercent = Math.max(filledPercent, (float) heater.getClientPredictedVolume() / balloon.getCapacity());
                }

                filledPercent = Mth.clamp(filledPercent, 0.0f, 1.0f);
                final GpuBufferSlice uniform = uniforms.writeUniform(new HotAirUniform(scroll, (1.0f - filledPercent) * (balloon.getHeight() + 1.0f)));
                renderPass.setUniform(HOT_AIR_UNIFORM, uniform);

                drawn |= renderRegion.render(renderPass, modelView, indices) > 0;
            }
        }

        if (drawn) {
            applyHeatingToScreen(encoder, mainTarget);
        }
    }

    private static void applyHeatingToScreen(final CommandEncoder encoder, final RenderTarget mainTarget) {
        // The pass writes into the main target, so it reads a copy of the scene colors
        encoder.copyTextureToTexture(mainTarget.getColorTexture(), sceneCopy.getColorTexture(), 0, 0, 0, 0, 0, mainTarget.width, mainTarget.height);

        final GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
        try (final RenderPass renderPass = encoder.createRenderPass(() -> "Aeronautics hot air soft light",
                mainTarget.getColorTextureView(), OptionalInt.empty())) {
            renderPass.setPipeline(SOFT_LIGHT);
            renderPass.bindTexture("MainSampler", sceneCopy.getColorTextureView(), sampler);
            renderPass.bindTexture("MainDepthSampler", mainTarget.getDepthTextureView(), sampler);
            renderPass.bindTexture("OverlaySampler", overlayTarget.getColorTextureView(), sampler);
            renderPass.bindTexture("OverlayDepthSampler", overlayTarget.getDepthTextureView(), sampler);
            renderPass.draw(0, 3);
        }
    }

    private static void freeTargets() {
        if (overlayTarget != null) {
            overlayTarget.destroyBuffers();
            overlayTarget = null;
        }
        if (sceneCopy != null) {
            sceneCopy.destroyBuffers();
            sceneCopy = null;
        }
    }

    public static void free() {
        freeTargets();
        if (uniforms != null) {
            uniforms.close();
            uniforms = null;
        }
    }

    private record HotAirUniform(float scroll, float cutoffY) implements DynamicUniformStorage.DynamicUniform {
        @Override
        public void write(final ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer).putVec4(this.scroll, this.cutoffY, 0.0f, 0.0f);
        }
    }
}
