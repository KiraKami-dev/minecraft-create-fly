/*
 * Derived from Veil (https://github.com/FoundryMC/Veil), licensed under the
 * GNU Lesser General Public License v3.0. Adapted for Minecraft 1.21.11 / Fabric
 * as part of the "veillite" shim bundled with Sable.
 */
package dev.ryanhcode.sable.veillite.api.network.handler;

import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Common context for packet handling.
 * <p>
 * Note: since 1.21.5 the send callback is a netty {@link ChannelFutureListener} (was {@code PacketSendListener}).
 */
public interface PacketContext {

    /**
     * @return The player that received the payload
     */
    @Nullable
    Player player();

    /**
     * @return The level the context player is on
     */
    default @Nullable Level level() {
        final Player player = this.player();
        return player != null ? player.level() : null;
    }

    Packet<?> createPacket(CustomPacketPayload payload);

    default void sendPacket(final CustomPacketPayload payload) {
        this.sendPacket(this.createPacket(payload), null);
    }

    default void sendPacket(final CustomPacketPayload payload, @Nullable final ChannelFutureListener callback) {
        this.sendPacket(this.createPacket(payload), callback);
    }

    default void sendPacket(final Packet<?> packet) {
        this.sendPacket(packet, null);
    }

    void sendPacket(Packet<?> packet, @Nullable ChannelFutureListener callback);

    void disconnect(Component disconnectReason);
}
