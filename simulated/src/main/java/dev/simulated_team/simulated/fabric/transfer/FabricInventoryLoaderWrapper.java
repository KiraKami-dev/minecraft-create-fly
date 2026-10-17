package dev.simulated_team.simulated.fabric.transfer;

import dev.simulated_team.simulated.multiloader.inventory.InventoryLoaderWrapper;
import dev.simulated_team.simulated.multiloader.inventory.ItemInfoWrapper;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

/**
 * Wraps another block's inventory, as returned by Create Fly's {@code ItemHelper.getInventory} (a vanilla container, or
 * Create Fly's container view of a Fabric item storage), using Create Fly's container helpers
 * ({@code count}/{@code extract}/{@code insert}, the "simulate" variants being {@code count*}). Fabric half of NeoForge's
 * {@code InventoryLoaderWrapperImpl}.
 */
public class FabricInventoryLoaderWrapper extends InventoryLoaderWrapper {

    private static final Predicate<ItemStack> ANY = stack -> true;

    private final Container attachedInventory;

    public FabricInventoryLoaderWrapper(final Container attachedInventory) {
        this.attachedInventory = attachedInventory;
    }

    @Override
    public ItemStack extractAny(final int maxAmount, final boolean simulate, final boolean exact) {
        final ItemStack extracted;
        if (exact) {
            extracted = simulate ? this.attachedInventory.preciseCount(ANY, maxAmount) : this.attachedInventory.preciseExtract(ANY, maxAmount);
        } else {
            extracted = simulate ? this.attachedInventory.countAny(maxAmount) : this.attachedInventory.extractAny(maxAmount);
        }

        if (this.callback != null && !extracted.isEmpty() && !simulate) {
            this.callback.accept(true);
        }

        return extracted;
    }

    @Override
    public int insertGeneral(final ItemInfoWrapper info, final int amountToInsert, final boolean simulate) {
        if (amountToInsert <= 0) {
            return 0;
        }

        final ItemStack is = ItemInfoWrapper.generateFromInfo(info);
        is.setCount(amountToInsert);

        final int amountInserted = simulate ? this.attachedInventory.countSpace(is, amountToInsert) : this.attachedInventory.insert(is, amountToInsert);
        if (this.callback != null && amountInserted > 0 && !simulate) {
            this.callback.accept(false);
        }

        return amountInserted;
    }

    @Override
    public ItemStack insertSlot(final ItemStack stack, final int slot, final boolean simulate) {
        if (stack.isEmpty()) {
            return stack;
        }

        final int count = stack.getCount();
        final int inserted = simulate ? this.attachedInventory.countSpace(stack, count, slot, slot + 1) : this.attachedInventory.insert(stack, count, slot, slot + 1);
        if (inserted <= 0) {
            return stack;
        }

        if (this.callback != null && !simulate) {
            this.callback.accept(false);
        }

        return inserted >= count ? ItemStack.EMPTY : stack.copyWithCount(count - inserted);
    }

    @Override
    public int extractGeneral(final ItemInfoWrapper info, final int amountToExtract, final boolean simulate) {
        if (amountToExtract <= 0) {
            return 0;
        }

        final Predicate<ItemStack> filter = stack -> stack.getItem() == info.type();
        final int extractAmount = (simulate ? this.attachedInventory.count(filter, amountToExtract) : this.attachedInventory.extract(filter, amountToExtract)).getCount();
        if (this.callback != null && extractAmount > 0 && !simulate) {
            this.callback.accept(true);
        }

        return extractAmount;
    }

    @Override
    public ItemStack extractSlot(final int index, final int amountToExtract, final boolean simulate) {
        if (amountToExtract <= 0 || index < 0 || index >= this.attachedInventory.getContainerSize()) {
            return ItemStack.EMPTY;
        }

        final ItemStack inSlot = this.attachedInventory.getItem(index);
        if (inSlot.isEmpty() || (this.attachedInventory instanceof final WorldlyContainer sided && !sided.canTakeItemThroughFace(index, inSlot, null))) {
            return ItemStack.EMPTY;
        }

        final int amount = Math.min(amountToExtract, inSlot.getCount());
        final ItemStack extracted;
        if (simulate) {
            extracted = inSlot.copyWithCount(amount);
        } else {
            // removeItem goes through a transaction for storage-backed containers, and mutates the slot for vanilla ones
            extracted = this.attachedInventory.removeItem(index, amount);
            this.attachedInventory.setChanged();
        }

        if (this.callback != null && !extracted.isEmpty() && !simulate) {
            this.callback.accept(true);
        }

        return extracted;
    }

    @Override
    public int getContainerSize() {
        return this.attachedInventory.getContainerSize();
    }

    @Override
    public int getMaxStackSize() {
        return this.attachedInventory.getMaxStackSize();
    }

    @Override
    public @NotNull ItemStack getItem(final int slot) {
        return this.attachedInventory.getItem(slot);
    }
}
