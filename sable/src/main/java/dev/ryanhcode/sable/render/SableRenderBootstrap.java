package dev.ryanhcode.sable.render;

import dev.ryanhcode.sable.sublevel.render.SubLevelRenderPipelines;
import dev.ryanhcode.sable.veillite.api.client.render.VeilRenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

/**
 * Client render setup for Sable.
 * <p>
 * On 1.21.11 there is no shader pre-processing: the sub-level render effects are implemented with dedicated
 * {@link com.mojang.blaze3d.pipeline.RenderPipeline}s, which pick up their shaders from the resource manager like
 * vanilla pipelines.
 */
public final class SableRenderBootstrap {

    private SableRenderBootstrap() {
    }

    /**
     * Called once from {@link dev.ryanhcode.sable.SableClient#init()}.
     */
    public static void init() {
        // Make sure the sub-level pipelines are created up front, so a broken pipeline definition fails at startup
        // instead of in the middle of the first frame with a sub-level
        SubLevelRenderPipelines.get(ChunkSectionLayer.SOLID);
    }

    /**
     * Called when a client config option that used to require a shader reload changed.
     * <p>
     * The shader side of these effects is now chosen per frame, but the sub-level meshes bake the shading mode
     * (see {@link dev.ryanhcode.sable.render.dynamic_shade.SableDynamicDirectionalShading}), so sections are rebuilt.
     */
    public static void onShaderConfigChanged() {
        Minecraft.getInstance().execute(VeilRenderSystem::rebuildChunks);
    }
}
