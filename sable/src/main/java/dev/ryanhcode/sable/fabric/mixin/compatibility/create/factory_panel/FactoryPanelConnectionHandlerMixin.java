package dev.ryanhcode.sable.fabric.mixin.compatibility.create.factory_panel;

import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.client.content.logistics.factoryBoard.FactoryPanelConnectionHandler;
import dev.ryanhcode.sable.Sable;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FactoryPanelConnectionHandler.class)
public class FactoryPanelConnectionHandlerMixin {

    @Redirect(method = "clientTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;closerThan(Lnet/minecraft/core/Vec3i;D)Z", remap = true), remap = false)
    private static boolean sable$closerThan(final BlockPos instance, final Vec3i pos, final double maxDistance, @Local(argsOnly = true) final Minecraft mc) {
        return Sable.HELPER.distanceSquaredWithSubLevels(mc.level, instance.getX(), instance.getY(), instance.getZ(), pos.getX(), pos.getY(), pos.getZ()) < maxDistance * maxDistance;
    }
}
