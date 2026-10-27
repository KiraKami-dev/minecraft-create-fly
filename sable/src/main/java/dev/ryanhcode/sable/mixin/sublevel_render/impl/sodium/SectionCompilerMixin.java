package dev.ryanhcode.sable.mixin.sublevel_render.impl.sodium;

import dev.ryanhcode.sable.sublevel.render.iris.SubLevelIrisCompat;
import dev.ryanhcode.sable.veillite.api.compat.IrisCompat;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * With Sodium, the vanilla section compiler only builds sub-level sections. Keeps their meshes in the vanilla
 * {@code BLOCK} format the sub-level pipelines draw, even while an Iris shader pack is in use
 * (see {@link SubLevelIrisCompat#setSkipVertexFormatExtension}).
 */
@Mixin(SectionCompiler.class)
public class SectionCompilerMixin {

    @Inject(method = "compile", at = @At("HEAD"))
    private void sable$skipIrisFormat(final CallbackInfoReturnable<SectionCompiler.Results> cir) {
        if (IrisCompat.isLoaded()) {
            SubLevelIrisCompat.setSkipVertexFormatExtension(true);
        }
    }

    @Inject(method = "compile", at = @At("RETURN"))
    private void sable$restoreIrisFormat(final CallbackInfoReturnable<SectionCompiler.Results> cir) {
        if (IrisCompat.isLoaded()) {
            SubLevelIrisCompat.setSkipVertexFormatExtension(false);
        }
    }
}
