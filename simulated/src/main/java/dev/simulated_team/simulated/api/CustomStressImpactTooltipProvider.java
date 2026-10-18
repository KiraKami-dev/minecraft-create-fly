package dev.simulated_team.simulated.api;

import net.minecraft.network.chat.Component;

public interface CustomStressImpactTooltipProvider {

    /**
     * Returns a plain {@link Component} rather than a LangBuilder: LangBuilder is client-only in Create Fly,
     * and implementing blocks must stay reflectable on a dedicated server (Lithium scans block methods).
     *
     * @return The lang associated with the tooltip. IE 2 x RPM x 'Component.translatable("aeronautics.propeller.sails")'
     */
    Component getCustomImpactLang();

    /**
     * @return How many bar segments there should be in the tooltip
     */
    int getBarLength();

    /**
     * @return How many bar segments should be filled in the tooltip
     */
    int getFilledBarLength();

}
