package dev.simulated_team.simulated.fabric.transfer;

import com.google.common.collect.MapMaker;
import dev.simulated_team.simulated.multiloader.tanks.CFluidType;
import dev.simulated_team.simulated.multiloader.tanks.SingleTank;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.util.Tuple;

import java.util.Map;

/**
 * Exposes a {@link SingleTank} as a Fabric fluid {@code Storage} (the Fabric half of NeoForge's {@code SingleTankWrapper}).
 * Amounts are in droplets, as {@link SingleTank} capacities already are on Fabric. Uses the tank's own snapshot hooks.
 */
public final class SingleTankStorage extends SnapshotParticipant<Tuple<CFluidType, Long>> implements SingleSlotStorage<FluidVariant> {

	/**
	 * One wrapper per tank, so that concurrent transactions share their snapshots. Weak values: a wrapper taking part in
	 * an open transaction is kept alive by that transaction.
	 */
	private static final Map<SingleTank, SingleTankStorage> WRAPPERS = new MapMaker().weakValues().makeMap();

	private final SingleTank tank;

	private SingleTankStorage(final SingleTank tank) {
		this.tank = tank;
	}

	public static SingleTankStorage of(final SingleTank tank) {
		return WRAPPERS.computeIfAbsent(tank, SingleTankStorage::new);
	}

	public static FluidVariant toVariant(final CFluidType type) {
		return FluidVariant.of(type.fluid(), type.data());
	}

	public static CFluidType toCType(final FluidVariant variant) {
		return variant.isBlank() ? CFluidType.BLANK : new CFluidType(variant.getFluid(), variant.getComponents());
	}

	@Override
	public long insert(final FluidVariant resource, final long maxAmount, final TransactionContext transaction) {
		StoragePreconditions.notBlankNotNegative(resource, maxAmount);
		return this.tank.insert(toCType(resource), maxAmount, false, () -> this.updateSnapshots(transaction));
	}

	@Override
	public long extract(final FluidVariant resource, final long maxAmount, final TransactionContext transaction) {
		StoragePreconditions.notBlankNotNegative(resource, maxAmount);
		return this.tank.extract(toCType(resource), maxAmount, false, () -> this.updateSnapshots(transaction));
	}

	@Override
	public boolean isResourceBlank() {
		return this.tank.type.isBlank();
	}

	@Override
	public FluidVariant getResource() {
		return toVariant(this.tank.type);
	}

	@Override
	public long getAmount() {
		return this.tank.amount;
	}

	@Override
	public long getCapacity() {
		return this.tank.capacity;
	}

	@Override
	protected Tuple<CFluidType, Long> createSnapshot() {
		return this.tank.createSnapshot();
	}

	@Override
	protected void readSnapshot(final Tuple<CFluidType, Long> snapshot) {
		this.tank.readSnapshot(snapshot.getA(), snapshot.getB());
	}

	@Override
	public String toString() {
		return "SingleTankStorage[" + this.tank.type.fluid() + " " + this.tank.amount + "/" + this.tank.capacity + "]";
	}
}
