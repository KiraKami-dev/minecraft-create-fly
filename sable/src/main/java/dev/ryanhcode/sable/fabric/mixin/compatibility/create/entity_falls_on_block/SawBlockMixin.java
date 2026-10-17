package dev.ryanhcode.sable.fabric.mixin.compatibility.create.entity_falls_on_block;

import com.zurrtum.create.content.kinetics.saw.SawBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SawBlock.class)
public class SawBlockMixin {

    @Redirect(method = "updateEntityMovementAfterFallOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;blockPosition()Lnet/minecraft/core/BlockPos;"))
    private BlockPos sable$updateEntityAfterFallOn(final Entity instance) {
        return instance.getOnPos();
    }

}
