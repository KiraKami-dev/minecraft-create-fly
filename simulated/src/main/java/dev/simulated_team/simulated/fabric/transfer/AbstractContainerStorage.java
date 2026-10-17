package dev.simulated_team.simulated.fabric.transfer;

import com.google.common.collect.MapMaker;
import dev.simulated_team.simulated.multiloader.inventory.AbstractContainer;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Exposes an {@link AbstractContainer} as a Fabric item {@code Storage} (the Fabric half of NeoForge's
 * {@code ContainerWrapper}). Every slot goes through the container's own {@link AbstractContainer#insertSlot} and
 * {@link AbstractContainer#extractSlot} rules, so filters, insert-only and extract-only slots keep working; Create Fly
 * reaches it through {@code ItemStorage.SIDED} like any other mod's inventory.
 * <p>
 * Changes made inside a transaction are applied with {@link AbstractContainer#setItemSilently} and reported through
 * {@link AbstractContainer#onTransferCommitted} once the outermost transaction commits, so simulated transfers that get
 * rolled back don't trigger block updates.
 */
public final class AbstractContainerStorage extends CombinedStorage<ItemVariant, SingleSlotStorage<ItemVariant>> implements SlottedStorage<ItemVariant> {

	/**
	 * One wrapper per container, so that concurrent transactions on the same slot share their snapshots (same scheme as
	 * Fabric's own container wrapper: weak values, the slots keep their parent wrapper alive).
	 */
	private static final Map<AbstractContainer, AbstractContainerStorage> WRAPPERS = new MapMaker().weakValues().makeMap();

	private final AbstractContainer container;
	private final List<ContainerSlotStorage> backingList = new ArrayList<>();

	private AbstractContainerStorage(final AbstractContainer container) {
		super(Collections.emptyList());
		this.container = container;
	}

	public static AbstractContainerStorage of(final AbstractContainer container) {
		final AbstractContainerStorage storage = WRAPPERS.computeIfAbsent(container, AbstractContainerStorage::new);
		storage.resizeSlotList();
		return storage;
	}

	private void resizeSlotList() {
		final int size = this.container.getContainerSize();
		if (size != this.parts.size()) {
			while (this.backingList.size() < size) {
				this.backingList.add(new ContainerSlotStorage(this, this.backingList.size()));
			}
			this.parts = Collections.unmodifiableList(this.backingList.subList(0, size));
		}
	}

	public AbstractContainer getContainer() {
		return this.container;
	}

	@Override
	public int getSlotCount() {
		return this.parts.size();
	}

	@Override
	public SingleSlotStorage<ItemVariant> getSlot(final int slot) {
		return this.parts.get(slot);
	}

	@Override
	public List<SingleSlotStorage<ItemVariant>> getSlots() {
		return this.parts;
	}

	/**
	 * Fills matching stacks before empty slots, like Fabric's own container wrapper.
	 */
	@Override
	public long insert(final ItemVariant resource, final long maxAmount, final TransactionContext transaction) {
		StoragePreconditions.notBlankNotNegative(resource, maxAmount);
		return StorageUtil.insertStacking(this.parts, resource, maxAmount, transaction);
	}

	@Override
	public String toString() {
		return "AbstractContainerStorage[" + this.container + "]";
	}

	private static final class ContainerSlotStorage extends SnapshotParticipant<ItemStack> implements SingleSlotStorage<ItemVariant> {

		/**
		 * Strong reference: keeps the parent wrapper (and its weak map entry) alive while a slot is in use.
		 */
		private final AbstractContainerStorage owner;
		private final int index;
		private ItemStack lastReleasedSnapshot = ItemStack.EMPTY;

		private ContainerSlotStorage(final AbstractContainerStorage owner, final int index) {
			this.owner = owner;
			this.index = index;
		}

		private AbstractContainer container() {
			return this.owner.container;
		}

		private ItemStack getStack() {
			return this.container().getItem(this.index);
		}

		@Override
		public long insert(final ItemVariant resource, final long maxAmount, final TransactionContext transaction) {
			StoragePreconditions.notBlankNotNegative(resource, maxAmount);

			final int amount = (int) Math.min(maxAmount, Integer.MAX_VALUE);
			if (amount == 0) {
				return 0;
			}

			final ItemStack offered = resource.toStack(amount);
			final ItemStack remainder = this.container().insertSlot(offered, this.index, true);
			final int inserted = amount - remainder.getCount();
			if (inserted <= 0) {
				return 0;
			}

			this.updateSnapshots(transaction);
			final ItemStack current = this.getStack();
			final ItemStack result = current.isEmpty() ? resource.toStack(inserted) : current.copyWithCount(current.getCount() + inserted);
			this.container().setItemSilently(this.index, result);
			return inserted;
		}

		@Override
		public long extract(final ItemVariant resource, final long maxAmount, final TransactionContext transaction) {
			StoragePreconditions.notBlankNotNegative(resource, maxAmount);

			final ItemStack current = this.getStack();
			final int amount = (int) Math.min(maxAmount, Integer.MAX_VALUE);
			if (amount == 0 || current.isEmpty() || !resource.matches(current)) {
				return 0;
			}

			final int extracted = Math.min(this.container().extractSlot(this.index, amount, true).getCount(), current.getCount());
			if (extracted <= 0) {
				return 0;
			}

			this.updateSnapshots(transaction);
			final int left = current.getCount() - extracted;
			this.container().setItemSilently(this.index, left > 0 ? current.copyWithCount(left) : ItemStack.EMPTY);
			return extracted;
		}

		@Override
		public boolean isResourceBlank() {
			return this.getStack().isEmpty();
		}

		@Override
		public ItemVariant getResource() {
			return ItemVariant.of(this.getStack());
		}

		@Override
		public long getAmount() {
			return this.getStack().getCount();
		}

		@Override
		public long getCapacity() {
			final ItemStack stack = this.getStack();
			return stack.isEmpty() ? this.container().getMaxStackSize() : Math.min(this.container().getMaxStackSize(), stack.getMaxStackSize());
		}

		@Override
		protected ItemStack createSnapshot() {
			return this.getStack().copy();
		}

		@Override
		protected void readSnapshot(final ItemStack snapshot) {
			this.container().setItemSilently(this.index, snapshot);
		}

		@Override
		protected void releaseSnapshot(final ItemStack snapshot) {
			// the last snapshot released before onFinalCommit is the one taken when the outermost transaction started
			this.lastReleasedSnapshot = snapshot;
		}

		@Override
		protected void onFinalCommit() {
			final ItemStack before = this.lastReleasedSnapshot;
			this.lastReleasedSnapshot = ItemStack.EMPTY;
			this.container().onTransferCommitted(this.index, before, this.getStack());
		}

		@Override
		public String toString() {
			return "ContainerSlotStorage[" + this.owner.container + "#" + this.index + "]";
		}
	}
}
