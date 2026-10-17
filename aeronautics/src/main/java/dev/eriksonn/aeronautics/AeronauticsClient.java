package dev.eriksonn.aeronautics;

import com.zurrtum.create.client.AllModels;
import com.zurrtum.create.client.foundation.block.connected.SimpleCTBehaviour;
import com.zurrtum.create.client.infrastructure.model.CTModel;
import com.zurrtum.create.client.ponder.foundation.PonderIndex;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.effect.ClientBalloonEffectRenderer;
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
import dev.eriksonn.aeronautics.index.client.AeroSituationalMusic;
import dev.ryanhcode.sable.veillite.platform.VeilEventPlatform;
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

        // Levitite Blend: world/tank rendering, fog (upstream: the NeoForge client fluid type extensions)
        LevititeBlendClient.register();

        registerEvents();
    }

    private static void registerEvents() {
        VeilEventPlatform.INSTANCE.onVeilRenderLevelStage((stage,
                                                           levelRenderer,
                                                           bufferSource,
                                                           matrixStack,
                                                           frustumMatrix,
                                                           projectionMatrix,
                                                           renderTick,
                                                           deltaTracker,
                                                           camera,
                                                           frustum) -> {
            ClientBalloonEffectRenderer.onRenderLevelStage(stage, frustumMatrix, projectionMatrix, renderTick);
        });

        // Upstream also registered AeroRenderTypes.levitite()/levititeGhosts() as Veil block layers and fixed buffers
        // (NeoForge: a ChunkRenderTypeSet for the Levitite blocks). Those custom layers are the levitite renderer's
        // (aero-render) job on 1.21.11; its client hook is wired in by the orchestrator.
    }
}
