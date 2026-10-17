package dev.simulated_team.simulated;

import com.zurrtum.create.client.catnip.render.SuperByteBufferCache;
import com.zurrtum.create.client.content.contraptions.wrench.RadialWrenchMenu;
import com.zurrtum.create.client.ponder.foundation.PonderIndex;
import dev.ryanhcode.sable.veillite.platform.VeilEventPlatform;
import dev.simulated_team.simulated.client.BlockPropertiesTooltip;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerTooltipBehaviour;
import dev.simulated_team.simulated.content.blocks.steering_wheel.SteeringWheelRenderer;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.SwivelBearingClient;
import dev.simulated_team.simulated.content.display_sources.SimDisplaySourceRenders;
import dev.simulated_team.simulated.content.end_sea.EndSeaShadowRenderer;
import dev.simulated_team.simulated.content.items.merging_glue.MergingGlueItemHandler;
import dev.simulated_team.simulated.content.items.plunger_launcher.PlungerLauncherItemRenderer;
import dev.simulated_team.simulated.content.particle.SimParticleProviders;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffClientHandler;
import dev.simulated_team.simulated.events.SimulatedCommonClientEvents;
import dev.simulated_team.simulated.index.SimBlockEntityBehaviours;
import dev.simulated_team.simulated.index.SimBlockEntityRenders;
import dev.simulated_team.simulated.index.SimBlockLayers;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.index.SimEntityRenders;
import dev.simulated_team.simulated.index.SimItemTooltips;
import dev.simulated_team.simulated.index.SimMenuScreens;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.index.SimResourceManagers;
import dev.simulated_team.simulated.index.SimSpriteShifts;
import dev.simulated_team.simulated.index.ponder.SimPonderPlugin;
import net.minecraft.core.registries.BuiltInRegistries;

public class SimulatedClient {

    public static final PhysicsStaffClientHandler PHYSICS_STAFF_CLIENT_HANDLER = new PhysicsStaffClientHandler();
    public static PlungerLauncherItemRenderer.RenderHandler PLUNGER_LAUNCHER_RENDER_HANDLER = new PlungerLauncherItemRenderer.RenderHandler();
	public static final MergingGlueItemHandler MERGING_GLUE_ITEM_HANDLER = new MergingGlueItemHandler();

    /**
     * Client initialization. Called from the Fabric client entrypoint ({@code fabric.SimulatedFabricClient}), before
     * the first resource reload.
     */
    public static void init() {
        SimPartialModels.init();
        SimSpriteShifts.init();
        BlockPropertiesTooltip.init();
        SimResourceManagers.init();

        // what Registrate did per entry on the client
        SimBlockLayers.register();
        SimBlockEntityRenders.register();
        SimBlockEntityBehaviours.register();
        SimEntityRenders.register();
        SimMenuScreens.register();
        SimItemTooltips.register(Simulated.getRegistrate());
        RadialWrenchMenu.registerBlacklistedBlock(BuiltInRegistries.BLOCK.getKey(SimBlocks.SWIVEL_BEARING));
        RadialWrenchMenu.registerBlacklistedBlock(BuiltInRegistries.BLOCK.getKey(SimBlocks.SWIVEL_BEARING_LINK_BLOCK));
        RadialWrenchMenu.registerBlacklistedBlock(BuiltInRegistries.BLOCK.getKey(SimBlocks.PAIRED_DOCKING_CONNECTOR));

        // client halves of content, split out by their areas (after the defaults above: last registration wins)
        PlungerLauncherItemRenderer.registerClient();
        SimParticleProviders.registerClient();
        SimDisplaySourceRenders.registerClient();
        SwivelBearingClient.registerClient();
        PhysicsAssemblerTooltipBehaviour.registerClient();

        PonderIndex.addPlugin(new SimPonderPlugin());

        VeilEventPlatform.INSTANCE.onVeilRenderLevelStage(EndSeaShadowRenderer::renderShadowMap);

        // PORT-TODO(1.21.11): upstream added EndSeaFadeTransformer as a Veil shader preprocessor
        //  (onVeilAddShaderProcessors); veillite has no shader processors, the end sea fade needs a re-implementation.
        // Upstream also registered SimRenderTypes.laser/lens/staffOverlay as Veil "fixed buffers" for specific render
        // stages; 1.21.11 render types are drawn by the feature renderer, so there is nothing to register.

        VeilEventPlatform.INSTANCE.onVeilRenderLevelStage(SimulatedCommonClientEvents::onRenderLevelStage);

        SuperByteBufferCache.getInstance().registerCompartment(SteeringWheelRenderer.STEERING_WHEEL);
    }
}
