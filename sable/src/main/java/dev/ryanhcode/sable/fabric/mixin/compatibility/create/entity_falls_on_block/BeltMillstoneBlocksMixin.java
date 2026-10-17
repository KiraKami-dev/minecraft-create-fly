package dev.ryanhcode.sable.fabric.mixin.compatibility.create.entity_falls_on_block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.zurrtum.create.AllBlocks;
import com.zurrtum.create.content.kinetics.belt.BeltBlock;
import com.zurrtum.create.content.kinetics.millstone.MillstoneBlock;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({ BeltBlock.class, MillstoneBlock.class })
public class BeltMillstoneBlocksMixin extends Block {

    public BeltMillstoneBlocksMixin(final Properties pProperties) {
        super(pProperties);
    }

	@WrapOperation(method = "updateEntityMovementAfterFallOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;blockPosition()Lnet/minecraft/core/BlockPos;"))
	public BlockPos sable$checkForSubLevels(final Entity instance, final Operation<BlockPos> original) {
		final Level level = instance.level();

		final Block block;
		if ((Object) this instanceof BeltBlock) {
			block = AllBlocks.BELT;
		} else {
			block = AllBlocks.MILLSTONE;
		}

        final ActiveSableCompanion helper = Sable.HELPER;
        final BlockPos gatheredBeltPos = helper.runIncludingSubLevels(level, instance.position(), true, null, (subLevel, internalPos) -> {
			if (level.getBlockState(internalPos).is(block)) {
				return internalPos;
			} else if (level.getBlockState(internalPos.below()).is(block)) {
				return internalPos.below();
			}

			return null;
		});

		if (gatheredBeltPos != null) {
			return gatheredBeltPos;
		}

		return original.call(instance);
	}
}
