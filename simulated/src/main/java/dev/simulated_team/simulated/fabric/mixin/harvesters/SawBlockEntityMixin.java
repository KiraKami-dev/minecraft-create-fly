package dev.simulated_team.simulated.fabric.mixin.harvesters;

import com.zurrtum.create.content.kinetics.saw.SawBlockEntity;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import dev.simulated_team.simulated.content.blocks.auger_shaft.BlockHarvester;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Logs of trees cut by a saw go to an attached auger first. The saw gets {@link BlockHarvester} from
 * {@link BlockBreakingKineticBlockEntityMixin}.
 */
@Mixin(SawBlockEntity.class)
public abstract class SawBlockEntityMixin extends SmartBlockEntity {

    public SawBlockEntityMixin(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "dropItemFromCutTree", at = @At("HEAD"), cancellable = true, remap = false)
    private void simulated$checkDeposit(final BlockPos pos, final ItemStack stack, final CallbackInfo ci) {
        if (((BlockHarvester) this).depositItemStack(this.worldPosition, stack).isEmpty()) {
            ci.cancel();
        }
    }
}
