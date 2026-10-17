package dev.simulated_team.simulated.multiloader.tanks;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.simulated_team.simulated.Simulated;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * A loader-independent representation of a fluid
 */
public record CFluidType(Fluid fluid, DataComponentPatch data) {
    public boolean isBlank() {
        return this.equals(BLANK);
    }

    public static final CFluidType BLANK = new CFluidType(Fluids.EMPTY, DataComponentPatch.EMPTY);

    /**
     * Same layout as {@link #write()}.
     */
    public static final Codec<CFluidType> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.FLUID.byNameCodec().fieldOf("Fluid").forGetter(CFluidType::fluid),
            DataComponentPatch.CODEC.optionalFieldOf("data", DataComponentPatch.EMPTY).forGetter(CFluidType::data)
    ).apply(instance, CFluidType::new));

    public CompoundTag write() {
        final CompoundTag tag = new CompoundTag();
        tag.putString("Fluid", BuiltInRegistries.FLUID.getKey(this.fluid).toString());

        final DataResult<Tag> result = DataComponentPatch.CODEC.encodeStart(NbtOps.INSTANCE, this.data);
        if (result.isError()) {
            Simulated.LOGGER.warn(result.error().get().message());
        } else {
            tag.put("data", result.result().get());
        }

        return tag;
    }

    public static CFluidType read(final CompoundTag tag) {
        final Identifier fluidId = Identifier.tryParse(tag.getStringOr("Fluid", ""));
        final Fluid fluid = fluidId != null ? BuiltInRegistries.FLUID.getValue(fluidId) : Fluids.EMPTY;
        DataComponentPatch data = DataComponentPatch.EMPTY;
        final Tag dataTag = tag.get("data");
        if (dataTag != null) {
            final DataResult<Pair<DataComponentPatch, Tag>> result = DataComponentPatch.CODEC.decode(NbtOps.INSTANCE, dataTag);
            if (result.isError()) {
                Simulated.LOGGER.warn(result.error().get().message());
            } else {
                data = result.result().get().getFirst();
            }
        }

        return new CFluidType(fluid, data);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof CFluidType(Fluid fluid1, DataComponentPatch data1)) {
            // both haves tag, or both no haves tag
            return this.fluid.isSame(fluid1) && this.data.equals(data1);
        }
        return false;
    }
}
