package dev.ryanhcode.sable.mixin.world_border;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.ryanhcode.sable.mixinterface.world_border.WorldBorderExtension;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.border.WorldBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// PORT(1.21.11): Level no longer owns the world border; ServerLevel fetches it from the data storage on demand
// and ClientLevel owns its own (see ClientLevelMixin), so the level is attached whenever the border is requested.
@Mixin(ServerLevel.class)
public class LevelMixin {

    @ModifyReturnValue(method = "getWorldBorder", at = @At("RETURN"))
    private WorldBorder sable$initializeWorldBorder(final WorldBorder original) {
        ((WorldBorderExtension) original).sable$setLevel((ServerLevel) (Object) this);
        return original;
    }

}
