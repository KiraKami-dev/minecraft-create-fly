package dev.simulated_team.simulated.mixin.end_sea;

import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderPipelines;
import dev.ryanhcode.sable.sublevel.render.dispatcher.VanillaSubLevelRenderDispatcher;
import dev.simulated_team.simulated.content.end_sea.EndSeaPhysics;
import dev.simulated_team.simulated.content.end_sea.EndSeaPhysicsData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DynamicUniformStorage;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Passes the camera height above the end sea to the sub-level terrain shader, for the fade patched in by
 * {@link dev.simulated_team.simulated.content.end_sea.EndSeaFadeTransformer}. Upstream set the {@code EndSeaCameraY}
 * uniform in {@code setupDynamicEffects}; 1.21.11 Sable writes one uniform block per sub-level instead, and its
 * {@code SubLevelParams.w} component is unused.
 */
@Mixin(VanillaSubLevelRenderDispatcher.class)
public class VanillaSubLevelRenderDispatcherMixin {

    @ModifyArg(method = "prepareSections", remap = false, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/DynamicUniformStorage;writeUniform(Lnet/minecraft/client/renderer/DynamicUniformStorage$DynamicUniform;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;", remap = true))
    private DynamicUniformStorage.DynamicUniform simulated$setEndSeaCameraY(final DynamicUniformStorage.DynamicUniform uniform, @Local(argsOnly = true, ordinal = 1) final double cameraY) {
        if (!(uniform instanceof final SubLevelRenderPipelines.SubLevelUniform subLevelUniform)) {
            return uniform;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        final EndSeaPhysics physics = minecraft.level != null ? EndSeaPhysicsData.of(minecraft.level) : null;

        if (physics == null) {
            return uniform;
        }

        final Vector4f params = new Vector4f(subLevelUniform.params());
        params.w = (float) (cameraY - physics.startY());
        return new SubLevelRenderPipelines.SubLevelUniform(subLevelUniform.pose(), subLevelUniform.faceShadeA(), subLevelUniform.faceShadeB(), params);
    }

}
