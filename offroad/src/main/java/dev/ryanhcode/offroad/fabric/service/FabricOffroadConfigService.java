package dev.ryanhcode.offroad.fabric.service;

import com.zurrtum.create.api.stress.BlockStressValues;
import com.zurrtum.create.catnip.config.Builder;
import com.zurrtum.create.infrastructure.config.CStress;
import dev.ryanhcode.offroad.Offroad;
import dev.ryanhcode.offroad.config.OffroadConfig;
import dev.ryanhcode.offroad.config.client.OffroadClientConfig;
import dev.ryanhcode.offroad.config.server.OffroadServer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

/**
 * Fabric configs (upstream: {@code NeoForgeOffroadConfigService}). Like Simulated's, they extend Create Fly's catnip
 * {@code ConfigBase}, backed by Create Fly's own JSON config files (no Forge Config API Port):
 * {@code config/offroad/server.json} and {@code config/offroad/client.json}.
 */
public class FabricOffroadConfigService implements OffroadConfig {

	private static OffroadServer server;
	private static OffroadClientConfig client;

	@Override
	public OffroadServer getServerConfig() {
		return server;
	}

	@Override
	public OffroadClientConfig getClientConfig() {
		return client;
	}

	/**
	 * Creates (or reads) the server config and hooks Offroad's stress values into Create. Must run after the blocks
	 * registered their stress defaults ({@code OffroadBlocks.register()}).
	 */
	public static void registerCommon() {
		// PORT-TODO(1.21.11): NeoForge synced server configs to clients; Create Fly's JSON configs are per side, so a
		//  client on a dedicated server reads its local defaults (same as Simulated's FabricSimConfigService).
		server = Builder.create(OffroadServer::new, Offroad.MOD_ID, "server");

		final CStress stress = server.kinetics.stressValues;
		BlockStressValues.IMPACTS.registerProvider(stress::getImpact);
		BlockStressValues.CAPACITIES.registerProvider(stress::getCapacity);

		// re-read the file on /reload and world load, like Create Fly's AllConfigs.LISTENER
		ResourceLoader.get(PackType.SERVER_DATA).registerReloader(Offroad.path("config"), (ResourceManagerReloadListener) manager -> server.reload());
	}

	public static void registerClient() {
		client = Builder.create(OffroadClientConfig::new, Offroad.MOD_ID, "client");
	}
}
