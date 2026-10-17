package dev.eriksonn.aeronautics.content.blocks.hot_air.lifting_gas;

import dev.eriksonn.aeronautics.config.AeroConfig;
import dev.eriksonn.aeronautics.Aeronautics;
import net.minecraft.network.chat.Component;

public class DefaultLiftingGas implements LiftingGasType {
    @Override
    public Component getName() {
        return Component.translatable(Aeronautics.MOD_ID + ".lifting_gas.default");
    }

    @Override
    public double getFillingTime() {
        return 9.0 * 20;
    }

    @Override
    public double getEmptyingTime() {
        return 9.0 * 20;
    }

    @Override
    public double getLiftStrength() {
        return AeroConfig.server().physics.hotAirStrength.get();
    }

    @Override
    public double getResponsivenessAdjustmentFactor() {
        return 5;
    }

    @Override
    public double getResponsivenessAdjustmentRange() {
        return 0.05;
    }
}
