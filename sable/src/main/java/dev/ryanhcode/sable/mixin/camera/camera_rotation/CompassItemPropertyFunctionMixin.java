package dev.ryanhcode.sable.mixin.camera.camera_rotation;

import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.renderer.item.properties.numeric.CompassAngleState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(CompassAngleState.class)
public abstract class CompassItemPropertyFunctionMixin {

    /**
     * @author RyanH
     * @reason Take into account sub-levels
     */
    @Overwrite
    private static double getAngleFromEntityToPos(final ItemOwner itemOwner, final BlockPos pos) {
        Vec3 localPos = Vec3.atCenterOf(pos);
        final Vec3 ownerPos = itemOwner.position();
        double entityX = ownerPos.x;
        double entityZ = ownerPos.z;
        final Entity entity = itemOwner instanceof Entity e ? e : null;

        final ActiveSableCompanion helper = Sable.HELPER;
        SubLevel subLevel = entity != null ? helper.getContaining(entity) : helper.getContaining(itemOwner.level(), ownerPos);

        if (subLevel == null) {
            final Entity vehicle = entity != null ? entity.getVehicle() : null;

            if (vehicle != null) {
                subLevel = helper.getContaining(vehicle);

                if (subLevel != null) {
                    final Vec3 localEntityPos = subLevel.lastPose().transformPositionInverse(ownerPos);
                    entityX = localEntityPos.x;
                    entityZ = localEntityPos.z;
                }
            }
        }

        if (subLevel != null) {
            localPos = subLevel.lastPose().transformPositionInverse(localPos);
        }

        return Math.atan2(localPos.z() - entityZ, localPos.x() - entityX) / (float) (Math.PI * 2);
    }

}
