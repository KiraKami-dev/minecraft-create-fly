package dev.simulated_team.simulated;

import com.zurrtum.create.client.content.contraptions.wrench.RadialWrenchMenu;
import com.zurrtum.create.client.ponder.foundation.PonderIndex;
import dev.simulated_team.simulated.client.BlockPropertiesTooltip;
import dev.simulated_team.simulated.content.blocks.SimKineticBlocksClient;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerTooltipBehaviour;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.SwivelBearingClient;
import dev.simulated_team.simulated.content.display_sources.SimDisplaySourceRenders;
import dev.simulated_team.simulated.content.items.merging_glue.MergingGlueItemHandler;
import dev.simulated_team.simulated.content.items.plunger_launcher.PlungerLauncherItemRenderer;
import dev.simulated_team.simulated.content.particle.SimParticleProviders;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffClient;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffClientHandler;
import dev.simulated_team.simulated.index.SimBlockEntityBehaviours;
import dev.simulated_team.simulated.index.SimBlockEntityRenders;
import dev.simulated_team.simulated.index.SimBlockLayers;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.index.SimEntityRenders;
import dev.simulated_team.simulated.index.SimItemTooltips;
import dev.simulated_team.simulated.index.SimMenuScreens;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.index.SimRenderTypes;
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
        SimRenderTypes.init();
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
        SimKineticBlocksClient.registerClient();
        SwivelBearingClient.registerClient();
        PhysicsAssemblerTooltipBehaviour.registerClient();
        // physics staff item model + world render hook (beams, lock icons, hover box)
        PhysicsStaffClient.registerClient();

        PonderIndex.addPlugin(new SimPonderPlugin());

        // The End Sea shadow map and fade are driven by its own frame pass and mixins (content.end_sea), not by
        // Veil render stages or shader preprocessors. Upstream also registered SimRenderTypes.laser/lens/staffOverlay
        // as Veil "fixed buffers"; 1.21.11 render types are drawn by the feature renderer, so there is nothing to register.
        // (the steering wheel's SuperByteBufferCache compartment is registered by SimKineticBlocksClient)
    }
}
