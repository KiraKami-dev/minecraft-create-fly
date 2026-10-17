package dev.eriksonn.aeronautics.index;

import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.api.levitite_blend_crystallization.CrystalPropagationContext;
import dev.eriksonn.aeronautics.content.blocks.levitite.LevititeCrystalPropagationContext;
import dev.eriksonn.aeronautics.content.blocks.levitite.LevititeSoulCrystalPropagationContext;
import net.minecraft.core.Registry;

import java.util.function.Supplier;

public class AeroLevititeBlendPropagationContexts {
	public static final CrystalPropagationContext
			STANDARD_CONTEXT = create("standard", LevititeCrystalPropagationContext::new),
			SOUL_CONTEXT = create("soul", LevititeSoulCrystalPropagationContext::new);

	public static <T extends CrystalPropagationContext> T create(final String id, final Supplier<T> context) {
		return Registry.register(AeroRegistries.LEVITITE_CRYSTAL_PROPAGATION_CONTEXT, Aeronautics.path(id), context.get());
	}

	public static void register() {
	}
}
