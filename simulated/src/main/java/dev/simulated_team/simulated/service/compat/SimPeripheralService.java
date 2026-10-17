package dev.simulated_team.simulated.service.compat;

import dan200.computercraft.api.network.wired.WiredElement;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

public interface SimPeripheralService {

    <T extends BlockEntity> void addPeripheral(BlockEntityType<T> type, CapabilityGetter<T, IPeripheral> getter);

    default <T extends BlockEntity> void addPeripheral(final BlockEntityType<T> type, final SimpleCapabilityGetter<T, IPeripheral> getter) {
        this.addPeripheral(type, (CapabilityGetter<T, IPeripheral>) getter);
    }

    <T extends BlockEntity> void addWired(BlockEntityType<T> type, CapabilityGetter<T, WiredElement> getter);

    default <T extends BlockEntity> void addWired(final BlockEntityType<T> type, final SimpleCapabilityGetter<T, WiredElement> getter) {
        this.addWired(type, (CapabilityGetter<T, WiredElement>) getter);
    }

    @FunctionalInterface
    interface CapabilityGetter<T extends BlockEntity, V> {

        @Nullable
        V get(T blockEntity, Direction direction);
    }

    @FunctionalInterface
    interface SimpleCapabilityGetter<T extends BlockEntity, V> extends CapabilityGetter<T, V> {

        @Nullable
        V get(T blockEntity);

        @Override
        default V get(final T blockEntity, final Direction direction) {
            return this.get(blockEntity);
        }
    }
}
