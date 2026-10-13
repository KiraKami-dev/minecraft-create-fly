/*
 * Derived from Veil (https://github.com/FoundryMC/Veil), licensed under the
 * GNU Lesser General Public License v3.0. Adapted for Minecraft 1.21.11 / Fabric
 * as part of the "veillite" shim bundled with Sable.
 */
package dev.ryanhcode.sable.veillite.api.event;

/**
 * Fired when the client is shutting down and native resources should be freed.
 * <p>
 * Fired from Fabric's {@code ClientLifecycleEvents.CLIENT_STOPPING}.
 */
@FunctionalInterface
public interface FreeNativeResourcesEvent {

    void onFree();
}
