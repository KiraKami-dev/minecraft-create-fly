package dev.ryanhcode.sable.fabric.mixin.compatibility.create.flywheel_lib;

import com.zurrtum.create.client.flywheel.backend.engine.DrawManager;
import com.zurrtum.create.client.flywheel.backend.engine.EngineImpl;
import com.zurrtum.create.client.flywheel.backend.engine.LightStorage;
import dev.ryanhcode.sable.fabric.compatibility.create.flywheel.SableFlywheelLightStorage;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EngineImpl.class)
public class EngineImplMixin {

    @Shadow(remap = false)
    @Final
    @Mutable
    private LightStorage lightStorage;

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void sable$replaceLightStorage(final LevelAccessor level, final DrawManager<?> drawManager, final int maxOriginDistance, final CallbackInfo ci) {
        this.lightStorage.delete();
        this.lightStorage = new SableFlywheelLightStorage(level);
    }
}
