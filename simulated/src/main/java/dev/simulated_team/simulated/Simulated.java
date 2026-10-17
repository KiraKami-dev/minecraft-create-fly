package dev.simulated_team.simulated;

import com.mojang.logging.LogUtils;
import dev.ryanhcode.sable.platform.SableEventPlatform;
import dev.simulated_team.simulated.data.advancements.SimAdvancementTriggers;
import dev.simulated_team.simulated.data.advancements.SimAdvancements;
import dev.simulated_team.simulated.events.SimulatedCommonEvents;
import dev.simulated_team.simulated.index.*;
import dev.simulated_team.simulated.index.fabric.FabricSimStats;
import dev.simulated_team.simulated.network.SimPacketManager;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import dev.simulated_team.simulated.service.SimModCompatibilityService;
import dev.simulated_team.simulated.util.SimAssemblyHelper;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

public final class Simulated {
    public static final String MOD_ID = "simulated";
    public static final String MOD_NAME = "Create Simulated";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final SimulatedRegistrate REGISTRATE = new SimulatedRegistrate(path("simulated"), MOD_ID);

    /**
     * Common initialization: registers all content. Called from the Fabric main entrypoint
     * ({@code fabric.SimulatedFabric}); the server config is created there right after, since it needs the stress
     * defaults collected here.
     */
    public static void init() {
        SimEntityDataSerializers.register();

        SimRegistries.register();
        SimDataComponents.register();
        SimTags.register();
        SimSoundEvents.init();
        SimBlocks.register();
        SimItems.register();
        SimBlockEntityTypes.register();
        SimParticleTypes.register();
        SimPacketManager.init();
        SimEntityTypes.register();
        SimMenuTypes.register();
        SimDisplaySources.register();
        SimNavigationTargets.register();
        SimItemAttributeTypes.init();
        SimRecipeSerializers.register();
        SimArmInteractions.init();
        FabricSimStats.register();

        // advancement triggers ("<id>_builtin" criteria of the shipped advancement JSON)
        SimAdvancements.register();
        SimAdvancementTriggers.register();

        SimItemUseOverrides.register();
        SimulatedCommonEvents.register();
        SimBlockMovementChecks.register();
        SimAssemblyHelper.register();
        SimModCompatibilityService.initLoaded();

        SableEventPlatform.INSTANCE.onPhysicsTick(SimulatedCommonEvents::onPhysicsTick);
        SableEventPlatform.INSTANCE.onPostPhysicsTick(SimulatedCommonEvents::onPostPhysicsTick);
    }

    public static SimulatedRegistrate getRegistrate() {
        return REGISTRATE;
    }

    public static Identifier path(final String path) {
        return Identifier.tryBuild(MOD_ID, path);
    }

}
