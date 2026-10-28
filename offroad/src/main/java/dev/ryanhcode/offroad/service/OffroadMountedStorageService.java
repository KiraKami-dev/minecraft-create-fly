package dev.ryanhcode.offroad.service;

import com.zurrtum.create.content.contraptions.MountedStorageManager;
import dev.simulated_team.simulated.multiloader.inventory.InventoryLoaderWrapper;
import dev.simulated_team.simulated.service.ServiceUtil;
import dev.ryanhcode.offroad.content.blocks.borehead_bearing.BoreheadAttachedStorage;

public interface OffroadMountedStorageService {

    OffroadMountedStorageService INSTANCE = ServiceUtil.load(OffroadMountedStorageService.class);

    <T extends MountedStorageManager & BoreheadAttachedStorage> T getSidedBoreheadContraptionMountedStorage();

    /**
     * PORT(1.21.11): upstream's NeoForge storage wrapped its item handlers so that only the borehead itself can insert
     * and every extraction unstalls it. Create Fly's containers have too many insert/extract paths for that, so the
     * gate sits on the one wrapper everything uses instead (the borehead's own inserts and the exposed item storage).
     *
     * @return the borehead contraption's items, gated like upstream's NeoForge storage
     */
    InventoryLoaderWrapper wrapBoreheadStorage(MountedStorageManager storage);

}
