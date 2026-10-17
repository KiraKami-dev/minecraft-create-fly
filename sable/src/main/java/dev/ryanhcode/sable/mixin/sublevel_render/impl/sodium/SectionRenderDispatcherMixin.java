package dev.ryanhcode.sable.mixin.sublevel_render.impl.sodium;

import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.SectionBufferBuilderPool;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Sodium empties vanilla's section buffer pool, so vanilla's async section compiles would never run. Sub-levels still
 * compile through the vanilla dispatcher, so give it a pool of its own.
 */
@Mixin(SectionRenderDispatcher.class)
public class SectionRenderDispatcherMixin {

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderBuffers;sectionBufferPool()Lnet/minecraft/client/renderer/SectionBufferBuilderPool;"))
    private SectionBufferBuilderPool sable$ownBufferPool(final RenderBuffers renderBuffers) {
        return SectionBufferBuilderPool.allocate(Math.min(4, Runtime.getRuntime().availableProcessors()));
    }
}
