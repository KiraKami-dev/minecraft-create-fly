package dev.ryanhcode.sable.mixin.sublevel_render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.Sable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.CompileTaskDynamicQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Fixes the distance used to prioritize section compiles to take sub-levels into account
 */
@Mixin(CompileTaskDynamicQueue.class)
public class CompileTaskDynamicQueueMixin {

    @WrapOperation(method = "poll", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;distToCenterSqr(Lnet/minecraft/core/Position;)D"))
    private double sable$distanceWithSubLevels(final BlockPos origin, final Position camera, final Operation<Double> original) {
        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return original.call(origin, camera);
        }

        return Sable.HELPER.distanceSquaredWithSubLevels(level, camera, origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5);
    }
}
