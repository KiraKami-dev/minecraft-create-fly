package dev.eriksonn.aeronautics.index;

import com.zurrtum.create.client.content.kinetics.base.ShaftRenderer;
import com.zurrtum.create.client.content.kinetics.base.SingleAxisRotatingVisual;
import dev.eriksonn.aeronautics.content.blocks.hot_air.hot_air_burner.HotAirBurnerRenderer;
import dev.eriksonn.aeronautics.content.blocks.hot_air.steam_vent.SteamVentRenderer;
import dev.eriksonn.aeronautics.content.blocks.mounted_potato_cannon.MountedPotatoCannonRenderer;
import dev.eriksonn.aeronautics.content.blocks.mounted_potato_cannon.MountedPotatoCannonVisual;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.gyroscopic_propeller_bearing.GyroscopicPropellerBearingRenderer;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.gyroscopic_propeller_bearing.GyroscopicPropellerBearingVisual;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.propeller_bearing.PropellerBearingRenderer;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.propeller_bearing.PropellerBearingVisual;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.andesite.AndesitePropellerRenderer;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.andesite.AndesitePropellerVisual;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.smart_propeller.SmartPropellerRenderer;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.wooden.WoodenPropellerRenderer;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.wooden.WoodenPropellerVisual;

import static dev.simulated_team.simulated.index.SimBlockEntityRenders.normal;
import static dev.simulated_team.simulated.index.SimBlockEntityRenders.render;
import static dev.simulated_team.simulated.index.SimBlockEntityRenders.visual;

/**
 * Client: block entity renderers and Flywheel visuals (upstream: Registrate's {@code .renderer()/.visual()}).
 * Registrate's {@code .visual(f)} kept the renderer running ({@code normal}), {@code .visual(f, false)} let the visual
 * replace it while Flywheel is on ({@code visual}).
 */
public class AeroBlockEntityRenders {

    public static void register() {
        render(AeroBlockEntityTypes.STEAM_VENT, SteamVentRenderer::new);
        render(AeroBlockEntityTypes.HOT_AIR_BURNER, HotAirBurnerRenderer::new);
        normal(AeroBlockEntityTypes.PROPELLER_BEARING, PropellerBearingRenderer::new, PropellerBearingVisual::new);
        normal(AeroBlockEntityTypes.GYROSCOPIC_PROPELLER_BEARING, GyroscopicPropellerBearingRenderer::new, GyroscopicPropellerBearingVisual::new);
        visual(AeroBlockEntityTypes.ENVELOPE_ENCASED_SHAFT, ShaftRenderer::new, SingleAxisRotatingVisual::shaft);
        visual(AeroBlockEntityTypes.ANDESITE_PROPELLER, AndesitePropellerRenderer::new, AndesitePropellerVisual::new);
        visual(AeroBlockEntityTypes.WOODEN_PROPELLER, WoodenPropellerRenderer::new, WoodenPropellerVisual::new);
        render(AeroBlockEntityTypes.SMART_PROPELLER, SmartPropellerRenderer::new);
        normal(AeroBlockEntityTypes.MOUNTED_POTATO_CANNON, MountedPotatoCannonRenderer::new, MountedPotatoCannonVisual::new);
    }
}
