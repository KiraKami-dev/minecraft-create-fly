package dev.ryanhcode.sable.mixin.tracking_points;

import dev.ryanhcode.sable.mixinterface.tracking_points.LoginPointExtension;
import dev.ryanhcode.sable.sublevel.tracking_points.SubLevelTrackingPointSavedData;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin implements LoginPointExtension {

    @Unique
    private boolean sable$skipRootVehicle = false;

    @Shadow
    public abstract ServerLevel level();

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void addAdditionalSaveData(final ValueOutput valueOutput, final CallbackInfo ci) {
        final SubLevelTrackingPointSavedData data = SubLevelTrackingPointSavedData.getOrLoad(this.level());
        final UUID loginPointUUID = data.generateTrackingPoint((ServerPlayer) (Object) this);
        if (loginPointUUID != null) {
            valueOutput.store("LoginPoint", UUIDUtil.CODEC, loginPointUUID);
        }
    }

    @Override
    public void sable$skipRootVehicle() {
        this.sable$skipRootVehicle = true;
    }

    /**
     * Since 1.21.11 the saved vehicle is re-attached by a separate call with the player's saved data, which is read only.
     * When the player was moved by a login point they must not be put back on their old vehicle.
     */
    @Inject(method = "loadAndSpawnParentVehicle", at = @At("HEAD"), cancellable = true)
    private void sable$skipParentVehicle(final ValueInput valueInput, final CallbackInfo ci) {
        if (this.sable$skipRootVehicle) {
            this.sable$skipRootVehicle = false;
            ci.cancel();
        }
    }

}
