package dev.simulated_team.simulated.fabric;

import dev.simulated_team.simulated.SimulatedClient;
import dev.simulated_team.simulated.events.SimulatedCommonClientEvents;
import dev.simulated_team.simulated.fabric.service.FabricSimConfigService;
import net.fabricmc.api.ClientModInitializer;

/**
 * Fabric client entrypoint (upstream: {@code SimulatedNeoForgeClient}). Runs after every main entrypoint and before
 * the first resource reload.
 */
public class SimulatedFabricClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		FabricSimConfigService.registerClient();

		SimulatedClient.init();

		// Fabric client events, HUD overlays and key mappings (SimKeys)
		SimulatedCommonClientEvents.register();
	}

}
