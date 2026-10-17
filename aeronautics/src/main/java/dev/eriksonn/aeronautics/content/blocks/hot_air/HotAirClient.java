package dev.eriksonn.aeronautics.content.blocks.hot_air;

import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import dev.eriksonn.aeronautics.content.blocks.hot_air.LiftingGasGoggleInformation.LiftingGasTooltipBehaviour;
import dev.eriksonn.aeronautics.content.blocks.hot_air.hot_air_burner.HotAirBurnerBlockEntity;
import dev.eriksonn.aeronautics.content.blocks.hot_air.hot_air_burner.HotAirBurnerScrollValueBehaviour;
import dev.eriksonn.aeronautics.content.blocks.hot_air.steam_vent.SteamVentBlockEntity;
import dev.eriksonn.aeronautics.index.AeroBlockEntityTypes;

/**
 * Client-only. Attaches the client halves of the hot air burner and steam vent block entities (value boxes and goggle
 * tooltips, which Create Fly reads from client behaviours instead of block entity interfaces).
 */
public final class HotAirClient {
    private HotAirClient() {
    }

    public static void registerClient() {
        BlockEntityBehaviour.addClient(AeroBlockEntityTypes.HOT_AIR_BURNER, HotAirBurnerScrollValueBehaviour::burner);
        BlockEntityBehaviour.addClient(AeroBlockEntityTypes.HOT_AIR_BURNER, (HotAirBurnerBlockEntity be) -> new LiftingGasTooltipBehaviour<>(be));
        BlockEntityBehaviour.addClient(AeroBlockEntityTypes.STEAM_VENT, HotAirBurnerScrollValueBehaviour::steamVent);
        BlockEntityBehaviour.addClient(AeroBlockEntityTypes.STEAM_VENT, (SteamVentBlockEntity be) -> new LiftingGasTooltipBehaviour<>(be));
    }
}
