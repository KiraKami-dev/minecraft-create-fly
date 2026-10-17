package dev.ryanhcode.sable.fabric.mixin.compatibility.create.impact;

import com.zurrtum.create.content.equipment.bell.AbstractBellBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractBellBlock.class)
public interface AbstractBellBlockAccessor {

    @Invoker(value = "ring", remap = false)
    boolean invokeRing(Level world, BlockPos pos, Direction direction, Player player);

}
