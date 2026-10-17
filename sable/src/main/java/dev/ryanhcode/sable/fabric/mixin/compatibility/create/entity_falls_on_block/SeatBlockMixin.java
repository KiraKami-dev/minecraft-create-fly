package dev.ryanhcode.sable.fabric.mixin.compatibility.create.entity_falls_on_block;

import com.zurrtum.create.content.contraptions.actors.seat.SeatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Makes seats use the standing on position of entities instead of their block position for sitting them down, as the
 * on position of entities will be overwritten by Sable to be inside of the plot of a sub-level an entity is resting on
 */
@Mixin(SeatBlock.class)
public class SeatBlockMixin {

    @Redirect(method = "updateEntityMovementAfterFallOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;blockPosition()Lnet/minecraft/core/BlockPos;"))
    private BlockPos sable$updateEntityAfterFallOn(final Entity instance) {
        return instance.getOnPos();
    }

}
