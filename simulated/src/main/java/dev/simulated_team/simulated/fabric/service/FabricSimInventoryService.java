package dev.simulated_team.simulated.fabric.service;

import com.zurrtum.create.content.contraptions.MountedStorageManager;
import com.zurrtum.create.foundation.item.ItemHelper;
import dev.simulated_team.simulated.fabric.transfer.AbstractContainerStorage;
import dev.simulated_team.simulated.fabric.transfer.FabricInventoryLoaderWrapper;
import dev.simulated_team.simulated.fabric.transfer.SingleTankStorage;
import dev.simulated_team.simulated.multiloader.energy.SingleBattery;
import dev.simulated_team.simulated.multiloader.inventory.AbstractContainer;
import dev.simulated_team.simulated.multiloader.inventory.InventoryLoaderWrapper;
import dev.simulated_team.simulated.multiloader.tanks.SingleTank;
import dev.simulated_team.simulated.service.SimInventoryService;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Item and fluid storages are exposed through Fabric's transfer API ({@code ItemStorage.SIDED} / {@code FluidStorage.SIDED}),
 * which is also how Create Fly finds the inventories of other mods' blocks ({@code AllTransfer}).
 */
public class FabricSimInventoryService implements SimInventoryService {

    /**
     * Getters registered through {@link #registerBattery}. Fabric API has no energy API (NeoForge's
     * {@code Capabilities.EnergyStorage}); they are kept so an energy compat layer can expose them.
     */
    public static final List<EnergyGetterHolder<? extends BlockEntity>> ENERGY_GETTERS = Collections.synchronizedList(new ArrayList<>());

    @Override
    @SuppressWarnings("unchecked")
    public <T extends InventoryLoaderWrapper> T getInventory(@Nullable final BlockEntity be, @Nullable final Direction dir) {
        if (be != null) {
            final Level level = be.getLevel();
            if (level != null) {
                final Container inventory = ItemHelper.getInventory(level, be.getBlockPos(), be.getBlockState(), be, dir);
                if (inventory != null) {
                    return (T) new FabricInventoryLoaderWrapper(inventory);
                }
            }
        }

        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends InventoryLoaderWrapper> T getWrappedAllItemsFromContraption(final MountedStorageManager manager) {
        return (T) new FabricInventoryLoaderWrapper(manager.getAllItems());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends InventoryLoaderWrapper> T getWrappedMountedItemsFromContraption(final MountedStorageManager manager) {
        return (T) new FabricInventoryLoaderWrapper(manager.getMountedItems());
    }

    @Override
    public <T extends BlockEntity> Consumer<BlockEntityType<T>> registerInventory(final BiFunction<T, Direction, AbstractContainer> getter) {
        return type -> ItemStorage.SIDED.registerForBlockEntity((be, dir) -> {
            final AbstractContainer container = getter.apply(be, dir);
            return container != null ? AbstractContainerStorage.of(container) : null;
        }, type);
    }

    @Override
    public <T extends BlockEntity> Consumer<BlockEntityType<T>> registerTank(final BiFunction<T, Direction, SingleTank> getter) {
        return type -> FluidStorage.SIDED.registerForBlockEntity((be, dir) -> {
            final SingleTank tank = getter.apply(be, dir);
            return tank != null ? SingleTankStorage.of(tank) : null;
        }, type);
    }

    @Override
    public <T extends BlockEntity> Consumer<BlockEntityType<T>> registerBattery(final BiFunction<T, Direction, SingleBattery> getter) {
        // PORT-TODO(1.21.11): no energy API on Fabric in our dependencies (NeoForge exposed SingleBattery through
        //  Capabilities.EnergyStorage). Team Reborn Energy's EnergyStorage.SIDED would be the equivalent; it needs a
        //  compileOnly dependency and optional registration. The getters are kept in ENERGY_GETTERS until then.
        return type -> ENERGY_GETTERS.add(new EnergyGetterHolder<>(getter, type));
    }

    public record EnergyGetterHolder<T extends BlockEntity>(BiFunction<T, Direction, SingleBattery> getter, BlockEntityType<T> type) {
        @SuppressWarnings("unchecked")
        public SingleBattery castBlockEntityAndGetBattery(final BlockEntity be, final Direction dir) {
            return this.getter.apply((T) be, dir);
        }
    }
}
