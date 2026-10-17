package dev.simulated_team.simulated.fabric.service.compat;

import dan200.computercraft.api.network.wired.WiredElement;
import dan200.computercraft.api.network.wired.WiredElementLookup;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.PeripheralLookup;
import dev.simulated_team.simulated.service.compat.SimPeripheralService;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * CC: Tweaked on Fabric looks peripherals and wired elements up through Fabric API block lookups (NeoForge used block
 * capabilities). Only loaded when CC: Tweaked is present ({@code ComputerCraftPeripherals}).
 */
public class FabricSimPeripheralService implements SimPeripheralService {

    @Override
    public <T extends BlockEntity> void addPeripheral(final BlockEntityType<T> type, final CapabilityGetter<T, IPeripheral> getter) {
        PeripheralLookup.get().registerForBlockEntity(getter::get, type);
    }

    @Override
    public <T extends BlockEntity> void addWired(final BlockEntityType<T> type, final CapabilityGetter<T, WiredElement> getter) {
        WiredElementLookup.get().registerForBlockEntity(getter::get, type);
    }
}
