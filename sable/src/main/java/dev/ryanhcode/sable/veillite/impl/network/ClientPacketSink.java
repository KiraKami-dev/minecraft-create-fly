/*
 * Derived from Veil (https://github.com/FoundryMC/Veil), licensed under the
 * GNU Lesser General Public License v3.0. Adapted for Minecraft 1.21.11 / Fabric
 * as part of the "veillite" shim bundled with Sable.
 */
package dev.ryanhcode.sable.veillite.impl.network;

import dev.ryanhcode.sable.veillite.api.network.VeilPacketManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.ApiStatus;

/**
 * Sends packets from the client to the server.
 */
@ApiStatus.Internal
public enum ClientPacketSink implements VeilPacketManager.PacketSink {

    INSTANCE;

    @Override
    public void sendPacket(final CustomPacketPayload... payloads) {
        for (final CustomPacketPayload payload : payloads) {
            this.sendPacket(new ServerboundCustomPayloadPacket(payload));
        }
    }

    @Override
    public void sendPacket(final Packet<?> packet) {
        final ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            throw new IllegalStateException("Cannot send client packets while not connected to a server");
        }

        connection.send(packet);
    }
}
