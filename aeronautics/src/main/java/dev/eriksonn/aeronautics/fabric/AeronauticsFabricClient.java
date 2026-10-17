package dev.eriksonn.aeronautics.fabric;

import dev.eriksonn.aeronautics.AeronauticsClient;
import dev.eriksonn.aeronautics.fabric.events.AeroFabricClientEvents;
import dev.eriksonn.aeronautics.fabric.service.FabricAeroConfigService;
import net.fabricmc.api.ClientModInitializer;

/**
 * Fabric client entrypoint (upstream: {@code AeronauticsNeoForgeClient}). Runs after every main entrypoint and before
 * the first resource reload.
 */
public class AeronauticsFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        FabricAeroConfigService.registerClient();

        AeronauticsClient.init();

        AeroFabricClientEvents.register();
    }
}
