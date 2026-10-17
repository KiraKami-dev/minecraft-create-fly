package dev.eriksonn.aeronautics;

import dev.eriksonn.aeronautics.data.AeroAdvancementTriggers;
import dev.eriksonn.aeronautics.events.AeronauticsCommonEvents;
import dev.eriksonn.aeronautics.index.*;
import dev.eriksonn.aeronautics.network.AeroPacketManager;
import dev.eriksonn.aeronautics.registry.AeroRegistrate;
import dev.ryanhcode.sable.platform.SableEventPlatform;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Aeronautics {
	public static final String MOD_ID = "aeronautics";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	// Aeronautics' items go into their own section of the Simulated creative tab
	private static final AeroRegistrate REGISTRATE = new AeroRegistrate(Aeronautics.path("aeronautics"), MOD_ID);

	/**
	 * Common initialization: registers all content. Called from the Fabric main entrypoint
	 * ({@code fabric.AeronauticsFabric}); the server config is created there right after, since it needs the stress
	 * defaults collected here. (Item tooltips, upstream's registrate tooltip modifier factory, are client-side now:
	 * {@code AeronauticsClient}.)
	 */
	public static void init() {
		AeroRegistries.register();
		AeroDataComponents.init();
		AeroSoundEvents.init();
		AeroDisplaySources.register();
		AeroBlocks.register();
		// block items before anything calls Block#asItem() on an Aeronautics block (it caches the result)
		AeroItems.register();
		AeroFluids.register();
		AeroBlockEntityTypes.register();
		AeroEntityTypes.register();
		AeroParticleTypes.register();
		AeroArmorMaterials.register();
		AeroLiftingGasTypes.register();
		AeroBlockMovementChecks.init();
		AeroPacketManager.init();
		AeroLevititeBlendPropagationContexts.register();
		AeroArmInteractionPoints.register();

		// advancement triggers ("<id>_builtin" criteria of the shipped advancement JSON)
		AeroAdvancements.register();
		AeroAdvancementTriggers.register();

		listenCommonEvents();
	}

	private static void listenCommonEvents() {
		SableEventPlatform.INSTANCE.onPhysicsTick(AeronauticsCommonEvents::physicsTick);
		SableEventPlatform.INSTANCE.onSubLevelContainerReady(AeronauticsCommonEvents::onSubLevelContainerReady);
	}

	public static AeroRegistrate getRegistrate() {
		return REGISTRATE;
	}

	public static Identifier path(final String path) {
		return Identifier.tryBuild(MOD_ID, path);
	}
}
