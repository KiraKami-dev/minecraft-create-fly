package dev.simulated_team.simulated.fabric.service;

import com.zurrtum.create.foundation.fluid.FluidHelper;
import com.zurrtum.create.infrastructure.fluids.FluidItemInventory;
import com.zurrtum.create.infrastructure.fluids.FluidStack;
import dev.simulated_team.simulated.service.SimFluidService;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

public class FabricSimFluidService implements SimFluidService {

    /**
     * Fabric fluid amounts are in droplets (81 per millibucket), like Create Fly's.
     */
    @Override
    public long mbToLoaderUnits(final long mb) {
        return mb * (FluidConstants.BUCKET / 1000);
    }

    @Override
    public Fluid getFluidInItem(final ItemStack stack) {
        try (final FluidItemInventory inventory = FluidHelper.getFluidInventory(stack)) {
            if (inventory != null && inventory.size() > 0) {
                final FluidStack fluid = inventory.getStack(0);
                if (!fluid.isEmpty()) {
                    return fluid.getFluid();
                }
            }
        }
        return null;
    }
}
