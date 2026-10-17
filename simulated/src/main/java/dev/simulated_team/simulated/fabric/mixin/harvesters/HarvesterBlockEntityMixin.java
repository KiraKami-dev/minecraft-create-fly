package dev.simulated_team.simulated.fabric.mixin.harvesters;

import com.zurrtum.create.content.contraptions.actors.harvester.HarvesterBlockEntity;
import dev.simulated_team.simulated.content.blocks.auger_shaft.BlockHarvester;
import dev.simulated_team.simulated.content.blocks.auger_shaft.auger_groups.AugerDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Stationary harvesters can feed an attached auger. Upstream also wrapped Sable's {@code HarvesterTicker#dropItem}
 * (Sable's Create compatibility, which ticks harvesters placed on sub-levels) to deposit the drops.
 */
// PORT-TODO(1.21.11): SableHarvesterTickerMixin is not ported: Sable's Create harvester compatibility (HarvesterTicker)
//  isn't in this repo yet. Once it lands, wrap HarvesterTicker#dropItem(Level, ItemStack, BlockPos) to call
//  BlockHarvester#depositItemStack and only drop the remainder.
@Mixin(HarvesterBlockEntity.class)
public class HarvesterBlockEntityMixin implements BlockHarvester {

    @Unique
    private AugerDistributor simulated$distributor;

    @Override
    public AugerDistributor simulated$getAssociatedDistributor() {
        return this.simulated$distributor;
    }

    @Override
    public void simulated$setDistributor(final AugerDistributor distributor) {
        this.simulated$distributor = distributor;
    }
}
