package dev.simulated_team.simulated.content.end_sea;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.vertex.*;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.void_anchor.VoidAnchorBlockEntity;
import dev.simulated_team.simulated.util.SimpleSubLevelGroupRenderer;
import dev.ryanhcode.sable.veillite.api.client.render.MatrixStack;
import dev.ryanhcode.sable.veillite.api.event.VeilRenderLevelStageEvent;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import org.lwjgl.system.MemoryStack;

import java.lang.Math;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Renders the sub-levels above the end sea into a depth / "strength" shadow map seen from below, and spreads it out.
 * <p>
 * 1.21.11 port: the Veil framebuffer {@code simulated:end_sea_shadows} and the post pipeline
 * {@code simulated:spread_end_sea} are two {@link TextureTarget}s and the {@link #SPREAD_PIPELINE} pass, ping-ponged
 * five times like the post pipeline did.
 */
public class EndSeaShadowRenderer {
    public static final float SHADOW_VOLUME_RADIUS = 256f / 2f;
    private static final int SHADOW_MAP_SIZE = 1024;
    private static final Matrix4f PROJECTION_MAT = new Matrix4f();
    private static final Vector3d SHADOW_CAMERA_POSITION = new Vector3d();
    private static boolean isRenderingShadowMap = false;
    private static final ObjectArrayList<Vector3dc> voidAnchors = new ObjectArrayList<>();

    /**
     * Copies the shadow map outwards each pass, keeping the nearest depth and fading the strength
     * (the Veil post pipeline also wrote depth from the fragment shader)
     */
    private static final RenderPipeline SPREAD_PIPELINE = RenderPipeline.builder()
            .withLocation(Simulated.path("pipeline/spread_end_sea"))
            .withVertexShader("core/screenquad")
            .withFragmentShader(Simulated.path("core/spread_end_sea"))
            .withSampler("DiffuseSampler0")
            .withSampler("DiffuseDepthSampler")
            .withUniform("SpreadInfo", UniformType.UNIFORM_BUFFER)
            // the target depth is cleared to 1.0 first, so every fragment passes and writes its depth
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withDepthWrite(true)
            .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
            .build();

    private static final RenderType VOID_ANCHOR_CRACKS = RenderType.create(
            Simulated.MOD_ID + ":void_anchor_cracks",
            RenderSetup.builder(RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                            .withLocation(Simulated.path("pipeline/void_anchor_cracks"))
                            .withVertexShader("core/position_tex_color")
                            .withFragmentShader("core/position_tex_color")
                            .withSampler("Sampler0")
                            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
                            .build())
                    .withTexture("Sampler0", Simulated.path("textures/effects/cracks.png"))
                    .createRenderSetup());

    private static @Nullable RenderTarget shadowTarget;
    private static @Nullable RenderTarget swapTarget;
    private static @Nullable GpuBuffer spreadInfo;

    public static boolean isEnabled() {
        return true;
    }

    /**
     * Upstream entry point (Veil's AFTER_LEVEL stage). The veillite shim never fires render stages, so the shadow map is
     * rendered by {@link #renderShadowMap(Camera)} from the end sea frame pass instead.
     */
    @Deprecated
    public static void renderShadowMap(final VeilRenderLevelStageEvent.Stage stage, final LevelRenderer levelRenderer, final MultiBufferSource.BufferSource bufferSource, final MatrixStack matrixStack, final Matrix4fc frustumMatrix, final Matrix4fc projectionMatrix, final int renderTick, final DeltaTracker deltaTracker, final Camera camera, final Frustum frustum) {
    }

    public static void renderShadowMap(final Camera camera) {
        if (!EndSeaShadowRenderer.isEnabled()) {
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        final EndSeaPhysics physics = EndSeaPhysicsData.of(level);

        if (physics == null) {
            return;
        }

        final RenderTarget fbo = getOrCreateShadowsFramebuffer();
        if (fbo == null) {
            return;
        }

        final float zNear = 0.5f;

        final Matrix4f modelView = new Matrix4f();
        PROJECTION_MAT.identity().ortho(-SHADOW_VOLUME_RADIUS, SHADOW_VOLUME_RADIUS, -SHADOW_VOLUME_RADIUS, SHADOW_VOLUME_RADIUS, zNear, SHADOW_VOLUME_RADIUS);

        // account for the smaller screen size
        final Vec3 cameraPosition = camera.position();
        final Vec3 shadowCameraPosition = new Vec3(cameraPosition.x, physics.startY() - SHADOW_VOLUME_RADIUS, cameraPosition.z);

        SHADOW_CAMERA_POSITION.set(JOMLConversion.toJOML(shadowCameraPosition));
        SHADOW_CAMERA_POSITION.set(Math.floor(SHADOW_CAMERA_POSITION.x), SHADOW_CAMERA_POSITION.y, Math.floor(SHADOW_CAMERA_POSITION.z));
        isRenderingShadowMap = true;

        final Quaternionf orientation = new Quaternionf().rotateX(Mth.DEG_TO_RAD * -90);
        final BoundingBox3dc bounds = new BoundingBox3d(-30_000_000, -10_000, -30_000_000, 30_000_000, 10_000, 30_000_000);

        final List<ClientSubLevel> clientSubLevelGroup = new ObjectArrayList<>();
        final Iterable<SubLevel> intersecting = Sable.HELPER.getAllIntersecting(level, bounds);

        for (final SubLevel subLevel : intersecting) {
            clientSubLevelGroup.add((ClientSubLevel) subLevel);
        }

        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(fbo.getColorTexture(), 0, fbo.getDepthTexture(), 1.0);
        try {
            SimpleSubLevelGroupRenderer.renderGroup(level, clientSubLevelGroup, fbo, modelView, PROJECTION_MAT, SHADOW_CAMERA_POSITION, orientation, SHADOW_VOLUME_RADIUS / 16f, false);
        } finally {
            isRenderingShadowMap = false;
        }

        for (int i = 0; i < 5; i++) {
            spread(fbo, swapTarget);
            spread(swapTarget, fbo);
        }
    }

    /**
     * One blit of the spread pipeline
     */
    private static void spread(final RenderTarget in, final RenderTarget out) {
        if (spreadInfo == null) {
            try (final MemoryStack stack = MemoryStack.stackPush()) {
                // OutSize
                spreadInfo = RenderSystem.getDevice().createBuffer(() -> "Simulated end sea spread info", GpuBuffer.USAGE_UNIFORM,
                        Std140Builder.onStack(stack, 16).putVec2(out.width, out.height).get());
            }
        }

        final GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);

        try (final RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(() -> "Simulated end sea shadow spread", out.getColorTextureView(), OptionalInt.empty(), out.getDepthTextureView(), OptionalDouble.of(1.0))) {
            renderPass.setPipeline(SPREAD_PIPELINE);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("SpreadInfo", spreadInfo);
            renderPass.bindTexture("DiffuseSampler0", in.getColorTextureView(), sampler);
            renderPass.bindTexture("DiffuseDepthSampler", in.getDepthTextureView(), sampler);
            renderPass.draw(0, 3);
        }
    }

    /**
     * Draws the void anchors collected this frame. (Never called upstream either.)
     */
    public static void renderVoidAnchors(final Camera camera, final MultiBufferSource bufferSource) {
        if (voidAnchors.isEmpty()) {
            return;
        }

        final VertexConsumer builder = bufferSource.getBuffer(VOID_ANCHOR_CRACKS);
        final Vector3d pos = new Vector3d();
        final Vec3 cameraPos = camera.position();

        for (final Vector3dc voidAnchor : voidAnchors) {
            // render quad
            final float size = 60;

            voidAnchor.sub(cameraPos.x, cameraPos.y, cameraPos.z, pos);
            final Matrix4f pose = new Matrix4f().translate((float) pos.x, (float) pos.y, (float) pos.z);
            builder.addVertex(pose, -size, 0, -size).setUv(0.0f, 0.0f).setColor(0.5f, 0, 0, 1);
            builder.addVertex(pose, size, 0, -size).setUv(1.0f, 0.0f).setColor(0.5f, 0, 0, 1);
            builder.addVertex(pose, size, 0, size).setUv(1.0f, 1.0f).setColor(0.5f, 0, 0, 1);
            builder.addVertex(pose, -size, 0, size).setUv(0.0f, 1.0f).setColor(0.5f, 0, 0, 1);
        }

        voidAnchors.clear();
    }

    public static void tick() {
        // void anchors are added every frame, but nothing draws (and clears) them, so keep the list from growing forever
        voidAnchors.clear();
    }

    private static @Nullable RenderTarget getOrCreateShadowsFramebuffer() {
        if (shadowTarget == null) {
            shadowTarget = new TextureTarget("Simulated end sea shadows", SHADOW_MAP_SIZE, SHADOW_MAP_SIZE, true);
            swapTarget = new TextureTarget("Simulated end sea shadows (swap)", SHADOW_MAP_SIZE, SHADOW_MAP_SIZE, true);
        }
        return shadowTarget;
    }

    public static @Nullable RenderTarget getShadowsFramebuffer() {
        return shadowTarget;
    }

    public static boolean renderingShadowMap() {
        return isRenderingShadowMap;
    }

    public static Vector3dc getLastRenderOrigin() {
        return SHADOW_CAMERA_POSITION;
    }

    public static void addVoidAnchor(final VoidAnchorBlockEntity voidAnchor) {
        voidAnchors.add(Sable.HELPER.projectOutOfSubLevel(voidAnchor.getLevel(), JOMLConversion.atCenterOf(voidAnchor.getBlockPos())));
    }
}
