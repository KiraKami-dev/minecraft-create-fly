package dev.simulated_team.simulated.content.blocks.analog_transmission;

import com.zurrtum.create.client.catnip.lang.FontHelper;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.tooltip.KineticTooltipBehaviour;
import com.zurrtum.create.client.foundation.item.TooltipHelper;
import dev.simulated_team.simulated.data.SimLang;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

import static net.minecraft.ChatFormatting.GOLD;

/**
 * Client behaviour: the "too fast" hover hint of the analog transmission (upstream AnalogTransmissionBlockEntity#addToTooltip).
 */
public class AnalogTransmissionTooltipBehaviour extends KineticTooltipBehaviour<AnalogTransmissionBlockEntity> {
    public AnalogTransmissionTooltipBehaviour(final AnalogTransmissionBlockEntity be) {
        super(be);
    }

    @Override
    public boolean addToTooltip(final List<Component> tooltip, final boolean isPlayerSneaking) {
        if (this.blockEntity.isOversaturated()) {
            SimLang.translate("analog_transmission.too_fast")
                    .style(GOLD)
                    .forGoggles(tooltip);

            final MutableComponent component = SimLang.translate("analog_transmission.too_fast_error")
                    .component();

            final List<Component> cutString = TooltipHelper.cutTextComponent(component, FontHelper.Palette.GRAY_AND_WHITE);
            tooltip.addAll(cutString);

            return true;
        }

        return super.addToTooltip(tooltip, isPlayerSneaking);
    }
}
