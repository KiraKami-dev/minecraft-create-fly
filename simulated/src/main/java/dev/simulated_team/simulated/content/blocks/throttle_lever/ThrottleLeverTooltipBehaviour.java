package dev.simulated_team.simulated.content.blocks.throttle_lever;

import com.zurrtum.create.client.api.goggles.IHaveGoggleInformation;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.tooltip.TooltipBehaviour;
import com.zurrtum.create.client.foundation.utility.CreateLang;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Client behaviour: goggle information of the throttle lever (upstream ThrottleLeverBlockEntity#addToGoggleTooltip).
 */
public class ThrottleLeverTooltipBehaviour extends TooltipBehaviour<ThrottleLeverBlockEntity> implements IHaveGoggleInformation {
    public ThrottleLeverTooltipBehaviour(final ThrottleLeverBlockEntity be) {
        super(be);
    }

    @Override
    public boolean addToGoggleTooltip(final List<Component> tooltip, final boolean isPlayerSneaking) {
        CreateLang.builder().add(CreateLang.translateDirect("tooltip.analogStrength", this.blockEntity.getState())).forGoggles(tooltip);
        return true;
    }
}
