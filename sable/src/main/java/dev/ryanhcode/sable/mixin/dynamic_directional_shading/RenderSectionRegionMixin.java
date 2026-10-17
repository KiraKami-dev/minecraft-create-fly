package dev.ryanhcode.sable.mixin.dynamic_directional_shading;

import dev.ryanhcode.sable.mixinterface.dynamic_directional_shading.ModelBlockRendererCacheExtension;
import dev.ryanhcode.sable.render.dynamic_shade.SableDynamicDirectionalShading;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sub-level sections bake no face shade, the sub-level terrain shader applies it from the rotated normal.
 * Covers every shade lookup of a section compile, including Fabric's Indigo renderer (flat, AO and normal shading).
 */
@Mixin(RenderSectionRegion.class)
public class RenderSectionRegionMixin {

    @Inject(method = "getShade", at = @At("HEAD"), cancellable = true)
    private void sable$getShade(final Direction direction, final boolean shade, final CallbackInfoReturnable<Float> cir) {
        if (SableDynamicDirectionalShading.isEnabled() && ((ModelBlockRendererCacheExtension) ModelBlockRenderer.CACHE.get()).sable$getOnSubLevel()) {
            cir.setReturnValue(1.0f);
        }
    }
}
