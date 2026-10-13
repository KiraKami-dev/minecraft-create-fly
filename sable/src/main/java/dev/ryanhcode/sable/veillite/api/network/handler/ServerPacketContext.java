/*
 * Derived from Veil (https://github.com/FoundryMC/Veil), licensed under the
 * GNU Lesser General Public License v3.0. Adapted for Minecraft 1.21.11 / Fabric
 * as part of the "veillite" shim bundled with Sable.
 */
package dev.ryanhcode.sable.veillite.api.network.handler;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Context for server-side packet handling.
 */
public interface ServerPacketContext extends PacketContext {

    /**
     * @return The Minecraft server instance
     */
    default MinecraftServer server() {
        return this.player().level().getServer();
    }

    @Override
    @NotNull
    ServerPlayer player();

    @Override
    default @NotNull ServerLevel level() {
        return this.player().level();
    }
}
