package dev.ryanhcode.sable.fabric.mixin.compatibility.create.frogports;

import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.AllHandle;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.Position;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Makes the distance check done by Create's {@code PackagePortPlacementPacket} handling take sub-levels into account.
 * <br>
 * Create Fly handles the packet in {@link AllHandle#onPackagePortPlacement} (upstream: {@code PackagePortPlacementPacket#handle}).
 */
@Mixin(AllHandle.class)
public class PackagePortPlacementPacketMixin {

    @Redirect(method = "onPackagePortPlacement", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;closerThan(Lnet/minecraft/core/Position;D)Z", remap = true), remap = false)
    private static boolean sable$handle(final Vec3 instance, final Position position, final double d, @Local(argsOnly = true) final ServerGamePacketListenerImpl listener) {
        return Sable.HELPER.distanceSquaredWithSubLevels(listener.player.level(), instance, position.x(), position.y(), position.z()) < d * d;
    }

}
