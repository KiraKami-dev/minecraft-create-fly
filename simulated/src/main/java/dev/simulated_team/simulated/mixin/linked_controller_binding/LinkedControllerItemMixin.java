package dev.simulated_team.simulated.mixin.linked_controller_binding;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.zurrtum.create.content.redstone.link.controller.LinkedControllerItem;
import dev.simulated_team.simulated.index.SimBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LinkedControllerItem.class)
public abstract class LinkedControllerItemMixin {
	// Create Fly's static onItemUseFirst checks hitState.is(AllBlocks.X) where Create used AllBlocks.X.has(hitState);
	// ordinal 1 is still the redstone link check (0 is the lectern controller check of the sneaking branch).
	@WrapOperation(method = "onItemUseFirst", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z", ordinal = 1))
	private static boolean simulated$onItemUseFirst(final BlockState instance, final Block block, final Operation<Boolean> original) {
		return original.call(instance, block) || instance.is(SimBlocks.MODULATING_LINKED_RECEIVER) || instance.is(SimBlocks.DIRECTIONAL_LINKED_RECEIVER);
	}
}
