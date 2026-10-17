package dev.simulated_team.simulated.index;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.zurrtum.create.client.foundation.render.AllRenderPipelines;
import dev.simulated_team.simulated.Simulated;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.util.function.Function;

/**
 * Client: Simulated's render types, on the 1.21.11 {@link RenderPipeline} / {@link RenderSetup} model.
 * <p>
 * Upstream these were Veil "pinwheel" shader programs; they are ported to core shaders under
 * {@code assets/simulated/shaders/core} (same math; Veil's fog/light includes replaced by vanilla's uniform blocks).
 * Upstream also registered laser/lens/staff overlay as Veil fixed buffers per render stage; the 1.21.11 feature renderer
 * draws submitted geometry by render type, so that is gone.
 */
public final class SimRenderTypes {

    // Pipelines

    /**
     * Upstream pointed at a {@code staff_overlay} Veil program that does not exist in the upstream resources either;
     * this is the closest vanilla equivalent (untextured, translucent, no depth test). Unused upstream.
     */
    private static final RenderPipeline STAFF_OVERLAY_PIPELINE = register("staff_overlay", RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withVertexShader("core/position_color")
            .withFragmentShader("core/position_color")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthWrite(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLE_STRIP));

    private static final RenderPipeline LASER_PIPELINE = register("laser", RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withVertexShader(Simulated.path("core/laser"))
            .withFragmentShader(Simulated.path("core/laser"))
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS));

    private static final RenderPipeline LENS_PIPELINE = register("laser_pointer_lens", RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withVertexShader(Simulated.path("core/laser_pointer_lens"))
            .withFragmentShader(Simulated.path("core/laser_pointer_lens"))
            .withSampler("Sampler0")
            .withSampler("Sampler2")
            .withVertexFormat(DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS));

    /**
     * Vanilla's {@code position_color_tex_lightmap} shader is gone; {@code rendertype_text} is the same plus fog.
     */
    private static final RenderPipeline LOCK_PIPELINE = register("lock", RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withVertexShader("core/rendertype_text")
            .withFragmentShader("core/rendertype_text")
            .withSampler("Sampler0")
            .withSampler("Sampler2")
            .withDepthWrite(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS));

    private static final RenderPipeline ROPE_PIPELINE = register("rope", RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withVertexShader(Simulated.path("core/rope"))
            .withFragmentShader(Simulated.path("core/rope"))
            .withSampler("Sampler0")
            .withSampler("Sampler2")
            .withVertexFormat(DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS));

    /**
     * Vertex layout as upstream ({@code Position, Color ("Stress"), UV0, UV2, Normal}), i.e. {@link DefaultVertexFormat#BLOCK}.
     */
    private static final RenderPipeline SPRING_PIPELINE = register("spring", RenderPipeline.builder(RenderPipelines.MATRICES_FOG_LIGHT_DIR_SNIPPET)
            .withVertexShader(Simulated.path("core/spring"))
            .withFragmentShader(Simulated.path("core/spring"))
            .withSampler("Sampler0")
            .withSampler("Sampler2")
            .withVertexFormat(DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS));

    // Render types

    private static final RenderType STAFF_OVERLAY = RenderType.create(
            Simulated.MOD_ID + ":staff_overlay/staff_overlay",
            RenderSetup.builder(STAFF_OVERLAY_PIPELINE)
                    .bufferSize(RenderType.TRANSIENT_BUFFER_SIZE)
                    .sortOnUpload()
                    .createRenderSetup()
    );
    private static final RenderType LASER = RenderType.create(
            Simulated.MOD_ID + ":laser",
            RenderSetup.builder(LASER_PIPELINE)
                    .bufferSize(RenderType.TRANSIENT_BUFFER_SIZE)
                    .sortOnUpload()
                    .createRenderSetup()
    );
    private static final RenderType LENS = RenderType.create(
            Simulated.MOD_ID + ":laser_pointer_lens",
            RenderSetup.builder(LENS_PIPELINE)
                    .withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS)
                    .useLightmap()
                    .bufferSize(RenderType.TRANSIENT_BUFFER_SIZE)
                    .affectsCrumbling()
                    .sortOnUpload()
                    .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                    .createRenderSetup()
    );
    private static final RenderType LOCK = RenderType.create(
            Simulated.MOD_ID + ":lock",
            RenderSetup.builder(LOCK_PIPELINE)
                    .withTexture("Sampler0", Simulated.path("textures/gui/lock.png"))
                    .useLightmap()
                    .bufferSize(RenderType.TRANSIENT_BUFFER_SIZE)
                    .affectsCrumbling()
                    .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                    .createRenderSetup()
    );
    private static final RenderType ROPE = RenderType.create(
            Simulated.MOD_ID + ":rope",
            RenderSetup.builder(ROPE_PIPELINE)
                    .withTexture("Sampler0", Simulated.path("textures/block/rope_particle.png"))
                    .useLightmap()
                    .bufferSize(RenderType.TRANSIENT_BUFFER_SIZE)
                    .affectsCrumbling()
                    .createRenderSetup()
    );
    private static final Function<Identifier, RenderType> SPRING = Util.memoize((Identifier texture) -> RenderType.create(
            Simulated.MOD_ID + ":spring",
            RenderSetup.builder(SPRING_PIPELINE)
                    .withTexture("Sampler0", texture)
                    .useLightmap()
                    .bufferSize(RenderType.TRANSIENT_BUFFER_SIZE)
                    .affectsCrumbling()
                    .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                    .createRenderSetup()
    ));

    /**
     * Create's glowing item render types bound to the <b>items</b> atlas: since 1.21.11, item textures (the Physics
     * Staff's partials use {@code item/...} textures) are stitched into {@link TextureAtlas#LOCATION_ITEMS}.
     */
    private static final RenderType ITEM_GLOWING_SOLID = RenderType.create(
            Simulated.MOD_ID + ":item_glowing_solid",
            RenderSetup.builder(AllRenderPipelines.GLOWING)
                    .bufferSize(256)
                    .affectsCrumbling()
                    .withTexture("Sampler0", TextureAtlas.LOCATION_ITEMS)
                    .useLightmap()
                    .useOverlay()
                    .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                    .createRenderSetup()
    );
    private static final RenderType ITEM_GLOWING_TRANSLUCENT = RenderType.create(
            Simulated.MOD_ID + ":item_glowing_translucent",
            RenderSetup.builder(AllRenderPipelines.GLOWING_TRANSLUCENT)
                    .bufferSize(256)
                    .affectsCrumbling()
                    .sortOnUpload()
                    .withTexture("Sampler0", TextureAtlas.LOCATION_ITEMS)
                    .useLightmap()
                    .useOverlay()
                    .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                    .createRenderSetup()
    );

    private SimRenderTypes() {
    }

    private static RenderPipeline register(final String name, final RenderPipeline.Builder builder) {
        return RenderPipelines.register(builder.withLocation(Simulated.path("pipeline/" + name)).build());
    }

    /**
     * Registers the pipelines before the first resource reload, so they are precompiled with the vanilla ones.
     */
    public static void init() {
    }

    public static RenderType staffOverlay() {
        return STAFF_OVERLAY;
    }

    public static RenderType laser() {
        return LASER;
    }

    public static RenderType lens() {
        return LENS;
    }

    public static RenderType lock() {
        return LOCK;
    }

    public static RenderType rope() {
        return ROPE;
    }

    /**
     * @param shadersActive whether a shader pack is active; Create's glowing shader is then replaced by the item sheet
     */
    public static RenderType itemGlowingSolid(final boolean shadersActive) {
        return shadersActive ? Sheets.translucentItemSheet() : ITEM_GLOWING_SOLID;
    }

    public static RenderType itemGlowingTranslucent(final boolean shadersActive) {
        return shadersActive ? Sheets.translucentItemSheet() : ITEM_GLOWING_TRANSLUCENT;
    }

    public static RenderType spring(final Identifier texture) {
        return SPRING.apply(texture);
    }
}
