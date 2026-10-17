package dev.simulated_team.simulated.content.blocks.altitude_sensor;

import com.zurrtum.create.client.api.goggles.IHaveGoggleInformation;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.tooltip.TooltipBehaviour;
import dev.simulated_team.simulated.data.SimLang;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Goggle tooltip of the altitude sensor (Create Fly moved goggle information from block entities to client behaviours)
 */
public class AltitudeSensorTooltipBehaviour extends TooltipBehaviour<AltitudeSensorBlockEntity> implements IHaveGoggleInformation {

    public AltitudeSensorTooltipBehaviour(final AltitudeSensorBlockEntity be) {
        super(be);
    }

    @Override
    public boolean addToGoggleTooltip(final List<Component> tooltip, final boolean isPlayerSneaking) {
        final AltitudeSensorBlockEntity be = this.blockEntity;
        final float height = be.getWorldHeight();
        final float airPressure = (float) be.getAirPressure() * 100.0f;

        SimLang.blockName(be.getBlockState())
                .forGoggles(tooltip, 1);

        SimLang.translate("altitude_sensor.height", SimLang.text(String.format("%.2f", height)).style(ChatFormatting.AQUA))
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip, 2);

        SimLang.translate("altitude_sensor.air_pressure", SimLang.text(String.format("%.2f%%", airPressure)).style(ChatFormatting.AQUA))
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip, 2);

        be.sendObserved(be.getBlockPos());

        return true;
    }
}
