/*
 * Derived from Veil (https://github.com/FoundryMC/Veil), licensed under the
 * GNU Lesser General Public License v3.0. Adapted for Minecraft 1.21.11 / Fabric
 * as part of the "veillite" shim bundled with Sable.
 */
package dev.ryanhcode.sable.veillite.impl.network;

import dev.ryanhcode.sable.veillite.api.network.handler.ClientPacketContext;
import io.netty.channel.ChannelFutureListener;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public record FabricClientPacketContext(ClientPlayNetworking.Context context) implements ClientPacketContext {

    @Override
    public Minecraft client() {
        return this.context.client();
    }

    @Override
    public LocalPlayer player() {
        return this.context.player();
    }

    @Override
    public Packet<?> createPacket(final CustomPacketPayload payload) {
        return this.context.responseSender().createPacket(payload);
    }

    @Override
    public void sendPacket(final Packet<?> packet, @Nullable final ChannelFutureListener callback) {
        this.context.responseSender().sendPacket(packet, callback);
    }

    @Override
    public void disconnect(final Component disconnectReason) {
        this.context.responseSender().disconnect(disconnectReason);
    }
}
