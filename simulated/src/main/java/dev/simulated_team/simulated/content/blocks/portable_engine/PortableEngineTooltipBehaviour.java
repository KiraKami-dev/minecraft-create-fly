package dev.simulated_team.simulated.content.blocks.portable_engine;

import com.zurrtum.create.client.catnip.lang.LangBuilder;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.tooltip.GeneratingKineticTooltipBehaviour;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.service.SimItemService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Client behaviour: goggle information of the portable engine (upstream PortableEngineBlockEntity#addToGoggleTooltip).
 */
public class PortableEngineTooltipBehaviour extends GeneratingKineticTooltipBehaviour<PortableEngineBlockEntity> {
    public PortableEngineTooltipBehaviour(final PortableEngineBlockEntity be) {
        super(be);
    }

    @Override
    public boolean addToGoggleTooltip(final List<Component> tooltip, final boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        final PortableEngineBlockEntity be = this.blockEntity;

        SimLang.translate("portable_engine.tooltip_name").text(":").forGoggles(tooltip);

        final ItemStack currentStack = be.inventory.slot.getStack();
        final boolean hasByProduct = !currentStack.isEmpty() && SimItemService.INSTANCE.getBurnTime(currentStack) == 0;

        final LangBuilder noFuel = SimLang.translate("portable_engine.none").style(ChatFormatting.RED);
        final LangBuilder stackName = SimLang.builder().add(currentStack.getHoverName())
                .text(" x" + currentStack.getCount())
                .style(ChatFormatting.GREEN);

        if (!be.isCurrentFuelInfinite()) {
            final String langKey = hasByProduct ? "byproduct" : "fuel";
            SimLang.translate("portable_engine." + langKey, currentStack.isEmpty() ? noFuel : stackName)
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip);
        }

        if (be.getCurrentBurnTime() > 0) {
            final int seconds = be.getCurrentBurnTime() / 20;
            final int secondsTotal = be.getTotalBurnTime() / 20;

            final LangBuilder infiniteLang = SimLang.translate("portable_engine.infinite")
                    .style(ChatFormatting.LIGHT_PURPLE);
            final LangBuilder timeLang = SimLang.text(PortableEngineBlockEntity.getTime(secondsTotal))
                    .style(be.isSuperHeated() ? ChatFormatting.GOLD : ChatFormatting.AQUA);

            SimLang.translate("portable_engine.time", be.isTotalFuelInfinite() ? infiniteLang : timeLang)
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip);

            if (be.isSuperHeated()) {
                if (be.isCurrentFuelInfinite()) {
                    SimLang.translate("portable_engine.superheated")
                            .style(ChatFormatting.GOLD)
                            .forGoggles(tooltip);
                } else {
                    SimLang.translate("portable_engine.superheated_time", PortableEngineBlockEntity.getTime(be.getNextSuperHeated() ? secondsTotal : seconds))
                            .style(ChatFormatting.GOLD)
                            .forGoggles(tooltip);
                }
            }
        }
        return true;
    }
}
