package dev.ryanhcode.sable.fabric.mixin.compatibility.create.nozzle;

import com.zurrtum.create.content.kinetics.fan.NozzleBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(NozzleBlockEntity.class)
public interface NozzleBlockEntityAccessor {

    @Accessor(value = "range", remap = false)
    float getRange();

}
