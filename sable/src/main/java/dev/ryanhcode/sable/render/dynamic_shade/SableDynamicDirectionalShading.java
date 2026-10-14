package dev.ryanhcode.sable.render.dynamic_shade;

/**
 * Dynamic directional shading of rotated sub-levels: baked block-face shading is disabled in sub-level chunk meshes
 * (see {@link SubLevelVertexConsumer}), and a shader re-shades each vertex from its normal and the sub-level's rotation.
 * <p>
 * PORT-TODO(1.21.11): The shading half was a glslprocessor injection into the 1.21.1 {@code rendertype_*} chunk vertex
 * shaders (Veil's {@code light} include and {@code NormalMat}). 1.21.11 has no shader pre-processing; this needs a
 * dedicated sub-level terrain {@code RenderPipeline} with its own vertex shader taking the sub-level's rotation in a
 * uniform buffer, wired in by the sub-level section renderer. Until then this is hard-disabled regardless of
 * {@code sub_level_dynamic_shading}, because enabling only the mesh half would render sub-levels flat-lit.
 */
public class SableDynamicDirectionalShading {

    /**
     * Flip once the sub-level terrain pipeline applies the shading
     */
    private static final boolean SUPPORTED = false;

    private static boolean isEnabled = false;

    public static boolean isEnabled() {
        return SUPPORTED && isEnabled;
    }

    public static void setIsEnabled(final boolean isEnabled) {
        SableDynamicDirectionalShading.isEnabled = isEnabled;
    }
}
