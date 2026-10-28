package dev.ryanhcode.offroad.fabric.transfer;

import dev.ryanhcode.offroad.content.blocks.borehead_bearing.BoreheadAttachedStorage;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The container a player sees when opening a storage block of a borehead contraption (upstream: NeoForge's
 * {@code WrappedWrappedMountedItemStorage}): taking items out unstalls the borehead.
 */
public class BoreheadMenuContainer implements Container {

    private final BoreheadAttachedStorage storage;
    private final Container wrapped;

    public BoreheadMenuContainer(final BoreheadAttachedStorage storage, final Container wrapped) {
        this.storage = storage;
        this.wrapped = wrapped;
    }

    @Override
    public int getContainerSize() {
        return this.wrapped.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return this.wrapped.isEmpty();
    }

    @Override
    public ItemStack getItem(final int slot) {
        return this.wrapped.getItem(slot);
    }

    @Override
    public ItemStack removeItem(final int slot, final int amount) {
        final ItemStack removed = this.wrapped.removeItem(slot, amount);
        if (!removed.isEmpty()) {
            this.storage.invokeUnstall();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(final int slot) {
        final ItemStack removed = this.wrapped.removeItemNoUpdate(slot);
        if (!removed.isEmpty()) {
            this.storage.invokeUnstall();
        }
        return removed;
    }

    @Override
    public void setItem(final int slot, final ItemStack stack) {
        if (stack.isEmpty()) {
            this.storage.invokeUnstall();
        }
        this.wrapped.setItem(slot, stack);
    }

    @Override
    public int getMaxStackSize() {
        return this.wrapped.getMaxStackSize();
    }

    @Override
    public void setChanged() {
        this.wrapped.setChanged();
    }

    @Override
    public boolean stillValid(final Player player) {
        return this.wrapped.stillValid(player);
    }

    @Override
    public boolean canPlaceItem(final int slot, final ItemStack stack) {
        return this.wrapped.canPlaceItem(slot, stack);
    }

    @Override
    public void clearContent() {
        this.wrapped.clearContent();
    }
}
