package dev.simulated_team.simulated.content.blocks.velocity_sensor;

import com.zurrtum.create.client.api.goggles.IHaveGoggleInformation;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.tooltip.TooltipBehaviour;
import dev.simulated_team.simulated.data.SimLang;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Goggle tooltip of the velocity sensor (Create Fly moved goggle information from block entities to client behaviours)
 */
public class VelocitySensorTooltipBehaviour extends TooltipBehaviour<VelocitySensorBlockEntity> implements IHaveGoggleInformation {

    public VelocitySensorTooltipBehaviour(final VelocitySensorBlockEntity be) {
        super(be);
    }

    @Override
    public boolean addToGoggleTooltip(final List<Component> tooltip, final boolean isPlayerSneaking) {
        if (this.blockEntity.isOnSubLevel()) {
            SimLang.number(Math.abs(this.blockEntity.getAdjustedVelocity()))
                    .text(" m/s").forGoggles(tooltip);
        }

        return IHaveGoggleInformation.super.addToGoggleTooltip(tooltip, isPlayerSneaking);
    }
}
