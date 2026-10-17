package dev.simulated_team.simulated.content.blocks.lasers;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.simulated_team.simulated.Simulated;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlas;

/**
 * Laser beam and laser pointer lens render types (client only).
 * <p>
 * Both were Veil shaders on 1.21.1 ({@code pinwheel/shaders/program/laser/laser} and {@code laser_pointer/lens}) drawn
 * through Veil fixed buffers; they are core shaders ({@code assets/simulated/shaders/core/laser*}) in 1.21.11 render pipelines.
 */
public final class LaserRenderTypes {

    /**
     * The beam: vertex colors, the uv tapers the end of the beam off. Translucent and drawn before translucent terrain,
     * so it does not write depth (it used to be drawn after particles).
     */
    public static final RenderPipeline LASER_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(Simulated.path("pipeline/laser"))
            .withVertexShader(Simulated.path("core/laser"))
            .withFragmentShader(Simulated.path("core/laser"))
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .withDepthWrite(false)
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
            .build();

    /**
     * The laser pointer lens: the grayscale lens texture is mapped onto the laser color.
     */
    public static final RenderPipeline LENS_PIPELINE = RenderPipeline.builder(RenderPipelines.BLOCK_SNIPPET)
            .withLocation(Simulated.path("pipeline/laser_pointer_lens"))
            .withVertexShader(Simulated.path("core/laser_pointer_lens"))
            .withFragmentShader(Simulated.path("core/laser_pointer_lens"))
            .build();

    private static final RenderType LASER = RenderType.create(
            Simulated.MOD_ID + ":laser",
            RenderSetup.builder(LASER_PIPELINE)
                    .sortOnUpload()
                    .createRenderSetup());

    private static final RenderType LENS = RenderType.create(
            Simulated.MOD_ID + ":laser_pointer_lens",
            RenderSetup.builder(LENS_PIPELINE)
                    .useLightmap()
                    .withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS, RenderTypes.MOVING_BLOCK_SAMPLER)
                    .affectsCrumbling()
                    .sortOnUpload()
                    .createRenderSetup());

    private LaserRenderTypes() {
    }

    public static RenderType laser() {
        return LASER;
    }

    public static RenderType lens() {
        return LENS;
    }
}
