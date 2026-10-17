package dev.ryanhcode.sable.mixin.sublevel_render.impl.sodium;

import com.mojang.blaze3d.textures.GpuSampler;
import dev.ryanhcode.sable.SableClient;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.render.sky_light_shadow.SableSkyLightShadows;
import dev.ryanhcode.sable.mixinterface.plot.SubLevelContainerHolder;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.render.dispatcher.SubLevelRenderDispatcher;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.server.level.BlockDestructionProgress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.SortedSet;

/**
 * Hooks sub-levels into Sodium's world renderer. Sub-level sections are still compiled and drawn
 * by Sable's vanilla-based renderer; Sodium only ever sees (and never draws) the plot chunks.
 */
@Mixin(value = SodiumWorldRenderer.class, remap = false)
public abstract class SodiumWorldRendererMixin {

    @Shadow
    private ClientLevel level;

    @Inject(method = "setupTerrain", at = @At("TAIL"))
    private void sable$setSectionDispatcherCamera(final Camera camera, final Viewport viewport, final FogParameters fogParameters, final boolean spectator, final boolean updateChunksImmediately, final ChunkRenderMatrices matrices, final CallbackInfo ci) {
        // vanilla's cullTerrain sets this, but Sodium replaces cullTerrain
        Minecraft.getInstance().levelRenderer.getSectionRenderDispatcher().setCameraPosition(camera.position());
    }

    @Inject(method = "drawChunkLayer", at = @At("HEAD"))
    private void sable$preDrawChunkLayer(final ChunkSectionLayerGroup group, final ChunkRenderMatrices matrices, final double x, final double y, final double z, final GpuSampler sampler, final CallbackInfo ci) {
        // what ChunkSectionsToRender#renderGroup does for vanilla terrain, outside of any render pass
        if (group == ChunkSectionLayerGroup.OPAQUE) {
            SableSkyLightShadows.renderShadowMap();
        } else if (group == ChunkSectionLayerGroup.TRANSLUCENT) {
            SableClient.WATER_OCCLUSION_RENDERER.preRenderTranslucent();
        }
    }

    @Inject(method = "drawChunkLayer", at = @At("TAIL"))
    private void sable$drawSubLevelSections(final ChunkSectionLayerGroup group, final ChunkRenderMatrices matrices, final double x, final double y, final double z, final GpuSampler sampler, final CallbackInfo ci) {
        SubLevelRenderDispatcher.get().renderSectionLayerGroup(group, sampler);
    }

    @Inject(method = "scheduleRebuildForChunk", at = @At("HEAD"), cancellable = true)
    private void sable$scheduleSubLevelRebuild(final int x, final int y, final int z, final boolean important, final CallbackInfo ci) {
        final SubLevelContainer plotContainer = ((SubLevelContainerHolder) this.level).sable$getPlotContainer();
        final LevelPlot plot = plotContainer.getPlot(x, z);

        if (plot != null) {
            ((ClientSubLevel) plot.getSubLevel()).getRenderData().setDirty(x, y, z, important);
            ci.cancel();
        }
    }

    @Inject(method = "extractBlockEntities", at = @At("TAIL"))
    private void sable$extractSubLevelBlockEntities(final Camera camera, final float partialTick, final Long2ObjectMap<SortedSet<BlockDestructionProgress>> progression, final LevelRenderState levelRenderState, final CallbackInfo ci) {
        final var container = SubLevelContainer.getContainer(this.level);
        if (container != null) {
            SubLevelRenderDispatcher.get().extractBlockEntities(container.getAllSubLevels(), camera, partialTick, progression);
        }
    }
}
