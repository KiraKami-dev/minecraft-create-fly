package dev.simulated_team.simulated.fabric.service;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.service.SimTabService;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;

public class FabricSimTabService implements SimTabService {

	/**
	 * Id of Simulated's creative tab (as on NeoForge, where it was registered as {@code simulated:main_tab}).
	 */
	public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Simulated.path("main_tab"));

	private CreativeModeTab tab;

	@Override
	public CreativeModeTab getCreativeTab() {
		if (this.tab == null) {
			this.tab = BuiltInRegistries.CREATIVE_MODE_TAB.getValue(TAB_KEY);
		}
		return this.tab;
	}
}
