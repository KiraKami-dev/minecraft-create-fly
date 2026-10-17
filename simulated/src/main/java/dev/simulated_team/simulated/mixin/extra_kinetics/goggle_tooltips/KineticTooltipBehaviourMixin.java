package dev.simulated_team.simulated.mixin.extra_kinetics.goggle_tooltips;

import com.zurrtum.create.client.api.goggles.IHaveGoggleInformation;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.tooltip.KineticTooltipBehaviour;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.tooltip.TooltipBehaviour;
import com.zurrtum.create.content.kinetics.base.KineticBlockEntity;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.util.extra_kinetics.ExtraKinetics;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

// Create Fly moved KineticBlockEntity.addToGoggleTooltip into the client-side KineticTooltipBehaviour, attached per
// block entity type through BlockEntityBehaviour.CLIENT_REGISTRY. The extra kinetics block entity's own tooltip
// behaviour provides its lines (its type needs a KineticTooltipBehaviour registered like any kinetic type).
@Mixin(KineticTooltipBehaviour.class)
public abstract class KineticTooltipBehaviourMixin<T extends KineticBlockEntity> extends TooltipBehaviour<T> {

    public KineticTooltipBehaviourMixin(final T be) {
        super(be);
    }

    @Inject(method = "addToGoggleTooltip", at = @At("RETURN"), cancellable = true, remap = false)
    public void simulated$addExtraKineticsInfo(final List<Component> tooltip, final boolean isPlayerSneaking, final CallbackInfoReturnable<Boolean> cir) {
        if (this.blockEntity instanceof final ExtraKinetics ek) {
            final KineticBlockEntity extraKinetics = ek.getExtraKinetics();
            if (extraKinetics instanceof final ExtraKinetics.ExtraKineticsBlockEntity ekb
                    && extraKinetics.getBehaviour(TooltipBehaviour.TYPE) instanceof final IHaveGoggleInformation extraTooltip) {
                final ArrayList<Component> extraKineticsTooltips = new ArrayList<>();
                final boolean applied = extraTooltip.addToGoggleTooltip(extraKineticsTooltips, isPlayerSneaking);
                if (applied) {
                    if (cir.getReturnValue()) {
                        tooltip.add(Component.empty());
                    }
                    SimLang.translate("extra_kinetics.information", SimLang.builder().add(ekb.getKey()).style(ChatFormatting.AQUA)).style(ChatFormatting.WHITE)
                            .forGoggles(tooltip);
                    tooltip.addAll(extraKineticsTooltips);
                    cir.setReturnValue(true);
                }
            }
        }
    }
}
