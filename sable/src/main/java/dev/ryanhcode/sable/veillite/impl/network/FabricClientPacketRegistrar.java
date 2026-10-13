/*
 * Derived from Veil (https://github.com/FoundryMC/Veil), licensed under the
 * GNU Lesser General Public License v3.0. Adapted for Minecraft 1.21.11 / Fabric
 * as part of the "veillite" shim bundled with Sable.
 */
package dev.ryanhcode.sable.veillite.impl.network;

import dev.ryanhcode.sable.veillite.api.network.VeilPacketManager;
import dev.ryanhcode.sable.veillite.api.network.handler.ClientPacketContext;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.ApiStatus;

/**
 * Client-only receiver registration, kept in its own class so dedicated servers never load client classes.
 */
@ApiStatus.Internal
final class FabricClientPacketRegistrar {

    private FabricClientPacketRegistrar() {
    }

    static <T extends CustomPacketPayload> void register(final CustomPacketPayload.Type<T> id, final VeilPacketManager.PacketHandler<ClientPacketContext, T> handler) {
        ClientPlayNetworking.registerGlobalReceiver(id, (payload, context) -> handler.handlePacket(payload, new FabricClientPacketContext(context)));
    }
}
