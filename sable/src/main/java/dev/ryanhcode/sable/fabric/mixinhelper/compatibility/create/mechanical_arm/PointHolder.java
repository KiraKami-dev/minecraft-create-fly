package dev.ryanhcode.sable.fabric.mixinhelper.compatibility.create.mechanical_arm;

import com.zurrtum.create.catnip.nbt.NBTHelper;
import com.zurrtum.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.Optional;

/**
 * A point holder to record and gather information from ArmInteractionPoints
 *
 * @param pos The position of this holder
 * @param interactionMode the ArmInteractionPoint mode of this holder
 * @param covered Whether this point holder is being covered by an ArmInteractionPoint
 */
public record PointHolder(BlockPos pos, ArmInteractionPoint.Mode interactionMode, MutableBoolean covered) {

    public CompoundTag serialize(final BlockPos anchor) {
        final CompoundTag tag = new CompoundTag();

        tag.store("pos", BlockPos.CODEC, this.pos.subtract(anchor));
        NBTHelper.writeEnum(tag, "mode", this.interactionMode);

        return tag;
    }

    public static PointHolder deserialize(final CompoundTag tag, final BlockPos anchor) {
        final Optional<BlockPos> pos = tag.read("pos", BlockPos.CODEC);

        return pos.map(blockPos -> new PointHolder(blockPos.offset(anchor), NBTHelper.readEnum(tag, "mode", ArmInteractionPoint.Mode.class), new MutableBoolean(false))).orElse(null);

    }

}
