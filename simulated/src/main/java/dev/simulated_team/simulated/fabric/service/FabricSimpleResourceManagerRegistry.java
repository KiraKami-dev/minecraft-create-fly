package dev.simulated_team.simulated.fabric.service;

import dev.simulated_team.simulated.api.SimpleResourceManager;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;

/**
 * Registers {@link SimpleResourceManager}s as client resource reload listeners (NeoForge used
 * {@code RegisterClientReloadListenersEvent}). The managers are created during client initialisation, before the first
 * resource reload.
 */
public class FabricSimpleResourceManagerRegistry implements SimpleResourceManager.Registry {

	@Override
	public void registerListener(final Identifier id, final PreparableReloadListener listener) {
		ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloader(id, listener);
	}
}
