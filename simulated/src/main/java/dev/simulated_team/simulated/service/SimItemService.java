package dev.simulated_team.simulated.service;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface SimItemService {

    SimItemService INSTANCE = ServiceUtil.load(SimItemService.class);

    static DyeColor getDyeColor(final ItemStack itemStack) {
        return itemStack.getItem() instanceof final DyeItem dyeItem ? dyeItem.getDyeColor() : null;
    }

    /**
     * Burn time of the given stack in a furnace. Fuel values are level dependent since 1.21.2; this uses the fuel values
     * of the running server (or of the client level when there is no local server).
     */
    int getBurnTime(final ItemStack stack);

    /**
     * Burn time of the given stack in a furnace, using the fuel values of the given level.
     */
    int getBurnTime(final Level level, final ItemStack stack);

    int getSuperheatedBurnTime(final ItemStack stack);
}
