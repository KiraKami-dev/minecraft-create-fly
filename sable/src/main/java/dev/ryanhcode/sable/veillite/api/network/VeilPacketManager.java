/*
 * Derived from Veil (https://github.com/FoundryMC/Veil), licensed under the
 * GNU Lesser General Public License v3.0. Adapted for Minecraft 1.21.11 / Fabric
 * as part of the "veillite" shim bundled with Sable.
 */
package dev.ryanhcode.sable.veillite.api.network;

import dev.ryanhcode.sable.veillite.api.network.handler.ClientPacketContext;
import dev.ryanhcode.sable.veillite.api.network.handler.PacketContext;
import dev.ryanhcode.sable.veillite.api.network.handler.ServerPacketContext;
import dev.ryanhcode.sable.veillite.impl.network.ClientPacketSink;
import dev.ryanhcode.sable.veillite.impl.network.FabricVeilPacketManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages packet registration and sending (Fabric networking backed).
 */
public interface VeilPacketManager {

    /**
     * Creates a {@link VeilPacketManager}.
     *
     * @param modId   The id of the mod creating the channel
     * @param version The channel version (unused on Fabric)
     * @return The packet manager
     */
    static VeilPacketManager create(final String modId, final String version) {
        return new FabricVeilPacketManager(modId, version);
    }

    default <T extends CustomPacketPayload> void registerClientbound(final CustomPacketPayload.Type<T> id, final StreamCodec<? super RegistryFriendlyByteBuf, T> codec, final PacketHandler<ClientPacketContext, T> handler) {
        this.registerClientbound(id, codec, handler, false);
    }

    <T extends CustomPacketPayload> void registerClientbound(CustomPacketPayload.Type<T> id, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, PacketHandler<ClientPacketContext, T> handler, boolean optional);

    default <T extends CustomPacketPayload> void registerServerbound(final CustomPacketPayload.Type<T> id, final StreamCodec<? super RegistryFriendlyByteBuf, T> codec, final PacketHandler<ServerPacketContext, T> handler) {
        this.registerServerbound(id, codec, handler, false);
    }

    <T extends CustomPacketPayload> void registerServerbound(CustomPacketPayload.Type<T> id, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, PacketHandler<ServerPacketContext, T> handler, boolean optional);

    /**
     * @return A sink to send packets to the server (client only)
     */
    static PacketSink server() {
        return ClientPacketSink.INSTANCE;
    }

    /**
     * @return A sink to send packets to the specified player
     */
    static PacketSink player(final ServerPlayer player) {
        return packet -> player.connection.send(packet);
    }

    /**
     * @return A sink to send packets to all players in the specified level
     */
    static PacketSink level(final ServerLevel level) {
        return packet -> level.getServer().getPlayerList().broadcastAll(packet, level.dimension());
    }

    /**
     * @return A sink to send packets to all players within {@code radius} of the position, excluding {@code excluded}
     */
    static PacketSink around(@Nullable final ServerPlayer excluded, final ServerLevel level, final double x, final double y, final double z, final double radius) {
        return packet -> level.getServer().getPlayerList().broadcast(excluded, x, y, z, radius, level.dimension(), packet);
    }

    /**
     * @return A sink to send packets to all players on the server
     */
    static PacketSink all(final MinecraftServer server) {
        return packet -> server.getPlayerList().broadcastAll(packet);
    }

    /**
     * @return A sink to send packets to all players tracking the entity, excluding the entity
     */
    @SuppressWarnings("unchecked")
    static PacketSink tracking(final Entity entity) {
        if (!(entity.level().getChunkSource() instanceof final ServerChunkCache chunkCache)) {
            throw new IllegalStateException("Cannot send clientbound payloads on the client");
        }

        return packet -> chunkCache.sendToTrackingPlayers(entity, (Packet<? super ClientGamePacketListener>) packet);
    }

    /**
     * @return A sink to send packets to all players tracking the entity, including the entity
     */
    @SuppressWarnings("unchecked")
    static PacketSink trackingAndSelf(final Entity entity) {
        if (!(entity.level().getChunkSource() instanceof final ServerChunkCache chunkCache)) {
            throw new IllegalStateException("Cannot send clientbound payloads on the client");
        }

        return packet -> chunkCache.sendToTrackingPlayersAndSelf(entity, (Packet<? super ClientGamePacketListener>) packet);
    }

    /**
     * @return A sink to send packets to all players tracking the chunk
     */
    static PacketSink tracking(final ServerLevel level, final ChunkPos pos) {
        return packet -> {
            for (final ServerPlayer player : level.getChunkSource().chunkMap.getPlayers(pos, false)) {
                player.connection.send(packet);
            }
        };
    }

    static PacketSink tracking(final ServerLevel level, final BlockPos pos) {
        return tracking(level, new ChunkPos(pos));
    }

    static PacketSink tracking(final ServerLevel level, final BlockPos min, final BlockPos max) {
        return packet -> ChunkPos.rangeClosed(new ChunkPos(min), new ChunkPos(max))
                .flatMap(pos -> level.getChunkSource().chunkMap.getPlayers(pos, false).stream())
                .distinct()
                .forEach(player -> player.connection.send(packet));
    }

    static PacketSink tracking(final BlockEntity blockEntity) {
        if (!(blockEntity.getLevel() instanceof final ServerLevel serverLevel)) {
            throw new IllegalArgumentException("Only supported on server levels!");
        }
        return tracking(serverLevel, blockEntity.getBlockPos());
    }

    /**
     * Handles packets from the client/server.
     *
     * @param <T> The context to use
     * @param <P> The packet to handle
     */
    @FunctionalInterface
    interface PacketHandler<T extends PacketContext, P extends CustomPacketPayload> {

        void handlePacket(P payload, T ctx);
    }

    /**
     * Sends packets to players and automatically bundles payloads together.
     */
    @FunctionalInterface
    interface PacketSink {

        /**
         * Sends one or more payloads in a single (bundled) packet.
         */
        default void sendPacket(final CustomPacketPayload... payloads) {
            if (payloads.length == 0) {
                return;
            }
            if (payloads.length == 1) {
                this.sendPacket(new ClientboundCustomPayloadPacket(payloads[0]));
                return;
            }

            final List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
            for (final CustomPacketPayload payload : payloads) {
                packets.add(new ClientboundCustomPayloadPacket(payload));
            }
            this.sendPacket(new ClientboundBundlePacket(packets));
        }

        /**
         * Sends a single packet.
         */
        void sendPacket(Packet<?> packet);
    }
}
