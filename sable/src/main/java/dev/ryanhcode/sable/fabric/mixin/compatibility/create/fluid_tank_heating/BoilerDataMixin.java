package dev.ryanhcode.sable.fabric.mixin.compatibility.create.fluid_tank_heating;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.zurrtum.create.api.boiler.BoilerHeater;
import com.zurrtum.create.content.fluids.tank.BoilerData;
import com.zurrtum.create.content.fluids.tank.FluidTankBlockEntity;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BoilerData.class)
public class BoilerDataMixin {

    @Shadow(remap = false)
    public boolean needsHeatLevelUpdate;

    @Unique
    private int sable$ticksUntilUpdate = 20;


    @Inject(method = "tick", at = @At(value = "FIELD", target = "Lcom/zurrtum/create/content/fluids/tank/BoilerData;ticksUntilNextSample:I", ordinal = 0), remap = false)
    public void sable$forceUpdateHeatIfDisconnected(final FluidTankBlockEntity controller, final CallbackInfo ci) {
        if (this.sable$ticksUntilUpdate-- <= 0) {
            this.sable$ticksUntilUpdate = 20;
            this.needsHeatLevelUpdate = true;
        }
    }

    @WrapOperation(method = "updateTemperature", at = @At(value = "INVOKE", target = "Lcom/zurrtum/create/api/boiler/BoilerHeater;findHeat(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)F"), remap = false)
    public float sable$subLevelHeating(final Level level, final BlockPos pos, final BlockState state, final Operation<Float> original) {
        final float originalHeat = original.call(level, pos, state);
        if (originalHeat != BoilerHeater.NO_HEAT) {
            return originalHeat;
        }

        final ActiveSableCompanion helper = Sable.HELPER;
        final Float gatheredHeat = helper.runIncludingSubLevels(level, pos.getCenter(), false, helper.getContaining(level, pos), (subLevel, internalPos) -> {
            final float internalHeat = original.call(level, internalPos, level.getBlockState(internalPos));

            if (internalHeat != BoilerHeater.NO_HEAT) {
                return internalHeat;
            }

            return null;

        });

        if (gatheredHeat != null) {
            return gatheredHeat;
        }

        return BoilerHeater.NO_HEAT;
    }
}
