package dev.ryanhcode.sable.mixin.world_border;

import dev.ryanhcode.sable.mixinterface.world_border.WorldBorderExtension;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.border.WorldBorder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class ClientLevelMixin {

    @Shadow @Final private WorldBorder worldBorder;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void sable$initializeWorldBorder(final CallbackInfo ci) {
        ((WorldBorderExtension) this.worldBorder).sable$setLevel((ClientLevel) (Object) this);
    }

}
