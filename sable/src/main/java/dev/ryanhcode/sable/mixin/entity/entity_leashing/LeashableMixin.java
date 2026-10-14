package dev.ryanhcode.sable.mixin.entity.entity_leashing;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Takes sub-levels into account for leash physics.
 * <p>
 * PORT(1.21.11): the legacy elastic behaviour this used to overwrite was replaced by spring/wrench physics in
 * {@code computeElasticInteraction} + {@code checkElasticInteractions}; the same projection is applied there.
 */
@Mixin(Leashable.class)
public interface LeashableMixin {

    /**
     * Compute the spring interaction between the global positions of the leashed entity and the holder
     */
    @WrapOperation(method = "computeElasticInteraction", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;position()Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 sable$projectPosition(final Entity instance, final Operation<Vec3> original) {
        final ActiveSableCompanion helper = Sable.HELPER;
        return helper.projectOutOfSubLevel(instance.level(), original.call(instance));
    }

    /**
     * The resulting (global) force is applied in the leashed entity's sub-level space
     */
    @WrapOperation(method = "checkElasticInteractions", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;addDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
    private static void sable$transformImpulse(final Entity instance, final Vec3 impulse, final Operation<Void> original) {
        final SubLevel leashedSubLevel = Sable.HELPER.getContaining(instance);
        original.call(instance, leashedSubLevel != null ? leashedSubLevel.logicalPose().transformNormalInverse(impulse) : impulse);
    }
}
