package dev.eriksonn.aeronautics.index;

import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.content.blocks.hot_air.lifting_gas.DefaultLiftingGas;
import dev.eriksonn.aeronautics.content.blocks.hot_air.lifting_gas.SteamLiftingGas;
import dev.eriksonn.aeronautics.registry.AeroRegistrate;

public class AeroLiftingGasTypes {

    public static final AeroRegistrate REGISTRATE = Aeronautics.getRegistrate();

    public static final DefaultLiftingGas DEFAULT_GAS = REGISTRATE.liftingGasType("default", DefaultLiftingGas::new);
    public static final SteamLiftingGas STEAM = REGISTRATE.liftingGasType("steam", SteamLiftingGas::new);

    public static void register() {
    }
}
