package dev.ryanhcode.sable.fabric.mixin.compatibility.create.inventory_manipulation;

import com.google.common.base.Predicate;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.zurrtum.create.foundation.blockEntity.behaviour.inventory.CapManipulationBehaviourBase;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Lets funnels, chutes, deployers etc. (anything using a {@link CapManipulationBehaviourBase}) find inventories on
 * sub-levels. Create Fly resolves the inventory through the abstract
 * {@code getCapability(Level, BlockPos, BlockEntity, Direction)} instead of {@code Level#getCapability}.
 */
@Mixin(CapManipulationBehaviourBase.class)
public class CapManipulationBehaviourBaseMixin {

    @Shadow(remap = false) protected Predicate<BlockEntity> filter;

    @Unique
    private BlockPos sable$caughtPos;

    @Redirect(method = "findNewCapability", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;", remap = true), remap = false)
    public BlockEntity sable$findNewCapOnSubLevel(final Level level, final BlockPos blockPos) {
        // Falls back to the original position (e.g. block-only inventories such as composters) if no block entity is found
        this.sable$caughtPos = blockPos;

        final ActiveSableCompanion helper = Sable.HELPER;
        return helper.runIncludingSubLevels(level, blockPos.getCenter(), true, helper.getContaining(level, blockPos), (subLevel, internalPos) -> {
            final BlockEntity caughtBE = level.getBlockEntity(internalPos);
            if (caughtBE != null && this.filter.test(caughtBE)) {
                this.sable$caughtPos = internalPos;
                return caughtBE;
            }

            return null;
        });
    }

    @WrapOperation(method = "findNewCapability", at = @At(value = "INVOKE", target = "Lcom/zurrtum/create/foundation/blockEntity/behaviour/inventory/CapManipulationBehaviourBase;getCapability(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/core/Direction;)Ljava/lang/Object;"), remap = false)
    public Object sable$redirectPos(final CapManipulationBehaviourBase<?, ?> instance, final Level level, final BlockPos pos, final BlockEntity blockEntity, final Direction side, final Operation<Object> original) {
        return original.call(instance, level, this.sable$caughtPos != null ? this.sable$caughtPos : pos, blockEntity, side);
    }
}
