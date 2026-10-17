package dev.ryanhcode.sable;

import dev.ryanhcode.sable.debug.SableClientGizmoHandler;
import dev.ryanhcode.sable.network.client.SableClientNetworkEventLoop;
import dev.ryanhcode.sable.render.SableRenderBootstrap;
import dev.ryanhcode.sable.render.water_occlusion.WaterOcclusionRenderer;
import dev.ryanhcode.sable.sublevel.storage.debug.SubLevelContainerInspector;
import dev.ryanhcode.sable.veillite.api.client.editor.EditorManager;
import dev.ryanhcode.sable.veillite.api.client.render.VeilRenderSystem;
import dev.ryanhcode.sable.veillite.platform.VeilEventPlatform;
import net.minecraft.client.Minecraft;

public class SableClient {

    public static final SableClientGizmoHandler GIZMO_HANDLER = new SableClientGizmoHandler();
    public static SableClientNetworkEventLoop NETWORK_EVENT_LOOP = new SableClientNetworkEventLoop();
    public static WaterOcclusionRenderer WATER_OCCLUSION_RENDERER = new WaterOcclusionRenderer();

    public static void init() {
        VeilEventPlatform.INSTANCE.onVeilRendererAvailable(renderer -> {
            if (VeilRenderSystem.hasImGui()) {
                final EditorManager editorManager = renderer.getEditorManager();

                editorManager.add(new SubLevelContainerInspector());
            }
        });

        SableRenderBootstrap.init();

        GIZMO_HANDLER.init();
    }

    public static boolean useNativeTransport() {
        final Minecraft client = Minecraft.getInstance();
        return client.options.useNativeTransport();
    }
}
