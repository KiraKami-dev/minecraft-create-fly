package dev.eriksonn.aeronautics.index.client;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.content.blocks.levitite.LevititeShaderManager;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderPipelines;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.renderer.RenderPipelines;

/**
 * The levitite render pipelines (upstream: the {@code aeronautics:levitite} and {@code aeronautics:levitite_ghosts}
 * Veil chunk render types with the tessellated {@code aeronautics:levitite/levitite} shader).
 * <p>
 * Both draw the compiled solid section meshes of sub-levels with {@code aeronautics:core/levitite}, which skips every
 * quad that is not textured with a levitite sprite. See {@link dev.eriksonn.aeronautics.content.blocks.levitite.render.LevititeRenderer}.
 */
public class AeroRenderTypes {

    public static final String LEVITITE_UNIFORM = "LevititeInfo";

    private static final RenderPipeline.Snippet LEVITITE_SNIPPET = RenderPipeline.builder(RenderPipelines.TERRAIN_SNIPPET, RenderPipelines.GLOBALS_SNIPPET)
            .withVertexShader(Aeronautics.path("core/levitite"))
            .withFragmentShader(Aeronautics.path("core/levitite"))
            .withUniform(SubLevelRenderPipelines.SUB_LEVEL_UNIFORM, UniformType.UNIFORM_BUFFER)
            .withUniform(LEVITITE_UNIFORM, UniformType.UNIFORM_BUFFER)
            .withSampler("Noise")
            .withBlend(BlendFunction.TRANSLUCENT)
            .buildSnippet();

    /**
     * Layer 0: the shimmering block, drawn over the solid fallback block (depth tested, culled).
     */
    public static final RenderPipeline LEVITITE = RenderPipeline.builder(LEVITITE_SNIPPET)
            .withLocation(Aeronautics.path("pipeline/levitite"))
            .build();

    /**
     * Layers 1 and -1: the translucent ghosts trailing moving sub-levels. Drawn without a depth attachment (upstream
     * disabled the depth test), fading against the scene depth sampled from {@code DepthSampler}.
     */
    public static final RenderPipeline LEVITITE_GHOSTS = RenderPipeline.builder(LEVITITE_SNIPPET)
            .withLocation(Aeronautics.path("pipeline/levitite_ghosts"))
            .withShaderDefine("GHOSTS")
            .withSampler("DepthSampler")
            .withCull(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .build();

    private AeroRenderTypes() {
    }

    /**
     * Client hook (called from {@code AeronauticsClient}). The levitite layers need no chunk layer or buffer registration
     * on 1.21.11 (they are drawn by {@code mixin/levitite/VanillaSubLevelRenderDispatcherMixin}); this only drops the
     * per sub-level motion trackers when leaving a world.
     */
    public static void registerClient() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> LevititeShaderManager.managers.clear());
    }
}
