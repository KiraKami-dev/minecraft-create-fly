package dev.ryanhcode.sable.mixin.sublevel_render.impl.shared;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderData;
import dev.ryanhcode.sable.sublevel.render.dispatcher.SubLevelRenderDispatcher;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = LevelRenderer.class, priority = 1002) // Higher priority to go after Flywheel
public abstract class LevelRendererMixin {

    @Shadow
    private @Nullable ClientLevel level;

    @Unique
    private @Nullable List<ClientSubLevel> sable$getSubLevels() {
        final ClientSubLevelContainer container = SubLevelContainer.getContainer(this.level);
        return container != null ? container.getAllSubLevels() : null;
    }

    @Inject(method = "compileSections", at = @At("TAIL"))
    private void sable$compileSections(final Camera camera, final CallbackInfo ci) {
        final List<ClientSubLevel> sublevels = this.sable$getSubLevels();
        if (sublevels != null) {
            SubLevelRenderDispatcher.get().compileSections(sublevels, new RenderRegionCache(), camera);
        }
    }

    @Inject(method = "cullTerrain", at = @At("TAIL"))
    private void sable$cull(final Camera camera, final Frustum frustum, final boolean isSpectator, final CallbackInfo ci) {
        final List<ClientSubLevel> sublevels = this.sable$getSubLevels();
        if (sublevels == null) {
            return;
        }

        final ProfilerFiller profiler = Profiler.get();
        profiler.push("sub_level_culling");

        final Vec3 cameraPosition = camera.position();
        SubLevelRenderDispatcher.get().updateCulling(sublevels, cameraPosition.x, cameraPosition.y, cameraPosition.z, frustum, isSpectator);

        profiler.pop();
    }

    @Inject(method = "isSectionCompiledAndVisible", at = @At("HEAD"), cancellable = true)
    private void sable$isSectionCompiled(final BlockPos blockPos, final CallbackInfoReturnable<Boolean> cir) {
        final ClientSubLevelContainer container = SubLevelContainer.getContainer(this.level);

        if (container == null) {
            return;
        }

        if (container.inBounds(blockPos)) {
            final ClientSubLevel subLevel = (ClientSubLevel) Sable.HELPER.getContaining(this.level, blockPos);

            if (subLevel == null) {
                cir.setReturnValue(false);
            } else {
                final SubLevelRenderData renderData = subLevel.getRenderData();
                final SectionPos sectionPos = SectionPos.of(blockPos);
                cir.setReturnValue(renderData.isSectionCompiled(sectionPos.x(), sectionPos.y(), sectionPos.z()));
            }
        }
    }

    @Inject(method = "prepareChunkRenders", at = @At("RETURN"))
    private void sable$prepareSubLevelSections(final Matrix4fc modelView, final double x, final double y, final double z, final CallbackInfoReturnable<ChunkSectionsToRender> cir) {
        final List<ClientSubLevel> sublevels = this.sable$getSubLevels();
        if (sublevels != null) {
            SubLevelRenderDispatcher.get().prepareSections(sublevels, modelView, x, y, z);
        }
    }
}
