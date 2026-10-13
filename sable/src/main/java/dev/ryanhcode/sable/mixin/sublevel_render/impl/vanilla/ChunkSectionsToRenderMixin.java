package dev.ryanhcode.sable.mixin.sublevel_render.impl.vanilla;

import com.mojang.blaze3d.textures.GpuSampler;
import dev.ryanhcode.sable.sublevel.render.dispatcher.SubLevelRenderDispatcher;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws the sub-level sections of each layer group right after the world sections of that group.
 */
@Mixin(ChunkSectionsToRender.class)
public class ChunkSectionsToRenderMixin {

    @Inject(method = "renderGroup", at = @At("TAIL"))
    private void sable$renderSubLevelSections(final ChunkSectionLayerGroup group, final GpuSampler sampler, final CallbackInfo ci) {
        SubLevelRenderDispatcher.get().renderSectionLayerGroup(group, sampler);
    }
}
