package dev.ryanhcode.sable.fabric.mixin.compatibility.create.frogports;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.zurrtum.create.content.kinetics.chainConveyor.ChainConveyorPackage;
import com.zurrtum.create.content.logistics.packagePort.frogport.FrogportBlockEntity;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.infrastructure.config.AllConfigs;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ChainConveyorBlockEntity.class)
public abstract class ChainConveyorBlockEntityMixin extends SmartBlockEntity {

    public ChainConveyorBlockEntityMixin(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    @WrapOperation(method = "exportToPort", at = @At(value = "INVOKE", target = "Lcom/zurrtum/create/content/logistics/packagePort/frogport/FrogportBlockEntity;isBackedUp()Z"), remap = false)
    public boolean sable$testSublevelDistance(final FrogportBlockEntity instance, final Operation<Boolean> original, @Local(argsOnly = true) final ChainConveyorPackage chainPackage) {
        final Vec3 packagePos = chainPackage.worldPosition;
        if (packagePos == null) {
            return original.call(instance);
        }

        final Vec3 frogPos = instance.getBlockPos().getCenter();

        final int maxRange = AllConfigs.server().logistics.packagePortRange.get() + 2;
        return original.call(instance) || Sable.HELPER.distanceSquaredWithSubLevels(instance.getLevel(), packagePos, frogPos) > maxRange * maxRange;
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lcom/zurrtum/create/content/kinetics/chainConveyor/ChainConveyorBlockEntity;notifyPortToAnticipate(Lnet/minecraft/core/BlockPos;)V"), remap = false)
    public void sable$testSublevelDistance1(final ChainConveyorBlockEntity instance, final BlockPos blockPos, final Operation<Void> original, @Local final ChainConveyorPackage chainPackage) {
        final Vec3 packagePos = chainPackage.worldPosition;
        if (packagePos == null) {
            original.call(instance, blockPos);
            return;
        }

        // blockPos is the port entry's key, the offset of the port from this chain conveyor
        final Vec3 frogPos = this.worldPosition.offset(blockPos).getCenter();

        final int maxRange = AllConfigs.server().logistics.packagePortRange.get() + 2;

        if (Sable.HELPER.distanceSquaredWithSubLevels(this.getLevel(), packagePos, frogPos) < maxRange * maxRange) {
            original.call(instance, blockPos);
        }
    }
}
