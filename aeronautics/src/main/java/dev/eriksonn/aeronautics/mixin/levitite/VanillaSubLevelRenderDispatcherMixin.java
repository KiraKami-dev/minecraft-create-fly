package dev.eriksonn.aeronautics.mixin.levitite;

import com.mojang.blaze3d.textures.GpuSampler;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.effect.ClientBalloonEffectRenderer;
import dev.eriksonn.aeronautics.content.blocks.levitite.render.LevititeRenderer;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.render.dispatcher.VanillaSubLevelRenderDispatcher;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws the levitite effect layers of sub-levels after Sable's own section layers (upstream: a {@code WrapMethod} on
 * {@code VanillaChunkedSubLevelRenderData#renderChunkedSubLevel} for the levitite chunk layers, which no longer exist).
 * Sable calls the dispatcher once per frame and section layer group, sub-levels or not, so the hot air balloon effect
 * (upstream: Veil {@code AFTER_SOLID_BLOCKS}) is drawn from here too, after the opaque group.
 */
@Mixin(VanillaSubLevelRenderDispatcher.class)
public class VanillaSubLevelRenderDispatcherMixin {

    @Inject(method = "prepareSections", at = @At("TAIL"), remap = false)
    private void aeronautics$prepareLevitite(final Iterable<ClientSubLevel> sublevels, final Matrix4fc modelView, final double cameraX, final double cameraY, final double cameraZ, final CallbackInfo ci) {
        LevititeRenderer.prepare((VanillaSubLevelRenderDispatcher) (Object) this, sublevels, modelView, cameraX, cameraY, cameraZ);
        ClientBalloonEffectRenderer.setModelView(modelView);
    }

    @Inject(method = "renderSectionLayerGroup", at = @At("RETURN"), remap = false)
    private void aeronautics$renderLevitite(final ChunkSectionLayerGroup group, final GpuSampler sampler, final CallbackInfo ci) {
        LevititeRenderer.render(group, sampler);

        // upstream: Veil AFTER_SOLID_BLOCKS stage
        if (group == ChunkSectionLayerGroup.OPAQUE) {
            ClientBalloonEffectRenderer.render(group.outputTarget());
        }
    }

    @Inject(method = "free", at = @At("TAIL"), remap = false)
    private void aeronautics$freeLevitite(final CallbackInfo ci) {
        LevititeRenderer.free();
        ClientBalloonEffectRenderer.free();
    }
}
