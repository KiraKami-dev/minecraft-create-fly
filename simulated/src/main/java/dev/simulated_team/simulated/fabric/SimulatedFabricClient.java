package dev.simulated_team.simulated.fabric;

import dev.simulated_team.simulated.SimulatedClient;
import dev.simulated_team.simulated.fabric.service.FabricSimConfigService;
import dev.simulated_team.simulated.index.SimKeys;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

/**
 * Fabric client entrypoint (upstream: {@code SimulatedNeoForgeClient} + the mod-bus half of
 * {@code SimNeoForgeClientEvents}). Runs after every main entrypoint and before the first resource reload.
 */
public class SimulatedFabricClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		FabricSimConfigService.registerClient();
		SimKeys.registerTo(KeyBindingHelper::registerKeyBinding);

		SimulatedClient.init();
	}

}
