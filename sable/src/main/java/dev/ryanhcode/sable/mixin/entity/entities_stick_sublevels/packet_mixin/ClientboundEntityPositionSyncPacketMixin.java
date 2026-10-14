package dev.ryanhcode.sable.mixin.entity.entities_stick_sublevels.packet_mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.ryanhcode.sable.mixinterface.entity.entities_stick_sublevels.packet_mixin.PacketActuallyInSubLevelExtension;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * PORT(1.21.11): replaces the 1.21.1 ClientboundTeleportEntityPacketMixin. ServerEntity#sendChanges now sends
 * {@link ClientboundEntityPositionSyncPacket} where it used to send a teleport packet. The packet is a record with a
 * composite stream codec, so the extra flag is appended by wrapping the codec.
 */
@Mixin(ClientboundEntityPositionSyncPacket.class)
public class ClientboundEntityPositionSyncPacketMixin implements PacketActuallyInSubLevelExtension {

    /**
     * If the entity is actually in a sub-level, and not just networked aside one
     */
    @Unique
    private boolean sable$actuallyInSubLevel;

    @ModifyExpressionValue(method = "<clinit>", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/codec/StreamCodec;composite(Lnet/minecraft/network/codec/StreamCodec;Ljava/util/function/Function;Lnet/minecraft/network/codec/StreamCodec;Ljava/util/function/Function;Lnet/minecraft/network/codec/StreamCodec;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function3;)Lnet/minecraft/network/codec/StreamCodec;"))
    private static StreamCodec<FriendlyByteBuf, ClientboundEntityPositionSyncPacket> sable$wrapCodec(final StreamCodec<FriendlyByteBuf, ClientboundEntityPositionSyncPacket> original) {
        return new StreamCodec<>() {
            @Override
            public ClientboundEntityPositionSyncPacket decode(final FriendlyByteBuf buf) {
                final ClientboundEntityPositionSyncPacket packet = original.decode(buf);
                ((PacketActuallyInSubLevelExtension) (Object) packet).sable$setActuallyInSubLevel(buf.readBoolean());
                return packet;
            }

            @Override
            public void encode(final FriendlyByteBuf buf, final ClientboundEntityPositionSyncPacket packet) {
                original.encode(buf, packet);
                buf.writeBoolean(((PacketActuallyInSubLevelExtension) (Object) packet).sable$isActuallyInSubLevel());
            }
        };
    }

    @Override
    public void sable$setActuallyInSubLevel(final boolean actuallyInSubLevel) {
        this.sable$actuallyInSubLevel = actuallyInSubLevel;
    }

    @Override
    public boolean sable$isActuallyInSubLevel() {
        return this.sable$actuallyInSubLevel;
    }
}
