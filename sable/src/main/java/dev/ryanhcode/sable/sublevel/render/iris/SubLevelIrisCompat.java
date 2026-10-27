package dev.ryanhcode.sable.sublevel.render.iris;

import dev.ryanhcode.sable.sublevel.render.SubLevelRenderPipelines;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.api.v0.IrisProgram;
import net.irisshaders.iris.vertices.ImmediateState;

/**
 * Iris support for sub-level sections. Only loaded when Iris is present (check {@code IrisCompat.isLoaded()} first).
 * <p>
 * Iris only replaces the shaders of pipelines it knows; anything else is drawn into a buffer the shader pack's
 * final pass overwrites, so sub-levels were invisible. The sub-level pipelines are assigned to the shader pack's terrain
 * programs, which take their transform from the vanilla {@code DynamicTransforms} block (see
 * {@code VanillaSubLevelRenderDispatcher}) instead of Sable's sub-level pose block.
 */
public final class SubLevelIrisCompat {

    private SubLevelIrisCompat() {
    }

    public static void registerPipelines() {
        final IrisApi iris = IrisApi.getInstance();
        iris.assignPipeline(SubLevelRenderPipelines.SOLID, IrisProgram.TERRAIN_SOLID);
        iris.assignPipeline(SubLevelRenderPipelines.CUTOUT, IrisProgram.TERRAIN_CUTOUT);
        iris.assignPipeline(SubLevelRenderPipelines.TRANSLUCENT, IrisProgram.TRANSLUCENT);
        iris.assignPipeline(SubLevelRenderPipelines.TRIPWIRE, IrisProgram.TRANSLUCENT);
    }

    public static boolean isShaderPackInUse() {
        return IrisApi.getInstance().isShaderPackInUse();
    }

    /**
     * With a shader pack in use, Iris widens {@code BLOCK} format buffers to its own terrain format whenever the level is
     * rendering, which races with section compiles on worker threads. Sub-level sections are always drawn as
     * {@code BLOCK}, so their compiles opt out.
     */
    public static void setSkipVertexFormatExtension(final boolean skip) {
        ImmediateState.skipExtension.set(skip);
    }

    /**
     * Iris also reports {@code BLOCK} pipelines as its terrain format while the level renders. Sub-level sections are
     * drawn with this off, so the pipeline matches their {@code BLOCK} meshes ({@code BLOCK} is a prefix of the Iris
     * terrain format, so the shader pack program's attributes still line up).
     *
     * @return the previous value, to restore after the draws
     */
    public static boolean setExtendedVertexFormat(final boolean extended) {
        final boolean previous = ImmediateState.renderWithExtendedVertexFormat;
        ImmediateState.renderWithExtendedVertexFormat = extended;
        return previous;
    }
}
