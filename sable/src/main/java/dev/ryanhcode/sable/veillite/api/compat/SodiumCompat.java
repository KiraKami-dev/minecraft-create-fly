/*
 * Derived from Veil (https://github.com/FoundryMC/Veil), licensed under the
 * GNU Lesser General Public License v3.0. Adapted for Minecraft 1.21.11 / Fabric
 * as part of the "veillite" shim bundled with Sable.
 */
package dev.ryanhcode.sable.veillite.api.compat;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Sodium detection.
 */
public final class SodiumCompat {

    private static final boolean LOADED = FabricLoader.getInstance().isModLoaded("sodium");

    private SodiumCompat() {
    }

    public static boolean isLoaded() {
        return LOADED;
    }
}
