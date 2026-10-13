/*
 * Derived from Veil (https://github.com/FoundryMC/Veil), licensed under the
 * GNU Lesser General Public License v3.0. Adapted for Minecraft 1.21.11 / Fabric
 * as part of the "veillite" shim bundled with Sable.
 */
package dev.ryanhcode.sable.veillite.impl.event;

import dev.ryanhcode.sable.veillite.api.client.render.MatrixStack;
import dev.ryanhcode.sable.veillite.api.client.render.VeilRenderer;
import dev.ryanhcode.sable.veillite.api.event.FreeNativeResourcesEvent;
import dev.ryanhcode.sable.veillite.api.event.VeilAddShaderPreProcessorsEvent;
import dev.ryanhcode.sable.veillite.api.event.VeilRenderLevelStageEvent;
import dev.ryanhcode.sable.veillite.api.event.VeilRendererAvailableEvent;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.joml.Matrix4fc;

/**
 * Backing Fabric events for {@link dev.ryanhcode.sable.veillite.platform.VeilEventPlatform}, plus the
 * {@code fire*} entry points that the veillite renderer / level renderer hooks call.
 */
public final class VeilLiteEvents {

    public static final Event<VeilRenderLevelStageEvent> RENDER_LEVEL_STAGE = EventFactory.createArrayBacked(VeilRenderLevelStageEvent.class, listeners ->
            (stage, levelRenderer, bufferSource, matrixStack, frustumMatrix, projectionMatrix, renderTick, deltaTracker, camera, frustum) -> {
                for (final VeilRenderLevelStageEvent listener : listeners) {
                    listener.onRenderLevelStage(stage, levelRenderer, bufferSource, matrixStack, frustumMatrix, projectionMatrix, renderTick, deltaTracker, camera, frustum);
                }
            });

    public static final Event<VeilRendererAvailableEvent> RENDERER_AVAILABLE = EventFactory.createArrayBacked(VeilRendererAvailableEvent.class, listeners -> renderer -> {
        for (final VeilRendererAvailableEvent listener : listeners) {
            listener.onVeilRendererAvailable(renderer);
        }
    });

    public static final Event<VeilAddShaderPreProcessorsEvent> ADD_SHADER_PRE_PROCESSORS = EventFactory.createArrayBacked(VeilAddShaderPreProcessorsEvent.class, listeners -> (provider, registry) -> {
        for (final VeilAddShaderPreProcessorsEvent listener : listeners) {
            listener.onRegisterShaderPreProcessors(provider, registry);
        }
    });

    public static final Event<FreeNativeResourcesEvent> FREE_NATIVE_RESOURCES = EventFactory.createArrayBacked(FreeNativeResourcesEvent.class, listeners -> () -> {
        for (final FreeNativeResourcesEvent listener : listeners) {
            listener.onFree();
        }
    });

    private VeilLiteEvents() {
    }

    public static void fireRenderLevelStage(final VeilRenderLevelStageEvent.Stage stage, final LevelRenderer levelRenderer, final MultiBufferSource.BufferSource bufferSource, final MatrixStack matrixStack, final Matrix4fc frustumMatrix, final Matrix4fc projectionMatrix, final int renderTick, final DeltaTracker deltaTracker, final Camera camera, final Frustum frustum) {
        RENDER_LEVEL_STAGE.invoker().onRenderLevelStage(stage, levelRenderer, bufferSource, matrixStack, frustumMatrix, projectionMatrix, renderTick, deltaTracker, camera, frustum);
    }

    public static void fireRendererAvailable(final VeilRenderer renderer) {
        RENDERER_AVAILABLE.invoker().onVeilRendererAvailable(renderer);
    }

    public static void fireAddShaderPreProcessors(final ResourceProvider resourceProvider, final VeilAddShaderPreProcessorsEvent.Registry registry) {
        ADD_SHADER_PRE_PROCESSORS.invoker().onRegisterShaderPreProcessors(resourceProvider, registry);
    }

    public static void fireFreeNativeResources() {
        FREE_NATIVE_RESOURCES.invoker().onFree();
    }
}
