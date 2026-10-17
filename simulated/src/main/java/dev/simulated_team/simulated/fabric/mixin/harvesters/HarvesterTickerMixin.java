package dev.simulated_team.simulated.fabric.mixin.harvesters;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ryanhcode.sable.fabric.mixinhelper.compatibility.create.harvester.HarvesterTicker;
import dev.simulated_team.simulated.content.blocks.auger_shaft.BlockHarvester;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Harvesters ticked as block entities by Sable's Create compatibility hand their drops to an attached auger first
 * (upstream {@code SableHarvesterTickerMixin}).
 */
@Mixin(HarvesterTicker.class)
public class HarvesterTickerMixin {

    @WrapMethod(method = "dropItem", remap = false)
    private static void simulated$deferDrop(final Level level, final ItemStack dropped, final BlockPos selfPos, final Operation<Void> original) {
        final BlockEntity be = selfPos != null ? level.getBlockEntity(selfPos) : null;
        if (be instanceof final BlockHarvester harvester) {
            final ItemStack remainder = harvester.depositItemStack(selfPos, dropped);
            if (!remainder.isEmpty()) {
                original.call(level, remainder, selfPos);
            }
            return;
        }

        // upstream dropped nothing here; harvesters always are BlockHarvesters, so only keep the vanilla drop
        original.call(level, dropped, selfPos);
    }
}
