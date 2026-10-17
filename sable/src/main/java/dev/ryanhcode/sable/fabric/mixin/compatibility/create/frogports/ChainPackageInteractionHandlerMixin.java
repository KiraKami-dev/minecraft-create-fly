package dev.ryanhcode.sable.fabric.mixin.compatibility.create.frogports;

import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.client.content.kinetics.chainConveyor.ChainConveyorPackagePhysicsData;
import com.zurrtum.create.client.content.kinetics.chainConveyor.ChainPackageInteractionHandler;
import com.zurrtum.create.client.foundation.utility.RaycastHelper;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Traces against chain conveyor packages in the frame of the sub-level they are on.
 * <br>
 * Create Fly inlined the upstream {@code forEach} lambda ({@code lambda$onUse$0}) into {@code onUse} itself.
 */
@Mixin(ChainPackageInteractionHandler.class)
public class ChainPackageInteractionHandlerMixin {

    @Redirect(method = "onUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getEyePosition()Lnet/minecraft/world/phys/Vec3;", remap = true), remap = false)
    private static Vec3 sable$getTraceOrigin(final LocalPlayer instance, @Local final ChainConveyorPackagePhysicsData data) {
        Vec3 origin = instance.getEyePosition();

        final SubLevel subLevel = Sable.HELPER.getContainingClient(data.targetPos);
        if (subLevel != null) {
            origin = subLevel.logicalPose().transformPositionInverse(origin);
        }

        return origin;
    }

    @Redirect(method = "onUse", at = @At(value = "INVOKE", target = "Lcom/zurrtum/create/client/foundation/utility/RaycastHelper;getTraceTarget(Lnet/minecraft/world/entity/player/Player;DLnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"), remap = false)
    private static Vec3 sable$getTraceTarget(final Player playerIn, final double range, final Vec3 from, @Local final ChainConveyorPackagePhysicsData data) {
        Vec3 target = RaycastHelper.getTraceTarget(playerIn, range, playerIn.getEyePosition());

        final SubLevel subLevel = Sable.HELPER.getContainingClient(data.targetPos);
        if (subLevel != null) {
            target = subLevel.logicalPose().transformPositionInverse(target);
        }

        return target;
    }

}
