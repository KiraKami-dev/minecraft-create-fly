package dev.eriksonn.aeronautics.fabric.events;

import dev.eriksonn.aeronautics.events.AeronauticsCommonEvents;
import dev.eriksonn.aeronautics.index.AeroTags;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.registry.FuelRegistryEvents;
import net.minecraft.server.level.ServerLevel;

/**
 * Fabric listeners for upstream's NeoForge game-bus events ({@code AeroNeoForgeCommonEvents}). Mod-bus registration
 * events (arm interaction points, advancement triggers) run straight from {@code Aeronautics.init()}.
 */
public final class AeroFabricCommonEvents {
	private AeroFabricCommonEvents() {
	}

	public static void register() {
		// ServerStoppedEvent
		ServerLifecycleEvents.SERVER_STOPPED.register(AeronauticsCommonEvents::onServerStopped);

		// ServerTickEvent.Post
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (final ServerLevel level : server.getAllLevels()) {
				AeronauticsCommonEvents.onServerTickEnd(level);
			}
		});

		// upstream data map neoforge:furnace_fuels: envelopes burn for 100 ticks
		FuelRegistryEvents.BUILD.register((builder, context) -> builder.add(AeroTags.ItemTags.ENVELOPE, 100));
	}
}
