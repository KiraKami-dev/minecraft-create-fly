package dev.eriksonn.aeronautics.fabric.events;

import dev.eriksonn.aeronautics.events.AeronauticsClientEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * Fabric listeners for upstream's NeoForge client events ({@code AeroNeoForgeClientEvents}).
 */
public final class AeroFabricClientEvents {
	private AeroFabricClientEvents() {
	}

	public static void register() {
		// ClientTickEvent.Pre / Post
		ClientTickEvents.START_CLIENT_TICK.register(minecraft -> AeronauticsClientEvents.clientLevelTick(false));
		ClientTickEvents.END_CLIENT_TICK.register(minecraft -> AeronauticsClientEvents.clientLevelTick(true));
	}
}
