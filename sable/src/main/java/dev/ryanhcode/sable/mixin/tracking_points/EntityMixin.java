package dev.ryanhcode.sable.mixin.tracking_points;

import dev.ryanhcode.sable.mixinterface.player_freezing.PlayerFreezeExtension;
import dev.ryanhcode.sable.sublevel.tracking_points.SubLevelTrackingPointSavedData;
import dev.ryanhcode.sable.mixinterface.tracking_points.LoginPointExtension;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import org.joml.Vector3dc;
import java.util.UUID;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow private Level level;

    @Shadow public abstract void setPosRaw(double d, double e, double f);

    @Inject(method = "load", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setPosRaw(DDD)V", shift = At.Shift.AFTER))
    private void sable$load(final ValueInput valueInput, final CallbackInfo ci) {
        final UUID loginPoint = valueInput.read("LoginPoint", UUIDUtil.CODEC).orElse(null);
        if (loginPoint != null) {
            final SubLevelTrackingPointSavedData data = SubLevelTrackingPointSavedData.getOrLoad((ServerLevel) this.level);
            final SubLevelTrackingPointSavedData.TakenLoginPoint point = data.take(loginPoint, true);

            if (point != null) {
                final Vector3dc position = point.position();
                this.setPosRaw(position.x(), position.y(), position.z());

                if (point.subLevelId() != null && this instanceof final PlayerFreezeExtension extension) {
                    extension.sable$freezeTo(point.subLevelId(), point.localAnchor().add(0.0, 0.2, 0.0));
                }

                // kick them out of vehicle
                if (this instanceof final LoginPointExtension extension) {
                    extension.sable$skipRootVehicle();
                }
            }
        }
    }
}
