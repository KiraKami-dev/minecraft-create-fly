package dev.simulated_team.simulated.content.blocks.gimbal_sensor;

import com.zurrtum.create.client.api.goggles.IHaveGoggleInformation;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.tooltip.TooltipBehaviour;
import dev.simulated_team.simulated.data.SimLang;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Goggle tooltip of the gimbal sensor (Create Fly moved goggle information from block entities to client behaviours)
 */
public class GimbalSensorTooltipBehaviour extends TooltipBehaviour<GimbalSensorBlockEntity> implements IHaveGoggleInformation {

    public GimbalSensorTooltipBehaviour(final GimbalSensorBlockEntity be) {
        super(be);
    }

    @Override
    public boolean addToGoggleTooltip(final List<Component> tooltip, final boolean isPlayerSneaking) {
        final Component x = SimLang.text("%.2f".formatted(Math.toDegrees(this.blockEntity.getXAngle())))
                .style(ChatFormatting.RED)
                .component();
        final Component z = SimLang.text("%.2f".formatted(Math.toDegrees(this.blockEntity.getZAngle())))
                .style(ChatFormatting.BLUE)
                .component();

        SimLang.blockName(this.blockEntity.getBlockState())
                .forGoggles(tooltip, 1);
        SimLang.translate("gimbal_sensor.x_angle", x)
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip, 2);
        SimLang.translate("gimbal_sensor.z_angle", z)
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip, 2);

        return true;
    }
}
