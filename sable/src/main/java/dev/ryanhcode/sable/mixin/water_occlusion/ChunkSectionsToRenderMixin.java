package dev.ryanhcode.sable.mixin.water_occlusion;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuSampler;
import dev.ryanhcode.sable.SableClient;
import dev.ryanhcode.sable.render.water_occlusion.WaterOcclusionRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkSectionsToRender.class)
public class ChunkSectionsToRenderMixin {

    /**
     * Renders the water occlusion buffers right before the translucent layer, while no render pass is open
     */
    @Inject(method = "renderGroup", at = @At("HEAD"))
    private void sable$preRenderTranslucent(final ChunkSectionLayerGroup group, final GpuSampler sampler, final CallbackInfo ci) {
        if (group == ChunkSectionLayerGroup.TRANSLUCENT) {
            SableClient.WATER_OCCLUSION_RENDERER.preRenderTranslucent();
        }
    }

    /**
     * Swaps in the occluded translucent terrain pipeline, which discards water inside occlusion volumes
     */
    @WrapOperation(method = "renderGroup", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderPass;setPipeline(Lcom/mojang/blaze3d/pipeline/RenderPipeline;)V"))
    private void sable$setTranslucentPipeline(final RenderPass renderPass, final RenderPipeline pipeline, final Operation<Void> original) {
        final WaterOcclusionRenderer renderer = SableClient.WATER_OCCLUSION_RENDERER;

        if (pipeline == RenderPipelines.TRANSLUCENT_TERRAIN && renderer.isActive()) {
            original.call(renderPass, WaterOcclusionRenderer.TRANSLUCENT_TERRAIN_OCCLUDED);
            renderer.bindSamplers(renderPass);
            return;
        }

        original.call(renderPass, pipeline);
    }
}
