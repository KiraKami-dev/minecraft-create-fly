/*
 * Derived from Veil (https://github.com/FoundryMC/Veil), licensed under the
 * GNU Lesser General Public License v3.0. Adapted for Minecraft 1.21.11 / Fabric
 * as part of the "veillite" shim bundled with Sable.
 */
package dev.ryanhcode.sable.veillite.api.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.ryanhcode.sable.veillite.impl.event.VeilLiteEvents;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.ApiStatus;

import java.util.concurrent.Executor;

/**
 * The subset of Veil's render system helpers that Sable uses, implemented on top of vanilla 1.21.11.
 */
public final class VeilRenderSystem {

    private static final Executor RENDER_THREAD_EXECUTOR = task -> {
        if (RenderSystem.isOnRenderThread()) {
            task.run();
        } else {
            Minecraft.getInstance().execute(task);
        }
    };

    private static VeilRenderer renderer;

    private VeilRenderSystem() {
    }

    /**
     * Creates the renderer and fires the renderer-available event.
     */
    @ApiStatus.Internal
    public static void init() {
        if (renderer == null) {
            renderer = new VeilRenderer();
            VeilLiteEvents.fireRendererAvailable(renderer);
        }
    }

    /**
     * @return The renderer instance
     */
    public static VeilRenderer renderer() {
        if (renderer == null) {
            init();
        }
        return renderer;
    }

    /**
     * @return An executor that runs tasks on the render thread
     */
    public static Executor renderThreadExecutor() {
        return RENDER_THREAD_EXECUTOR;
    }

    /**
     * @return Whether the ImGui editor is available. Always <code>false</code> on 1.21.11.
     */
    public static boolean hasImGui() {
        return false;
    }

    /**
     * Marks all chunks (and sub-levels) for re-compilation.
     */
    public static void rebuildChunks() {
        RENDER_THREAD_EXECUTOR.execute(() -> {
            final Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.levelRenderer != null && minecraft.level != null) {
                minecraft.levelRenderer.allChanged();
            }
        });
    }
}
