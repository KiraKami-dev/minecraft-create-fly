package dev.simulated_team.simulated.fabric.service;

import com.zurrtum.create.AllItemTags;
import dev.simulated_team.simulated.service.SimItemService;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.FuelValues;
import org.jetbrains.annotations.Nullable;

public class FabricSimItemService implements SimItemService {

    /**
     * Create Fly replaces Create's superheated fuel data map with the {@code create:blaze_burner_fuel/special} tag, burning
     * for a fixed 3200 ticks (see {@code BlazeBurnerBlockEntity#tryUpdateFuel}).
     */
    private static final int SUPERHEATED_BURN_TIME = 3200;

    /**
     * The fuel values of the running server or of the joined client level, kept up to date by
     * {@link dev.simulated_team.simulated.fabric.events.SimFabricCommonEvents} and its client counterpart.
     */
    @Nullable
    private static volatile FuelValues fuelValues;

    public static void setFuelValues(@Nullable final FuelValues values) {
        fuelValues = values;
    }

    @Override
    public int getBurnTime(final ItemStack stack) {
        final FuelValues values = fuelValues;
        return values != null ? values.burnDuration(stack) : 0;
    }

    @Override
    public int getBurnTime(final Level level, final ItemStack stack) {
        return level.fuelValues().burnDuration(stack);
    }

    @Override
    public int getSuperheatedBurnTime(final ItemStack stack) {
        return stack.is(AllItemTags.BLAZE_BURNER_FUEL_SPECIAL) ? SUPERHEATED_BURN_TIME : 0;
    }
}
