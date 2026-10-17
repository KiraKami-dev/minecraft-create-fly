package dev.ryanhcode.sable.fabric.mixin.compatibility.create.super_glue;

import com.zurrtum.create.AllHandle;
import dev.ryanhcode.sable.Sable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Fixes the range check in Create's {@code SuperGlueRemovalPacket} handling to account for sub-levels.
 * Create Fly handles the packet in {@link AllHandle#onSuperGlueRemoval}.
 */
@Mixin(AllHandle.class)
public class SuperGlueRemovalPacketMixin {

    @Redirect(method = "onSuperGlueRemoval", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D", remap = true), remap = false)
    private static double sable$distanceSquared(final ServerPlayer instance, final Vec3 vec3) {
        return Sable.HELPER.distanceSquaredWithSubLevels(instance.level(), instance.position(), vec3);
    }
}
