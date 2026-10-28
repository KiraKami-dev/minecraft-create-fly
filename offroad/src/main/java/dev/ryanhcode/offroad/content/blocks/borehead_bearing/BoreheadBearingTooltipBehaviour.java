package dev.ryanhcode.offroad.content.blocks.borehead_bearing;

import com.zurrtum.create.client.catnip.lang.FontHelper;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.tooltip.MechanicalBearingTooltipBehaviour;
import com.zurrtum.create.client.foundation.item.TooltipHelper;
import com.zurrtum.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import dev.ryanhcode.offroad.data.OffroadLang;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Client goggle tooltip of the borehead bearing (upstream: {@code BoreheadBearingBlockEntity#addExceptionToTooltip}):
 * the mechanical bearing's tooltip, plus a warning while the drill is stalled by a full contraption inventory.
 */
public class BoreheadBearingTooltipBehaviour extends MechanicalBearingTooltipBehaviour {

    public BoreheadBearingTooltipBehaviour(final MechanicalBearingBlockEntity be) {
        super(be);
    }

    @Override
    public boolean addExceptionToTooltip(final List<Component> tooltip) {
        final boolean original = super.addExceptionToTooltip(tooltip);

        if (!original && this.blockEntity instanceof final BoreheadBearingBlockEntity borehead && borehead.isStalled()) {
            if (!tooltip.isEmpty()) {
                tooltip.add(CommonComponents.EMPTY);
            }

            OffroadLang.translate("exceptions.borehead_bearing.too_many_items")
                    .style(ChatFormatting.GOLD)
                    .forGoggles(tooltip);

            Arrays.stream(OffroadLang.translate("exceptions.borehead_bearing.too_many_items_description").component().getString().split("\n"))
                    .forEach(l -> TooltipHelper.cutStringTextComponent(l, FontHelper.Palette.GRAY_AND_WHITE)
                            .forEach(c -> OffroadLang.builder().add(c).forGoggles(tooltip)));

            return true;
        }

        return original;
    }
}
