package dev.eriksonn.aeronautics.fabric;

import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.fabric.events.AeroFabricCommonEvents;
import dev.eriksonn.aeronautics.fabric.service.FabricAeroConfigService;
import net.fabricmc.api.ModInitializer;

/**
 * Fabric main entrypoint (upstream: {@code AeronauticsNeoForge}). Everything registers here, in {@code onInitialize}
 * (like Simulated). Aeronautics' items are shown in the Simulated creative tab, in their own section.
 */
public final class AeronauticsFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Aeronautics.init();

        // after Aeronautics.init(): the stress config entries are collected while registering blocks
        FabricAeroConfigService.registerCommon();

        AeroFabricCommonEvents.register();
    }
}
