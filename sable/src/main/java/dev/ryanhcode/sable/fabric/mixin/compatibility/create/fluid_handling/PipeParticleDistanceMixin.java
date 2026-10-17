package dev.ryanhcode.sable.fabric.mixin.compatibility.create.fluid_handling;

import com.zurrtum.create.client.AllHandle;
import dev.ryanhcode.sable.Sable;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Takes sub-levels into account for the pipe particle render distance check.
 * Create Fly moved {@code PipeConnection#isRenderEntityWithinDistance} to the client-side
 * {@code AllHandle#isRenderEntityWithoutDistance} (same distance comparison, inverted result).
 */
@Mixin(AllHandle.class)
public class PipeParticleDistanceMixin {
    @Redirect(method = "isRenderEntityWithoutDistance", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;distanceTo(Lnet/minecraft/world/phys/Vec3;)D", remap = true), remap = false)
    private static double sable$distanceIncludingSubLevels(final Vec3 instance, final Vec3 vec3) {
        return Math.sqrt(Sable.HELPER.distanceSquaredWithSubLevels(Minecraft.getInstance().level, instance, vec3));
    }
}
