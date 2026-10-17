package dev.ryanhcode.sable.fabric.mixin.compatibility.create.chain_conveyor;

import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.zurrtum.create.content.kinetics.chainConveyor.ChainConveyorPackage;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(ChainConveyorBlockEntity.class)
public abstract class ChainConveyorBlockEntityMixin {

    @Shadow(remap = false)
    public Map<BlockPos, ChainConveyorBlockEntity.ConnectionStats> connectionStats;

    @Shadow(remap = false)
    Map<BlockPos, List<ChainConveyorPackage>> travellingPackages;

    @Shadow(remap = false)
    private void drop(final ChainConveyorPackage box) {
        throw new AssertionError();
    }

    @Inject(method = "removeInvalidConnections", at = @At(value = "INVOKE", target = "Ljava/util/Iterator;remove()V"), remap = false)
    public void dropInvalidPackages(final CallbackInfo ci, @Local(name = "next") final BlockPos next) {
        this.connectionStats.remove(next);

        final List<ChainConveyorPackage> packages = this.travellingPackages.remove(next);
        if (packages != null && !packages.isEmpty()) {
            for (final ChainConveyorPackage box : packages) {
                this.drop(box);
            }
        }
    }
}
