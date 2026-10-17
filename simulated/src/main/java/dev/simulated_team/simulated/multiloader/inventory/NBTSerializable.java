package dev.simulated_team.simulated.multiloader.inventory;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public interface NBTSerializable {
    CompoundTag write(HolderLookup.Provider provider);

    void read(HolderLookup.Provider provider, CompoundTag nbt);

    /**
     * Writes the same data as {@link #write(HolderLookup.Provider)} into the given view, for block entities saving through
     * {@link ValueOutput} (pass {@code view.child("Inventory")} to keep the old layout).
     */
    default void write(final ValueOutput view) {
        throw new UnsupportedOperationException(this.getClass().getName() + " does not support ValueOutput serialization");
    }

    /**
     * Reads the data written by {@link #write(ValueOutput)}.
     */
    default void read(final ValueInput view) {
        throw new UnsupportedOperationException(this.getClass().getName() + " does not support ValueInput serialization");
    }
}
