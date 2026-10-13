package dev.ryanhcode.sable.mixin.water_occlusion;

import dev.ryanhcode.sable.SableClient;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    /**
     * The chunk renderer only receives the level's model-view matrix here, so grab it for the water occlusion buffers
     */
    @Inject(method = "prepareChunkRenders", at = @At("HEAD"))
    private void sable$captureModelView(final Matrix4fc modelView, final double x, final double y, final double z, final CallbackInfoReturnable<ChunkSectionsToRender> cir) {
        SableClient.WATER_OCCLUSION_RENDERER.setLevelModelView(modelView);
    }
}
