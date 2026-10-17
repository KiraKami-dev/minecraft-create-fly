package dev.ryanhcode.sable.fabric.mixin.compatibility.create.belt;

import com.zurrtum.create.content.kinetics.belt.BeltBlock;
import dev.ryanhcode.sable.api.block.BlockWithSubLevelCollisionCallback;
import dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback;
import dev.ryanhcode.sable.fabric.physics.callback.BeltBlockCallback;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Allows belts on Sub-Levels to obtain entities as passengers
 */
@Mixin(BeltBlock.class)
public class BeltBlockMixin implements BlockWithSubLevelCollisionCallback {

    @Override
    public BlockSubLevelCollisionCallback sable$getCallback() {
        return BeltBlockCallback.INSTANCE;
    }
}
