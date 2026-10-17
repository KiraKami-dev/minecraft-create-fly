package dev.ryanhcode.sable.fabric.mixin.compatibility.create.deployer;

import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.catnip.data.Iterate;
import com.zurrtum.create.content.kinetics.deployer.DeployerBlockEntity;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(DeployerBlockEntity.class)
public abstract class DeployerBlockEntityMixin extends SmartBlockEntity {

    public DeployerBlockEntityMixin(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    @Redirect(method = "start", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(DD)D"), remap = false)
    private double sable$deployerMin(final double a, final double b, @Local(ordinal = 1) final Vec3 rayOrigin, @Local(ordinal = 0) final BlockHitResult result) {
        return Math.min(Math.sqrt(Sable.HELPER.distanceSquaredWithSubLevels(this.level, result.getLocation(), rayOrigin)), b);
    }


    @ModifyArg(method = "activate", at = @At(value = "INVOKE", target = "Lcom/zurrtum/create/content/kinetics/deployer/DeployerHandler;activate(Lcom/zurrtum/create/content/kinetics/deployer/DeployerPlayer;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/Vec3;Lcom/zurrtum/create/content/kinetics/deployer/DeployerBlockEntity$Mode;)V"), index = 2, remap = false)
    private BlockPos sable$checkPositions(final BlockPos pos) {
        final Vec3 centerPos = Vec3.atCenterOf(pos);

        final ActiveSableCompanion helper = Sable.HELPER;
        final BlockPos gatheredPos = helper.runIncludingSubLevels(this.getLevel(), centerPos, true, helper.getContaining(this), this::sable$getState);

        if (gatheredPos != null)
            return gatheredPos;

        return pos;
    }

    @Unique
    @Nullable
    private BlockPos sable$getState(final SubLevel subLevel, final BlockPos pos) {
        final Level level = this.getLevel();
        assert level != null;

        final BlockState state = level.getBlockState(pos);

        if (!state.isAir()) {
            return pos;
        } else {
            for (final Direction direction : Iterate.directions) {
                if (!level.getBlockState(pos.relative(direction)).isAir())
                    return pos;
            }
        }

        return null;
    }
}
