package dev.ryanhcode.offroad.fabric;

import dev.ryanhcode.offroad.OffroadClient;
import dev.ryanhcode.offroad.handlers.client.MultiMiningClientHandler;
import dev.ryanhcode.offroad.fabric.service.FabricOffroadConfigService;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * Fabric client entrypoint (upstream: {@code OffroadNeoForgeClient}). Runs after every main entrypoint and before the
 * first resource reload.
 */
public class OffroadFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        FabricOffroadConfigService.registerClient();

        OffroadClient.init();

        // LevelTickEvent.Post, client side
        ClientTickEvents.END_WORLD_TICK.register(MultiMiningClientHandler::tick);
    }
}
