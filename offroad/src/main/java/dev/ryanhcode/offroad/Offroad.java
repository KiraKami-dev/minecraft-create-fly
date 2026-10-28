package dev.ryanhcode.offroad;

import dev.ryanhcode.offroad.events.OffroadCommonEvents;
import dev.ryanhcode.offroad.index.*;
import dev.ryanhcode.offroad.network.OffroadPacketManager;
import dev.ryanhcode.sable.platform.SableEventPlatform;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Offroad {
	public static final String MOD_ID = "offroad";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	// Offroad's items go into their own section of the Simulated creative tab
	private static final SimulatedRegistrate REGISTRATE = new SimulatedRegistrate(Offroad.path(MOD_ID), MOD_ID);

	/**
	 * Common initialization: registers all content. Called from the Fabric main entrypoint ({@code fabric.OffroadFabric});
	 * the server config is created there right after, since it needs the stress defaults collected here. (Item tooltips,
	 * upstream's registrate tooltip modifier factory, are client-side now: {@code OffroadClient}.)
	 */
	public static void init() {
		OffroadDataComponents.init();
		OffroadBlocks.register();
		// block items before anything calls Block#asItem() on an Offroad block (it caches the result)
		OffroadItems.register();
		OffroadBlockEntityTypes.register();
		OffroadEntityTypes.register();
		OffroadSoundEvents.init();
		OffroadPacketManager.init();

		OffroadContraptionTypes.register();

		listenCommonEvents();
	}

	private static void listenCommonEvents() {
		SableEventPlatform.INSTANCE.onPhysicsTick(OffroadCommonEvents::physicsTick);
	}

	public static SimulatedRegistrate getRegistrate() {
		return REGISTRATE;
	}

	public static Identifier path(final String path) {
		return Identifier.tryBuild(MOD_ID, path);
	}
}
