package dev.ryanhcode.sable.mixin.entity.entity_rotations_and_riding;

import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.mixinhelpers.entity.entity_riding_sub_level_vehicle.EntityRidingSubLevelVehicleHelper;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityProcessor;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;

@Mixin(EntityType.class)
public class EntityTypeMixin {

    // PORT(1.21.11): passenger loading moved from a lambda in loadEntityRecursive to loadPassengersRecursive (ValueInput)
    @Inject(method = "loadPassengersRecursive", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z"))
    private static void sable$startRidingEntity(final Entity entity,
                                                final ValueInput valueInput,
                                                final Level level,
                                                final EntitySpawnReason entitySpawnReason,
                                                final EntityProcessor entityProcessor,
                                                final CallbackInfoReturnable<Entity> cir,
                                                @Local(ordinal = 1) final Entity newEntity) {
        final SubLevel vehicleSubLevel = Sable.HELPER.getContaining(entity);

        if (vehicleSubLevel != null && EntitySubLevelUtil.shouldKick(newEntity)) {
            final Vec3 pos = EntityRidingSubLevelVehicleHelper.kickRidingEntity(newEntity, vehicleSubLevel);
            newEntity.setPos(pos);
        }
    }
}
