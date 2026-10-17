package dev.simulated_team.simulated.index;

import com.mojang.serialization.Lifecycle;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.client.BlockPropertiesTooltip;
import dev.simulated_team.simulated.content.blocks.nav_table.navigation_target.NavigationTarget;
import dev.ryanhcode.sable.veillite.platform.registry.RegistrationProvider;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public class SimRegistries {
	public static class Keys {
		public static final ResourceKey<Registry<NavigationTarget>> NAVIGATION_TARGET = key("navigation_target");
		public static final ResourceKey<Registry<BlockPropertiesTooltip.Entry>> PROPERTY_TOOLTIP = key("property_tooltip");

		private static <T> ResourceKey<Registry<T>> key(final String name) {
			return ResourceKey.createRegistryKey(Simulated.path(name));
		}
	}

	public static final Registry<NavigationTarget> NAVIGATION_TARGET = registry(Keys.NAVIGATION_TARGET);
	// Client-only content (filled by BlockPropertiesTooltip.init() during client init, never synced). On a dedicated
	// server it would stay empty, which vanilla reports as an error, so there it is a standalone registry outside the root
	public static final Registry<BlockPropertiesTooltip.Entry> PROPERTY_TOOLTIP =
			FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT
					? registry(Keys.PROPERTY_TOOLTIP)
					: new MappedRegistry<>(Keys.PROPERTY_TOOLTIP, Lifecycle.stable());

	private static <T> Registry<T> registry(final ResourceKey<Registry<T>> registryKey) {
		final RegistrationProvider<T> provider = RegistrationProvider.get(registryKey, Simulated.MOD_ID);
		return provider.asVanillaRegistry();
	}

	public static void register() {}
}
