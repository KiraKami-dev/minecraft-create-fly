package dev.ryanhcode.offroad.fabric.transfer;

import dev.ryanhcode.offroad.content.blocks.borehead_bearing.BoreheadBearingBlockEntity;
import dev.ryanhcode.offroad.fabric.service.FabricOffroadMountedStorageService.FabricBoreheadBearingMountedStorage;
import dev.simulated_team.simulated.fabric.transfer.FabricInventoryLoaderWrapper;
import dev.simulated_team.simulated.multiloader.inventory.ItemInfoWrapper;
import net.minecraft.world.item.ItemStack;

/**
 * The borehead contraption's items (upstream: {@code NeoForgeBoreheadInvWrapper}): only the borehead may insert (it
 * opens the gate around its own inserts), and extracting needs a borehead bearing and unstalls it.
 */
public class BoreheadInventoryWrapper extends FabricInventoryLoaderWrapper {

    private final FabricBoreheadBearingMountedStorage storage;

    public BoreheadInventoryWrapper(final FabricBoreheadBearingMountedStorage storage) {
        super(storage.getAllItems());
        this.storage = storage;
    }

    @Override
    public int insertGeneral(final ItemInfoWrapper info, final int amountToInsert, final boolean simulate) {
        return this.storage.isInsertAllowed() ? super.insertGeneral(info, amountToInsert, simulate) : 0;
    }

    @Override
    public ItemStack insertSlot(final ItemStack stack, final int slot, final boolean simulate) {
        return this.storage.isInsertAllowed() ? super.insertSlot(stack, slot, simulate) : stack;
    }

    @Override
    public ItemStack extractAny(final int maxAmount, final boolean simulate, final boolean exact) {
        final BoreheadBearingBlockEntity bbe = this.storage.attachedBoreheadBearing.get();
        if (bbe == null) {
            return ItemStack.EMPTY;
        }
        final ItemStack extracted = super.extractAny(maxAmount, simulate, exact);
        if (!extracted.isEmpty()) {
            bbe.startUnstalling();
        }
        return extracted;
    }

    @Override
    public int extractGeneral(final ItemInfoWrapper info, final int amountToExtract, final boolean simulate) {
        final BoreheadBearingBlockEntity bbe = this.storage.attachedBoreheadBearing.get();
        if (bbe == null) {
            return 0;
        }
        final int extracted = super.extractGeneral(info, amountToExtract, simulate);
        if (extracted > 0) {
            bbe.startUnstalling();
        }
        return extracted;
    }

    @Override
    public ItemStack extractSlot(final int index, final int amountToExtract, final boolean simulate) {
        final BoreheadBearingBlockEntity bbe = this.storage.attachedBoreheadBearing.get();
        if (bbe == null) {
            return ItemStack.EMPTY;
        }
        final ItemStack extracted = super.extractSlot(index, amountToExtract, simulate);
        if (!extracted.isEmpty()) {
            bbe.startUnstalling();
        }
        return extracted;
    }
}
