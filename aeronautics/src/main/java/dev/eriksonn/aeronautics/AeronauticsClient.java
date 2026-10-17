package dev.eriksonn.aeronautics;

import com.zurrtum.create.client.AllModels;
import com.zurrtum.create.client.foundation.block.connected.SimpleCTBehaviour;
import com.zurrtum.create.client.infrastructure.model.CTModel;
import com.zurrtum.create.client.ponder.foundation.PonderIndex;
import dev.eriksonn.aeronautics.content.blocks.hot_air.HotAirClient;
import dev.eriksonn.aeronautics.content.blocks.mounted_potato_cannon.MountedPotatoCannonClient;
import dev.eriksonn.aeronautics.content.blocks.propeller.PropellerClient;
import dev.eriksonn.aeronautics.content.display_sources.GasDisplaySourceRender;
import dev.eriksonn.aeronautics.content.particle.AeroParticleProviders;
import dev.eriksonn.aeronautics.content.ponder.AeroPonderPlugin;
import dev.eriksonn.aeronautics.fabric.content.fluids.levitite.LevititeBlendClient;
import dev.eriksonn.aeronautics.index.AeroBlockEntityBehaviours;
import dev.eriksonn.aeronautics.index.AeroBlockEntityRenders;
import dev.eriksonn.aeronautics.index.AeroBlockLayers;
import dev.eriksonn.aeronautics.index.AeroBlocks;
import dev.eriksonn.aeronautics.index.AeroClickInteractions;
import dev.eriksonn.aeronautics.index.AeroEntityRenders;
import dev.eriksonn.aeronautics.index.AeroPartialModels;
import dev.eriksonn.aeronautics.index.AeroSpriteShift;
import dev.eriksonn.aeronautics.index.client.AeroClientRegistries;
import dev.eriksonn.aeronautics.index.client.AeroRenderTypes;
import dev.eriksonn.aeronautics.index.client.AeroSituationalMusic;
import dev.simulated_team.simulated.index.SimItemTooltips;

public class AeronauticsClient {

    /**
     * Client initialization. Called from the Fabric client entrypoint ({@code fabric.AeronauticsFabricClient}),
     * before the first resource reload.
     */
    public static void init() {
        PonderIndex.addPlugin(new AeroPonderPlugin());

        AeroClientRegistries.init();
        AeroPartialModels.init();
        AeroSituationalMusic.init();
        AeroClickInteractions.init();

        // what Registrate did per entry on the client
        AeroBlockLayers.register();
        AeroBlockEntityRenders.register();
        AeroBlockEntityBehaviours.register();
        AeroEntityRenders.register();
        // upstream's registrate tooltip modifier factory: Create's item description + kinetic stats, epic palette
        SimItemTooltips.registerAll(Aeronautics.getRegistrate(), SimItemTooltips.DEFAULT_FACTORY);
        // connected textures (upstream: Registrate onRegister(connectedTextures(...)))
        AllModels.register(AeroBlocks.LEVITITE, CTModel.of(new SimpleCTBehaviour(AeroSpriteShift.LEVITITE)));
        AllModels.register(AeroBlocks.PEARLESCENT_LEVITITE, CTModel.of(new SimpleCTBehaviour(AeroSpriteShift.PEARLESCENT_LEVITITE)));

        // client halves of content, split out by their areas (after the defaults above: their tooltip behaviours
        // replace the default kinetic tooltip, and the last registration wins)
        HotAirClient.registerClient();
        PropellerClient.registerClient();
        MountedPotatoCannonClient.registerClient();
        AeroParticleProviders.registerClient();
        GasDisplaySourceRender.registerClient();

        // Levitite Blend: world/tank rendering, fog (upstream: the NeoForge client fluid type extensions)
        LevititeBlendClient.register();
        // Levitite shimmer/ghost layers on sub-levels are drawn by their own pipelines (content.blocks.levitite.render);
        // this only resets their state on disconnect. The balloon hot-air effect renders in its own frame pass.
        AeroRenderTypes.registerClient();
    }
}
