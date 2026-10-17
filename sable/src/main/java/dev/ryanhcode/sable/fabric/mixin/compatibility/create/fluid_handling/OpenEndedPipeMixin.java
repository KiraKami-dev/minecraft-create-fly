package dev.ryanhcode.sable.fabric.mixin.compatibility.create.fluid_handling;

import com.zurrtum.create.catnip.math.BlockFace;
import com.zurrtum.create.content.fluids.OpenEndedPipe;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OpenEndedPipe.class)
public abstract class OpenEndedPipeMixin {
    @Shadow(remap = false)
    @Final
    @Mutable
    private BlockPos outputPos;

    @Shadow(remap = false)
    private Level world;

    @Unique
    private BlockPos sable$plotOutputPos;

    // Create Fly has a second (private, codec) constructor, which delegates to this one
    @Inject(method = "<init>(Lcom/zurrtum/create/catnip/math/BlockFace;)V", at = @At("TAIL"), remap = false)
    private void sable$saveCurrentPos(final BlockFace face, final CallbackInfo ci) {
        this.sable$plotOutputPos = this.outputPos;
    }

    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;", remap = true), remap = false)
    private BlockState sable$getBlockstateInclSublevels(final Level level, final BlockPos pos) {
        this.outputPos = this.sable$plotOutputPos;

        final ActiveSableCompanion helper = Sable.HELPER;
        final Vec3 checkPos = Vec3.atCenterOf(this.sable$plotOutputPos);
        BlockState gatheredState = helper.runIncludingSubLevels(level, checkPos, true, helper.getContaining(level, checkPos), this::sable$gatherState);
        if (gatheredState == null) {
            this.outputPos = this.sable$plotOutputPos;
            gatheredState = level.getBlockState(this.sable$plotOutputPos);
        }

        return gatheredState;
    }

    @Unique
    private BlockState sable$gatherState(final SubLevel level, final BlockPos b) {
        final BlockState checkedState = this.world.getBlockState(b);
        if (!checkedState.isAir()) {
            this.outputPos = b;
            return checkedState;
        }

        return null;
    }

    @Redirect(method = "provideFluidToSpace",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z", ordinal = 1, remap = true), remap = false)
    private boolean sable$preventInWorldPlace(final Level instance, final BlockPos pPos, final BlockState pNesubleveltate, final int pFlags) {
        return instance.setBlock(this.sable$plotOutputPos, pNesubleveltate, 3);
    }

}
