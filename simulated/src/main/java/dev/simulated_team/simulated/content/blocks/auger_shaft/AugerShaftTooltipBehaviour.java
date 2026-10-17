package dev.simulated_team.simulated.content.blocks.auger_shaft;

import com.zurrtum.create.client.foundation.blockEntity.behaviour.tooltip.KineticTooltipBehaviour;
import com.zurrtum.create.client.foundation.utility.CreateLang;
import dev.simulated_team.simulated.data.SimLang;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Client behaviour: goggle information of the auger shaft (upstream AugerShaftBlockEntity#addToGoggleTooltip).
 */
public class AugerShaftTooltipBehaviour extends KineticTooltipBehaviour<AugerShaftBlockEntity> {
    public AugerShaftTooltipBehaviour(final AugerShaftBlockEntity be) {
        super(be);
    }

    @Override
    public boolean addToGoggleTooltip(final List<Component> tooltip, final boolean isPlayerSneaking) {
        final AugerShaftBlockEntity be = this.blockEntity;
        boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        if (be.getSpeed() > 0) {
            final float perTickSpeed = be.getItemSpeed();
            final float perSecondSpeed = perTickSpeed * 20;
            SimLang.translate("auger_shaft.item_flow", Math.floor(perSecondSpeed * 100) / 100)
                    .style(ChatFormatting.YELLOW)
                    .forGoggles(tooltip);
            added = true;
        }

        final int actorItems = be.actorInventory.storedItemCount;
        if (actorItems > 0) {
            SimLang.translate("auger_shaft.actor_items", actorItems)
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip);
            added = true;
        }

        final ItemStack stack = be.inventory.slot.getStack();
        final int count = stack.getCount();
        if (count > 0) {
            CreateLang.translate("tooltip.chute.contains", stack.getHoverName().getString(), count)
                    .style(ChatFormatting.GREEN)
                    .forGoggles(tooltip);
            added = true;
        }

        be.markObserved();
        return added;
    }
}
