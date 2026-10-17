package dev.simulated_team.simulated.network.packets.linked_typewriter;

import com.mojang.serialization.DataResult;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterEntries;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.ryanhcode.sable.veillite.api.network.handler.ServerPacketContext;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.entity.BlockEntityType;

public record TypewriterSaveKeyToItemPacket(InteractionHand hand, LinkedTypewriterEntries.KeyboardEntry entry) implements CustomPacketPayload {

    public static Type<TypewriterSaveKeyToItemPacket> TYPE = new Type<>(Simulated.path("linked_typewriter_bind_item"));

    public static StreamCodec<RegistryFriendlyByteBuf, TypewriterSaveKeyToItemPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, (packet) -> packet.hand.ordinal(),
            LinkedTypewriterEntries.KeyboardEntry.STREAM_CODEC, TypewriterSaveKeyToItemPacket::entry,
            (h, e) -> new TypewriterSaveKeyToItemPacket(InteractionHand.values()[h], e));

    public void handle(final ServerPacketContext context) {
        final ServerPlayer player = context.player();
        final ItemStack item = player.getItemInHand(this.hand);

        // 1.21.11: block entity data is typed, the id is not part of the tag anymore
        final TypedEntityData<BlockEntityType<?>> currentData = item.get(DataComponents.BLOCK_ENTITY_DATA);
        final CompoundTag currentTag = currentData != null ? currentData.copyTagWithoutId() : new CompoundTag();

        final RegistryOps<Tag> ops = context.level().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        final DataResult<Tag> result = LinkedTypewriterEntries.KeyboardEntry.CODEC.encodeStart(ops, this.entry);
        if (result.isError()) {
            Simulated.LOGGER.warn("Unable to process entry for item saving!: {}", result.error().get().message());
            return;
        }

        final CompoundTag entryTag = (CompoundTag) result.getOrThrow();
        final ListTag keys = currentTag.getListOrEmpty("Keys");
        boolean alreadyPresent = false;

        for (int i = 0; i < keys.size(); i++) {
            final Tag key = keys.get(i);
            final int glfwKey = key instanceof final CompoundTag keyTag ? keyTag.getIntOr("GLFWKey", -1) : -1;

            if (glfwKey == this.entry.glfwKeyCode) {
                alreadyPresent = true;
                keys.set(i, entryTag);
                break;
            }
        }

        if (!alreadyPresent) {
            keys.add(entryTag);
        }

        currentTag.put("Keys", keys);
        if (item.is(SimBlocks.LINKED_TYPEWRITER.asItem())) {
            item.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.<BlockEntityType<?>>of(SimBlockEntityTypes.LINKED_TYPEWRITER, currentTag));
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
