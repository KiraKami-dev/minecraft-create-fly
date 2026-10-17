package dev.eriksonn.aeronautics.index;

import com.zurrtum.create.infrastructure.fluids.FlowableFluid;
import com.zurrtum.create.infrastructure.fluids.FluidEntry;
import com.zurrtum.create.infrastructure.fluids.FluidInteractionRegistry;
import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.fabric.content.fluids.levitite.LevititeBlendFluid;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributeHandler;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;

/**
 * Fluids, Create Fly style ({@code AllFluids}; upstream: the NeoForge module's {@code AeroFluidsNeoForge}). The fluid
 * block is {@link AeroBlocks#LEVITITE_BLEND}, the bucket {@link AeroItems#LEVITITE_BLEND_BUCKET}; client rendering is
 * registered by {@code fabric.content.fluids.levitite.LevititeBlendClient}.
 */
public class AeroFluids {
	public static final LevititeBlendFluid.Source LEVITITE_BLEND = register("levitite_blend", LevititeBlendFluid.Source::new, LevititeBlendFluid.Flowing::new);

	private static <S extends FlowableFluid, F extends FlowableFluid> S register(final String name, final Function<FluidEntry, S> still, final Function<FluidEntry, F> flowing) {
		final Identifier id = Aeronautics.path(name);
		final FluidEntry entry = new FluidEntry();
		final S source = still.apply(entry);
		entry.still = source;
		entry.flowing = flowing.apply(entry);
		Registry.register(BuiltInRegistries.FLUID, ResourceKey.create(Registries.FLUID, id), entry.still);
		Registry.register(BuiltInRegistries.FLUID, ResourceKey.create(Registries.FLUID, id.withPrefix("flowing_")), entry.flowing);
		return source;
	}

	public static FlowableFluid getFlowing(final FlowableFluid fluid) {
		return fluid.getEntry().flowing;
	}

	/**
	 * Fluid interactions and Fabric transfer attributes (upstream: {@code AeroFluidsNeoForge.registerFluidInteractions}
	 * on common setup and the fluid type's bucket sounds).
	 */
	public static void register() {
		// lava next to levitite blend: obsidian from a lava source, calcite from flowing lava
		FluidInteractionRegistry.addInteraction(Fluids.LAVA, new FluidInteractionRegistry.InteractionInformation(LEVITITE_BLEND,
				fluidState -> fluidState.isSource() ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.CALCITE.defaultBlockState()));

		final FluidVariantAttributeHandler attributes = new FluidVariantAttributeHandler() {
			@Override
			public Optional<SoundEvent> getFillSound(final FluidVariant variant) {
				return Optional.of(AeroSoundEvents.LEVITITE_BLEND_FILL.event());
			}

			@Override
			public Optional<SoundEvent> getEmptySound(final FluidVariant variant) {
				return Optional.of(AeroSoundEvents.LEVITITE_BLEND_EMPTY.event());
			}

			@Override
			public int getViscosity(final FluidVariant variant, @Nullable final net.minecraft.world.level.Level world) {
				// NeoForge viscosity 1500 (water 1000 = Fabric 5)
				return 7;
			}
		};
		FluidVariantAttributes.register(LEVITITE_BLEND, attributes);
		FluidVariantAttributes.register(getFlowing(LEVITITE_BLEND), attributes);
	}

	public static boolean isLevititeBlend(final Fluid fluid) {
		return LEVITITE_BLEND.isSame(fluid);
	}
}
