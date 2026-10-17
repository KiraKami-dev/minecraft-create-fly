package dev.simulated_team.simulated.service;

import com.zurrtum.create.content.contraptions.MountedStorageManager;
import dev.simulated_team.simulated.multiloader.energy.SingleBattery;
import dev.simulated_team.simulated.multiloader.inventory.AbstractContainer;
import dev.simulated_team.simulated.multiloader.inventory.InventoryLoaderWrapper;
import dev.simulated_team.simulated.multiloader.tanks.SingleTank;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Consumer;

public interface SimInventoryService {

	SimInventoryService INSTANCE = ServiceUtil.load(SimInventoryService.class);

	<T extends InventoryLoaderWrapper> T getInventory(@Nullable final BlockEntity be, @Nullable final Direction dir);

	//I don't think there's an easier way to do this...
	<T extends InventoryLoaderWrapper> T getWrappedAllItemsFromContraption(final MountedStorageManager manager);
	<T extends InventoryLoaderWrapper> T getWrappedMountedItemsFromContraption(final MountedStorageManager manager);

	/**
	 * Exposes the inventory returned by the getter to other blocks and mods. Call once the block entity type exists (its
	 * valid blocks are read at registration time).
	 */
	<T extends BlockEntity> Consumer<BlockEntityType<T>> registerInventory(final BiFunction<T, Direction, AbstractContainer> getter);

	<T extends BlockEntity> Consumer<BlockEntityType<T>> registerTank(final BiFunction<T, Direction, SingleTank> getter);

	<T extends BlockEntity> Consumer<BlockEntityType<T>> registerBattery(final BiFunction<T, Direction, SingleBattery> getter);

	default <T extends BlockEntity> void registerInventory(final BlockEntityType<T> type, final BiFunction<T, Direction, AbstractContainer> getter) {
		this.<T>registerInventory(getter).accept(type);
	}

	default <T extends BlockEntity> void registerTank(final BlockEntityType<T> type, final BiFunction<T, Direction, SingleTank> getter) {
		this.<T>registerTank(getter).accept(type);
	}

	default <T extends BlockEntity> void registerBattery(final BlockEntityType<T> type, final BiFunction<T, Direction, SingleBattery> getter) {
		this.<T>registerBattery(getter).accept(type);
	}
}
