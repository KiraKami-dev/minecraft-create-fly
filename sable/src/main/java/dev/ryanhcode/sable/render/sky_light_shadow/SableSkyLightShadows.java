package dev.ryanhcode.sable.render.sky_light_shadow;

import dev.ryanhcode.sable.veillite.api.compat.SodiumCompat;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.render.water_occlusion.WaterOcclusionRenderer;
import dev.ryanhcode.sable.sublevel.render.dispatcher.SubLevelRenderDispatcher;
import dev.ryanhcode.sable.sublevel.render.dispatcher.VanillaSubLevelRenderDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PerspectiveProjectionMatrixBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;

/**
 * Sub-levels casting shadows on the world's sky light.
 * <p>
 * Right before the opaque terrain group, every sub-level section is rendered depth-only, top-down and orthographic,
 * into a {@value #MAP_SIZE}x{@value #MAP_SIZE} target centered on the camera. The terrain pipelines are then swapped
 * for variants ({@code sable:core/terrain_shadowed}) whose vertex shader samples that depth and scales the sky light
 * component of the vertex light below sub-levels.
 * <p>
 * Single-block sub-levels are not part of the map, and neither is anything when Sodium replaces the section renderer.
 */
public class SableSkyLightShadows {

    public static final float SHADOW_VOLUME_SIZE = 256f / 2f;
    public static final String SAMPLER_NAME = "SableShadowSampler";
    public static final String UNIFORM_NAME = "SableShadow";

    private static final int MAP_SIZE = 256;
    private static final int UNIFORM_SIZE = 16;
    private static final float NEAR_PLANE = 0.5f;

    private static final RenderPipeline.Snippet SHADOWED_TERRAIN_SNIPPET = RenderPipeline.builder(RenderPipelines.TERRAIN_SNIPPET)
            .withVertexShader(Sable.sablePath("core/terrain_shadowed"))
            .withUniform(UNIFORM_NAME, UniformType.UNIFORM_BUFFER)
            .withSampler(SAMPLER_NAME)
            .buildSnippet();

    public static final RenderPipeline SOLID = RenderPipeline.builder(SHADOWED_TERRAIN_SNIPPET)
            .withLocation(Sable.sablePath("pipeline/shadowed_solid_terrain"))
            .build();

    public static final RenderPipeline CUTOUT = RenderPipeline.builder(SHADOWED_TERRAIN_SNIPPET)
            .withLocation(Sable.sablePath("pipeline/shadowed_cutout_terrain"))
            .withShaderDefine("ALPHA_CUTOUT", 0.5F)
            .build();

    public static final RenderPipeline TRANSLUCENT = RenderPipeline.builder(SHADOWED_TERRAIN_SNIPPET)
            .withLocation(Sable.sablePath("pipeline/shadowed_translucent_terrain"))
            .withBlend(com.mojang.blaze3d.pipeline.BlendFunction.TRANSLUCENT)
            .withShaderDefine("ALPHA_CUTOUT", 0.01F)
            .build();

    public static final RenderPipeline TRIPWIRE = RenderPipeline.builder(SHADOWED_TERRAIN_SNIPPET)
            .withLocation(Sable.sablePath("pipeline/shadowed_tripwire_terrain"))
            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
            .withBlend(com.mojang.blaze3d.pipeline.BlendFunction.TRANSLUCENT)
            .build();

    /**
     * Translucent terrain with both the water occlusion discard and the shadows
     */
    public static final RenderPipeline TRANSLUCENT_OCCLUDED = RenderPipeline.builder(SHADOWED_TERRAIN_SNIPPET)
            .withLocation(Sable.sablePath("pipeline/shadowed_translucent_terrain_occluded"))
            .withFragmentShader(Sable.sablePath("core/terrain_occluded"))
            .withSampler(WaterOcclusionRenderer.CLOSE_SAMPLER_NAME)
            .withSampler(WaterOcclusionRenderer.FAR_SAMPLER_NAME)
            .withBlend(com.mojang.blaze3d.pipeline.BlendFunction.TRANSLUCENT)
            .withShaderDefine("ALPHA_CUTOUT", 0.01F)
            .build();

    private static final Matrix4f PROJECTION = new Matrix4f();
    private static final Matrix4f VIEW = new Matrix4f();
    private static final Vector3d SHADOW_CAMERA_POSITION = new Vector3d();

    private static boolean isEnabled = false;
    private static boolean active = false;
    private static TextureTarget target;
    private static PerspectiveProjectionMatrixBuffer projectionBuffer;
    private static GpuBuffer uniformBuffer;

    public static boolean isEnabled() {
        return isEnabled;
    }

    public static void setIsEnabled(final boolean isEnabled) {
        SableSkyLightShadows.isEnabled = isEnabled;
        if (!isEnabled) {
            active = false;
        }
    }

    /**
     * Renders the shadow map for this frame. Must be called outside of any open render pass, before the opaque terrain group.
     */
    public static void renderShadowMap() {
        active = false;

        // Sodium draws the world terrain with its own shaders, which have no shadowed variant
        if (!isEnabled || SodiumCompat.isLoaded()) {
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        if (level == null || !(SubLevelRenderDispatcher.get() instanceof final VanillaSubLevelRenderDispatcher dispatcher)) {
            return;
        }

        final ClientSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null || container.getAllSubLevels().isEmpty()) {
            return;
        }

        if (target == null) {
            target = new TextureTarget("Sable sky light shadows", MAP_SIZE, MAP_SIZE, true);
            projectionBuffer = new PerspectiveProjectionMatrixBuffer("Sable sky light shadows");
            uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "Sable sky light shadow UBO", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, UNIFORM_SIZE);
        }

        // the map is centered on the camera, a bit above it, looking straight down
        final Vec3 camera = minecraft.gameRenderer.getMainCamera().position();
        SHADOW_CAMERA_POSITION.set(Math.floor(camera.x), camera.y + SHADOW_VOLUME_SIZE / 2.0, Math.floor(camera.z));

        // view space: x = world x, y = -world z (north is up on the map), -z = down
        VIEW.identity().rotateX((float) (Math.PI / 2));
        PROJECTION.identity().ortho(-SHADOW_VOLUME_SIZE, SHADOW_VOLUME_SIZE, -SHADOW_VOLUME_SIZE, SHADOW_VOLUME_SIZE, NEAR_PLANE, SHADOW_VOLUME_SIZE);
        final GpuBufferSlice projection = projectionBuffer.getBuffer(PROJECTION);

        // sections are positioned relative to the shadow camera, so the view matrix only rotates
        if (!dispatcher.renderShadowMap(container.getAllSubLevels(), target, VIEW, projection, SHADOW_CAMERA_POSITION)) {
            return;
        }

        try (final MemoryStack stack = MemoryStack.stackPush()) {
            final ByteBuffer buffer = Std140Builder.onStack(stack, UNIFORM_SIZE)
                    .putVec4((float) (SHADOW_CAMERA_POSITION.x - camera.x), (float) (SHADOW_CAMERA_POSITION.y - camera.y), (float) (SHADOW_CAMERA_POSITION.z - camera.z), SHADOW_VOLUME_SIZE)
                    .get();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(uniformBuffer.slice(), buffer);
        }

        active = true;
    }

    /**
     * @return if the terrain pipelines should be swapped for the shadowed ones this frame
     */
    public static boolean isActive() {
        return active;
    }

    /**
     * @param pipeline A vanilla terrain pipeline
     * @return The shadowed variant of the pipeline, or the pipeline itself if there is none
     */
    public static RenderPipeline getShadowedPipeline(final RenderPipeline pipeline) {
        if (pipeline == RenderPipelines.SOLID_TERRAIN) {
            return SOLID;
        } else if (pipeline == RenderPipelines.CUTOUT_TERRAIN) {
            return CUTOUT;
        } else if (pipeline == RenderPipelines.TRANSLUCENT_TERRAIN) {
            return TRANSLUCENT;
        } else if (pipeline == RenderPipelines.TRIPWIRE_TERRAIN) {
            return TRIPWIRE;
        } else if (pipeline == WaterOcclusionRenderer.TRANSLUCENT_TERRAIN_OCCLUDED) {
            return TRANSLUCENT_OCCLUDED;
        }
        return pipeline;
    }

    /**
     * Binds the shadow map and its uniform to a pass using a shadowed pipeline
     */
    public static void bind(final RenderPass renderPass) {
        renderPass.setUniform(UNIFORM_NAME, uniformBuffer);
        renderPass.bindTexture(SAMPLER_NAME, target.getDepthTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
    }
}
