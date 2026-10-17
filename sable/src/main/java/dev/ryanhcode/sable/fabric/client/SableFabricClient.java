package dev.ryanhcode.sable.fabric.client;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.SableClient;
import dev.ryanhcode.sable.SableClientConfig;
import dev.ryanhcode.sable.physics.config.FloatingBlockMaterialDataHandler;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderer;
import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import fuzs.forgeconfigapiport.fabric.api.v5.ModConfigEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.packs.PackType;
import net.neoforged.fml.config.ModConfig;

public final class SableFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        SableClient.init();

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> FloatingBlockMaterialDataHandler.clearMaterials());
        // The dispatcher is resolved lazily on every reload, as the selected renderer can change at runtime
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloader(Sable.sablePath("sub_level_renderer"),
                (sharedState, backgroundExecutor, preparationBarrier, gameExecutor) ->
                        SubLevelRenderer.getDispatcher().reload(sharedState, backgroundExecutor, preparationBarrier, gameExecutor));

        ModConfigEvents.loading(Sable.MOD_ID).register(config -> {
            if (config.getSpec().equals(SableClientConfig.SPEC))
                SableClientConfig.onUpdate(false);
        });

        ModConfigEvents.reloading(Sable.MOD_ID).register(config -> {
            if (config.getSpec().equals(SableClientConfig.SPEC))
                SableClientConfig.onUpdate(true);
        });

        ConfigRegistry.INSTANCE.register(Sable.MOD_ID, ModConfig.Type.CLIENT, SableClientConfig.SPEC);

        // Create Fly bundles Flywheel
        if (FabricLoader.getInstance().isModLoaded("create")) {
            Sable.LOGGER.warn("NOTE: Sable is loaded with Flywheel (bundled in Create Fly). Sable contains extensive shader overrides and a full light-storage replacement. Expect this to cause compatibility issues. If issues arise, please report them to the Sable issue tracker ({}) instead of the Flywheel issue tracker.", Sable.ISSUE_TRACKER_URL);
        }
    }
}
