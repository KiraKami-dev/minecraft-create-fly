package dev.ryanhcode.sable.fabric.client;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.SableClient;
import dev.ryanhcode.sable.SableClientConfig;
import dev.ryanhcode.sable.physics.config.FloatingBlockMaterialDataHandler;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderer;
import dev.ryanhcode.sable.veillite.api.client.editor.EditorManager;
import dev.ryanhcode.sable.veillite.api.client.editor.Inspector;
import dev.ryanhcode.sable.veillite.api.client.render.VeilRenderSystem;
import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import fuzs.forgeconfigapiport.fabric.api.v5.ModConfigEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.neoforged.fml.config.ModConfig;

public final class SableFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        SableClient.init();

        // Debug inspectors (ImGui); off until toggled
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) ->
                dispatcher.register(ClientCommandManager.literal("sableinspector").executes(context -> {
                    if (!VeilRenderSystem.hasImGui()) {
                        context.getSource().sendError(Component.translatable("command.sable.inspector.unavailable"));
                        return 0;
                    }
                    final EditorManager editorManager = VeilRenderSystem.renderer().getEditorManager();
                    for (final Inspector inspector : editorManager.getInspectors()) {
                        editorManager.toggle(inspector);
                    }
                    return 1;
                })));

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
    }
}
