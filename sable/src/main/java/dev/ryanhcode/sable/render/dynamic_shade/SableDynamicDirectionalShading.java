package dev.ryanhcode.sable.render.dynamic_shade;

/**
 * Dynamic directional shading of rotated sub-levels: baked block-face shading is disabled in sub-level chunk meshes
 * (see {@link SubLevelVertexConsumer}), and a shader re-shades each vertex from its normal and the sub-level's rotation.
 * <p>
 * On 1.21.11 the shading half is in {@code sub_level_terrain.vsh} (rotation from the {@code SableSubLevel} uniform
 * block). The mesh half covers both the vanilla block renderer and Fabric's Indigo renderer
 * ({@code RenderSectionRegionMixin}, {@code IndigoRenderContextMixin}).
 */
public class SableDynamicDirectionalShading {

    private static final boolean SUPPORTED = true;

    private static boolean isEnabled = false;

    public static boolean isEnabled() {
        return SUPPORTED && isEnabled;
    }

    public static void setIsEnabled(final boolean isEnabled) {
        SableDynamicDirectionalShading.isEnabled = isEnabled;
    }
}
