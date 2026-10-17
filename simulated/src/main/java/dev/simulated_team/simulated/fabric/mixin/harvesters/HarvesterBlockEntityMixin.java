package dev.simulated_team.simulated.fabric.mixin.harvesters;

import com.zurrtum.create.content.contraptions.actors.harvester.HarvesterBlockEntity;
import dev.simulated_team.simulated.content.blocks.auger_shaft.BlockHarvester;
import dev.simulated_team.simulated.content.blocks.auger_shaft.auger_groups.AugerDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Stationary harvesters can feed an attached auger; their drops go through {@link HarvesterTickerMixin}.
 */
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
