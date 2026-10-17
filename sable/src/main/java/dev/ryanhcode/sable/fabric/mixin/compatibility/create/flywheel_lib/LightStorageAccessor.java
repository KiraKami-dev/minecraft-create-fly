package dev.ryanhcode.sable.fabric.mixin.compatibility.create.flywheel_lib;

import com.zurrtum.create.client.flywheel.backend.engine.LightDataCollector;
import com.zurrtum.create.client.flywheel.backend.engine.LightStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LightStorage.class)
public interface LightStorageAccessor {

    @Accessor(remap = false)
    LightDataCollector getCollector();

    @Accessor(remap = false)
    void setNeedsLutRebuild(boolean needsLutRebuild);

}
