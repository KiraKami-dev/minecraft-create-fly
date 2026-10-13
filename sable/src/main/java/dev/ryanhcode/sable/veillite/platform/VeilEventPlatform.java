/*
 * Derived from Veil (https://github.com/FoundryMC/Veil), licensed under the
 * GNU Lesser General Public License v3.0. Adapted for Minecraft 1.21.11 / Fabric
 * as part of the "veillite" shim bundled with Sable.
 */
package dev.ryanhcode.sable.veillite.platform;

import dev.ryanhcode.sable.veillite.api.event.FreeNativeResourcesEvent;
import dev.ryanhcode.sable.veillite.api.event.VeilAddShaderPreProcessorsEvent;
import dev.ryanhcode.sable.veillite.api.event.VeilRenderLevelStageEvent;
import dev.ryanhcode.sable.veillite.api.event.VeilRendererAvailableEvent;
import dev.ryanhcode.sable.veillite.impl.event.VeilLiteEvents;

/**
 * Event subscriptions. Only the events Sable uses are provided.
 */
public interface VeilEventPlatform {

    VeilEventPlatform INSTANCE = new VeilEventPlatform() {
    };

    default void onFreeNativeResources(final FreeNativeResourcesEvent event) {
        VeilLiteEvents.FREE_NATIVE_RESOURCES.register(event);
    }

    default void onVeilAddShaderProcessors(final VeilAddShaderPreProcessorsEvent event) {
        VeilLiteEvents.ADD_SHADER_PRE_PROCESSORS.register(event);
    }

    default void onVeilRendererAvailable(final VeilRendererAvailableEvent event) {
        VeilLiteEvents.RENDERER_AVAILABLE.register(event);
    }

    default void onVeilRenderLevelStage(final VeilRenderLevelStageEvent event) {
        VeilLiteEvents.RENDER_LEVEL_STAGE.register(event);
    }
}
