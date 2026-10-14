package dev.ryanhcode.sable.mixin.loaded_chunk_debug;

import dev.ryanhcode.sable.SableClientConfig;
import dev.ryanhcode.sable.mixinhelpers.loaded_chunk_debug.SableChunkDebugRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.ChunkBorderRenderer;
import net.minecraft.util.debug.DebugValueAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkBorderRenderer.class)
public class ChunkBorderRendererMixin {

    @Inject(at = @At("HEAD"), method = "emitGizmos", cancellable = true)
    public void render(final double camX, final double camY, final double camZ, final DebugValueAccess debugValueAccess, final Frustum frustum, final float partialTick, final CallbackInfo ci) {
        if (SableClientConfig.DEBUG_DRAW_LOADED_CHUNKS.getAsBoolean()) {
            ci.cancel();
            SableChunkDebugRenderer.emitGizmos(camX, camY, camZ);
        }
    }
}
