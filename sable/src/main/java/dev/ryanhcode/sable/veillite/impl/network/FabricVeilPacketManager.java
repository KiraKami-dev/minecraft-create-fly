/*
 * Derived from Veil (https://github.com/FoundryMC/Veil), licensed under the
 * GNU Lesser General Public License v3.0. Adapted for Minecraft 1.21.11 / Fabric
 * as part of the "veillite" shim bundled with Sable.
 */
package dev.ryanhcode.sable.veillite.impl.network;

import dev.ryanhcode.sable.veillite.api.network.VeilPacketManager;
import dev.ryanhcode.sable.veillite.api.network.handler.ClientPacketContext;
import dev.ryanhcode.sable.veillite.api.network.handler.ServerPacketContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.ApiStatus;

/**
 * Fabric networking implementation of {@link VeilPacketManager}.
 * Handlers run on the main (client/server) thread, as Fabric API schedules them there.
 */
@ApiStatus.Internal
public class FabricVeilPacketManager implements VeilPacketManager {

    private final String modId;
    private final String version;

    public FabricVeilPacketManager(final String modId, final String version) {
        this.modId = modId;
        this.version = version;
    }

    @Override
    public <T extends CustomPacketPayload> void registerClientbound(final CustomPacketPayload.Type<T> id, final StreamCodec<? super RegistryFriendlyByteBuf, T> codec, final PacketHandler<ClientPacketContext, T> handler, final boolean optional) {
        PayloadTypeRegistry.playS2C().register(id, codec);
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            FabricClientPacketRegistrar.register(id, handler);
        }
    }

    @Override
    public <T extends CustomPacketPayload> void registerServerbound(final CustomPacketPayload.Type<T> id, final StreamCodec<? super RegistryFriendlyByteBuf, T> codec, final PacketHandler<ServerPacketContext, T> handler, final boolean optional) {
        PayloadTypeRegistry.playC2S().register(id, codec);
        ServerPlayNetworking.registerGlobalReceiver(id, (payload, context) -> handler.handlePacket(payload, new FabricServerPacketContext(context)));
    }

    public String getModId() {
        return this.modId;
    }

    public String getVersion() {
        return this.version;
    }
}
