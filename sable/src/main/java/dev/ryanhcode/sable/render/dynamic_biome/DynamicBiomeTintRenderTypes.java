package dev.ryanhcode.sable.render.dynamic_biome;

/**
 * Biome tinting of blocks moved by sub-levels.
 * <p>
 * This was never functional in the original mod: the render type was a commented-out sketch built on Veil's
 * {@code VeilRenderBridge.shaderState}. Nothing referenced it.
 * <p>
 * PORT-TODO(1.21.11): If this is ever implemented, it needs a terrain {@code RenderPipeline} variant whose vertex
 * shader takes the biome tint at the sub-level's current position, rather than a {@code RenderType} subclass
 * ({@code RenderType} can no longer be extended or built from {@code CompositeState} in 1.21.11).
 */
public final class DynamicBiomeTintRenderTypes {
    private DynamicBiomeTintRenderTypes() {
    }
}
