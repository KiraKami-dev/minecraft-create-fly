/*
 * Derived from Veil (https://github.com/FoundryMC/Veil), licensed under the
 * GNU Lesser General Public License v3.0. Adapted for Minecraft 1.21.11 / Fabric
 * as part of the "veillite" shim bundled with Sable.
 */
package dev.ryanhcode.sable.veillite.api.network.handler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Context for client-side packet handling.
 */
public interface ClientPacketContext extends PacketContext {

    /**
     * @return The Minecraft client instance
     */
    Minecraft client();

    @Override
    @Nullable
    LocalPlayer player();
}
