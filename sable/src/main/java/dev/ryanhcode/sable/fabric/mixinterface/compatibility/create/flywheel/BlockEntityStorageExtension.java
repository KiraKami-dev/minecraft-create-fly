package dev.ryanhcode.sable.fabric.mixinterface.compatibility.create.flywheel;

import net.minecraft.client.multiplayer.ClientLevel;

public interface BlockEntityStorageExtension {
    /**
     * Removes stale sub-level embeddings and moves the others to their sub-level's render pose.
     */
    void sable$preFlywheelFrame(ClientLevel level, float partialTick);
}
