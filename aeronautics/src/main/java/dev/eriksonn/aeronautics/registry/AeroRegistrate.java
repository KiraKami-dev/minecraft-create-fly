package dev.eriksonn.aeronautics.registry;

import dev.eriksonn.aeronautics.content.blocks.hot_air.lifting_gas.LiftingGasType;
import dev.eriksonn.aeronautics.index.AeroRegistries;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import net.minecraft.resources.Identifier;

import java.util.function.Supplier;

/**
 * Aeronautics' registration helper: Simulated's (Registrate-free) registrate, so Aeronautics' items end up in their own
 * section of the Simulated creative tab. Everything registers immediately.
 */
public class AeroRegistrate extends SimulatedRegistrate {
    public AeroRegistrate(final Identifier initialSection, final String modId) {
        super(initialSection, modId);
    }

    public <T extends LiftingGasType> T liftingGasType(final String name, final Supplier<T> type) {
        return this.simple(name, AeroRegistries.Keys.LIFTING_GAS_TYPE, type);
    }
}
