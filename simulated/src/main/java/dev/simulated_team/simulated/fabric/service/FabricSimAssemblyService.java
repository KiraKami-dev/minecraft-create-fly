package dev.simulated_team.simulated.fabric.service;

import dev.simulated_team.simulated.service.SimAssemblyService;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class FabricSimAssemblyService implements SimAssemblyService {

	/**
	 * Vanilla's sticky block rule (NeoForge exposed it as {@code BlockState#canStickTo}): slime and honey don't stick to
	 * each other, and anything sticks to a sticky block.
	 */
	@Override
	public boolean canStickTo(final BlockState stateA, final BlockState stateB) {
		if (stateA.is(Blocks.HONEY_BLOCK) && stateB.is(Blocks.SLIME_BLOCK)) {
			return false;
		}

		if (stateA.is(Blocks.SLIME_BLOCK) && stateB.is(Blocks.HONEY_BLOCK)) {
			return false;
		}

		return isSticky(stateA) || isSticky(stateB);
	}

	private static boolean isSticky(final BlockState state) {
		return state.is(Blocks.SLIME_BLOCK) || state.is(Blocks.HONEY_BLOCK);
	}
}
