package dev.ryanhcode.sable.mixin.sublevel_render;

import dev.ryanhcode.sable.mixinterface.sublevel_render.vanilla.RenderSectionExtension;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Records the translucency point of view of sub-level sections from the camera position in plot space.
 */
@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection$RebuildTask")
public class RebuildTaskMixin {

    @Shadow
    @Final
    SectionRenderDispatcher.RenderSection field_20839;

    @ModifyArg(method = "doTask", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/TranslucencyPointOfView;of(Lnet/minecraft/world/phys/Vec3;J)Lnet/minecraft/client/renderer/chunk/TranslucencyPointOfView;"), index = 0)
    private Vec3 sable$sortCamera(final Vec3 camera) {
        final Vec3 sortCamera = ((RenderSectionExtension) this.field_20839).sable$getSortCameraPosition();
        return sortCamera != null ? sortCamera : camera;
    }
}
