package dev.ryanhcode.sable.mixin.sublevel_render;

import com.mojang.blaze3d.vertex.VertexSorting;
import dev.ryanhcode.sable.mixinterface.sublevel_render.vanilla.RenderSectionExtension;
import dev.ryanhcode.sable.veillite.api.client.render.VeilRenderSystem;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

/**
 * Lets sub-level render data listen for dirty sections, and sorts sub-level translucency from the camera position in plot
 * space.
 */
@Mixin(SectionRenderDispatcher.RenderSection.class)
public class RenderSectionMixin implements RenderSectionExtension {

    @Shadow
    private boolean dirty;

    @Unique
    private Set<DirtyListener> sable$listeners;
    @Unique
    private boolean sable$listening = true;
    @Unique
    private volatile @Nullable Vec3 sable$sortCameraPosition;

    @Inject(method = "setDirty", at = @At("HEAD"))
    public void setDirty(final boolean playerChanged, final CallbackInfo ci) {
        if (this.sable$listening && !this.dirty && this.sable$listeners != null) {
            VeilRenderSystem.renderThreadExecutor().execute(() -> {
                for (final DirtyListener listener : this.sable$listeners) {
                    listener.markDirty((SectionRenderDispatcher.RenderSection) (Object) this);
                }
            });
        }
    }

    @Inject(method = "createVertexSorting", at = @At("HEAD"), cancellable = true)
    private void sable$createVertexSorting(final SectionPos sectionPos, final CallbackInfoReturnable<VertexSorting> cir) {
        final Vec3 camera = this.sable$sortCameraPosition;
        if (camera != null) {
            cir.setReturnValue(VertexSorting.byDistance((float) (camera.x - sectionPos.minBlockX()), (float) (camera.y - sectionPos.minBlockY()), (float) (camera.z - sectionPos.minBlockZ())));
        }
    }

    @Override
    public void sable$addDirtyListener(final DirtyListener listener) {
        if (this.sable$listeners == null) {
            this.sable$listeners = new ObjectArraySet<>();
        }
        this.sable$listeners.add(listener);
    }

    @Override
    public void sable$setListening(final boolean listening) {
        this.sable$listening = listening;
    }

    @Override
    public void sable$setSortCameraPosition(@Nullable final Vec3 position) {
        this.sable$sortCameraPosition = position;
    }

    @Override
    public @Nullable Vec3 sable$getSortCameraPosition() {
        return this.sable$sortCameraPosition;
    }
}
