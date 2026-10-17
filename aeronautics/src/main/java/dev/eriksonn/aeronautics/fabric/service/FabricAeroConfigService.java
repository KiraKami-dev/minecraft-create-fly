package dev.eriksonn.aeronautics.fabric.service;

import com.zurrtum.create.api.stress.BlockStressValues;
import com.zurrtum.create.catnip.config.Builder;
import com.zurrtum.create.infrastructure.config.CStress;
import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.config.AeroConfig;
import dev.eriksonn.aeronautics.config.client.AeroClient;
import dev.eriksonn.aeronautics.config.server.AeroServer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

/**
 * Fabric configs (upstream: {@code NeoForgeAeroConfigService}). Like Simulated's, they extend Create Fly's catnip
 * {@code ConfigBase}, backed by Create Fly's own JSON config files (no Forge Config API Port):
 * {@code config/aeronautics/server.json} and {@code config/aeronautics/client.json}.
 */
public class FabricAeroConfigService implements AeroConfig {

	private static AeroServer server;
	private static AeroClient client;

	@Override
	public AeroServer getServerConfig() {
		return server;
	}

	@Override
	public AeroClient getClientConfig() {
		return client;
	}

	/**
	 * Creates (or reads) the server config and hooks Aeronautics' stress values into Create. Must run after the blocks
	 * registered their stress defaults ({@code AeroBlocks.register()}).
	 */
	public static void registerCommon() {
		// PORT-TODO(1.21.11): NeoForge synced server configs to clients; Create Fly's JSON configs are per side, so a
		//  client on a dedicated server reads its local defaults (same as Simulated's FabricSimConfigService).
		server = Builder.create(AeroServer::new, Aeronautics.MOD_ID, "server");

		final CStress stress = server.kinetics.stressValues;
		BlockStressValues.IMPACTS.registerProvider(stress::getImpact);
		BlockStressValues.CAPACITIES.registerProvider(stress::getCapacity);

		// re-read the file on /reload and world load, like Create Fly's AllConfigs.LISTENER
		ResourceLoader.get(PackType.SERVER_DATA).registerReloader(Aeronautics.path("config"), (ResourceManagerReloadListener) manager -> server.reload());
	}

	public static void registerClient() {
		client = Builder.create(AeroClient::new, Aeronautics.MOD_ID, "client");
	}
}
