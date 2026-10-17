package dev.simulated_team.simulated.fabric.service;

import com.zurrtum.create.api.stress.BlockStressValues;
import com.zurrtum.create.catnip.config.Builder;
import com.zurrtum.create.infrastructure.config.CStress;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.config.client.SimClient;
import dev.simulated_team.simulated.config.server.SimServer;
import dev.simulated_team.simulated.service.SimConfigService;

/**
 * Fabric configs. The config classes extend Create Fly's catnip {@code ConfigBase}, which is backed by Create Fly's own
 * JSON config files (Create Fly's {@code AllConfigs} does the same; no Forge Config API Port involved):
 * {@code config/simulated/server.json} and {@code config/simulated/client.json}.
 */
public class FabricSimConfigService implements SimConfigService {

    private static SimServer server;
    private static SimClient client;

    @Override
    public SimServer server() {
        return server;
    }

    @Override
    public boolean serverLoaded() {
        return server != null;
    }

    @Override
    public SimClient client() {
        return client;
    }

    @Override
    public boolean clientLoaded() {
        return client != null;
    }

    /**
     * Creates (or reads) the server config and hooks Simulated's stress values into Create. Must run after the blocks
     * registered their stress defaults ({@code SimBlocks.register()}).
     */
    public static void registerCommon() {
        // PORT-TODO(1.21.11): NeoForge synced server configs to clients; Create Fly's JSON configs are per side
        //  (Create Fly syncs its own with a packet), so a client on a dedicated server reads its local defaults.
        server = Builder.create(SimServer::new, Simulated.MOD_ID, "server");

        final CStress stress = server.kinetics.stressValues;
        BlockStressValues.IMPACTS.registerProvider(stress::getImpact);
        BlockStressValues.CAPACITIES.registerProvider(stress::getCapacity);
    }

    public static void registerClient() {
        client = Builder.create(SimClient::new, Simulated.MOD_ID, "client");
    }
}
