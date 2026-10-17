package dev.ryanhcode.sable.mixin.water_occlusion;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuSampler;
import dev.ryanhcode.sable.SableClient;
import dev.ryanhcode.sable.render.sky_light_shadow.SableSkyLightShadows;
import dev.ryanhcode.sable.render.water_occlusion.WaterOcclusionRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import dev.ryanhcode.sable.veillite.api.compat.SodiumCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkSectionsToRender.class)
public class ChunkSectionsToRenderMixin {

    /**
     * Renders the sky light shadow map right before the first layer group, and the water occlusion buffers right
     * before the translucent layer, while no render pass is open
     */
    @Inject(method = "renderGroup", at = @At("HEAD"))
    private void sable$preRenderGroup(final ChunkSectionLayerGroup group, final GpuSampler sampler, final CallbackInfo ci) {
        if (SodiumCompat.isLoaded()) {
            return; // Sodium cancels renderGroup; its draw hook does this instead
        }

        if (group == ChunkSectionLayerGroup.OPAQUE) {
            SableSkyLightShadows.renderShadowMap();
        } else if (group == ChunkSectionLayerGroup.TRANSLUCENT) {
            SableClient.WATER_OCCLUSION_RENDERER.preRenderTranslucent();
        }
    }

    /**
     * Swaps in the occluded translucent terrain pipeline, which discards water inside occlusion volumes, and the
     * shadowed terrain pipelines, which darken the sky light below sub-levels
     */
    @WrapOperation(method = "renderGroup", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderPass;setPipeline(Lcom/mojang/blaze3d/pipeline/RenderPipeline;)V"))
    private void sable$setTranslucentPipeline(final RenderPass renderPass, final RenderPipeline pipeline, final Operation<Void> original) {
        final WaterOcclusionRenderer renderer = SableClient.WATER_OCCLUSION_RENDERER;

        final boolean occluded = pipeline == RenderPipelines.TRANSLUCENT_TERRAIN && renderer.isActive();
        final RenderPipeline base = occluded ? WaterOcclusionRenderer.TRANSLUCENT_TERRAIN_OCCLUDED : pipeline;
        final RenderPipeline swapped = SableSkyLightShadows.isActive() ? SableSkyLightShadows.getShadowedPipeline(base) : base;

        original.call(renderPass, swapped);
        if (occluded) {
            renderer.bindSamplers(renderPass);
        }
        if (swapped != base) {
            SableSkyLightShadows.bind(renderPass);
        }
    }
}
