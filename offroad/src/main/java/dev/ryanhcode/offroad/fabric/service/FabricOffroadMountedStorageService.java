package dev.ryanhcode.offroad.fabric.service;

import com.zurrtum.create.content.contraptions.MountedStorageManager;
import dev.ryanhcode.offroad.content.blocks.borehead_bearing.BoreheadAttachedStorage;
import dev.ryanhcode.offroad.content.blocks.borehead_bearing.BoreheadBearingBlockEntity;
import dev.ryanhcode.offroad.fabric.transfer.BoreheadInventoryWrapper;
import dev.ryanhcode.offroad.service.OffroadMountedStorageService;
import dev.simulated_team.simulated.multiloader.inventory.InventoryLoaderWrapper;

import java.lang.ref.WeakReference;

/**
 * Fabric half of the borehead's contraption storage (upstream: {@code NeoForgeOffroadMountedStorageService}).
 */
public class FabricOffroadMountedStorageService implements OffroadMountedStorageService {

    @Override
    @SuppressWarnings("unchecked")
    public <T extends MountedStorageManager & BoreheadAttachedStorage> T getSidedBoreheadContraptionMountedStorage() {
        return (T) new FabricBoreheadBearingMountedStorage();
    }

    @Override
    public InventoryLoaderWrapper wrapBoreheadStorage(final MountedStorageManager storage) {
        return new BoreheadInventoryWrapper((FabricBoreheadBearingMountedStorage) storage);
    }

    public static class FabricBoreheadBearingMountedStorage extends MountedStorageManager implements BoreheadAttachedStorage {

        public WeakReference<BoreheadBearingBlockEntity> attachedBoreheadBearing = new WeakReference<>(null);

        private boolean insertAllowed;

        @Override
        public void attachBlockEntity(final BoreheadBearingBlockEntity be) {
            this.attachedBoreheadBearing = new WeakReference<>(be);
        }

        @Override
        public void setInsertAllowed(final boolean insertionAllowed) {
            this.insertAllowed = insertionAllowed;
        }

        public boolean isInsertAllowed() {
            return this.insertAllowed;
        }

        @Override
        public void invokeUnstall() {
            final BoreheadBearingBlockEntity bbe = this.attachedBoreheadBearing.get();
            if (bbe != null) {
                bbe.startUnstalling();
            }
        }
    }
}
