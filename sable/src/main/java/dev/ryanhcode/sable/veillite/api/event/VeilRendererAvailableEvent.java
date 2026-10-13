/*
 * Derived from Veil (https://github.com/FoundryMC/Veil), licensed under the
 * GNU Lesser General Public License v3.0. Adapted for Minecraft 1.21.11 / Fabric
 * as part of the "veillite" shim bundled with Sable.
 */
package dev.ryanhcode.sable.veillite.api.event;

import dev.ryanhcode.sable.veillite.api.client.render.VeilRenderer;

/**
 * Fired when the veillite renderer has finished initialization and is safe to use.
 * <p>
 * Fired by {@link dev.ryanhcode.sable.veillite.impl.event.VeilLiteEvents#fireRendererAvailable}.
 */
@FunctionalInterface
public interface VeilRendererAvailableEvent {

    void onVeilRendererAvailable(VeilRenderer renderer);
}
