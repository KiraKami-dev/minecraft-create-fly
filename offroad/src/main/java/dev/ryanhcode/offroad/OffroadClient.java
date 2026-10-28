package dev.ryanhcode.offroad;

import com.zurrtum.create.client.AllBlockEntityBehaviours;
import com.zurrtum.create.client.content.contraptions.render.ControlledContraptionEntityRenderer;
import com.zurrtum.create.client.content.contraptions.render.ControlledContraptionVisual;
import com.zurrtum.create.client.flywheel.lib.visualization.SimpleEntityVisualizer;
import com.zurrtum.create.client.ponder.foundation.PonderIndex;
import dev.ryanhcode.offroad.content.blocks.borehead_bearing.BoreheadBearingRenderer;
import dev.ryanhcode.offroad.content.blocks.borehead_bearing.BoreheadBearingTooltipBehaviour;
import dev.ryanhcode.offroad.content.blocks.borehead_bearing.BoreheadBearingVisual;
import dev.ryanhcode.offroad.content.blocks.rock_cutting_wheel.RockCuttingWheelMovementRenderBehaviour;
import dev.ryanhcode.offroad.content.blocks.rock_cutting_wheel.RockCuttingWheelRenderer;
import dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountRenderer;
import dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountValueBehaviour;
import dev.ryanhcode.offroad.content.ponder.OffroadPonderPlugin;
import dev.ryanhcode.offroad.index.OffroadBlockEntityTypes;
import dev.ryanhcode.offroad.index.OffroadBlocks;
import dev.ryanhcode.offroad.index.OffroadEntityTypes;
import dev.ryanhcode.offroad.index.OffroadPartialModels;
import dev.simulated_team.simulated.index.SimItemTooltips;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.entity.EntityRenderers;

import static dev.simulated_team.simulated.index.SimBlockEntityRenders.normal;
import static dev.simulated_team.simulated.index.SimBlockEntityRenders.render;

public class OffroadClient {

	/**
	 * Client initialization. Called from the Fabric client entrypoint ({@code fabric.OffroadFabricClient}), before the
	 * first resource reload.
	 */
	public static void init() {
		PonderIndex.addPlugin(new OffroadPonderPlugin());

		OffroadPartialModels.init();

		// what Registrate did per entry on the client
		// upstream: addLayer(() -> RenderType::cutoutMipped) (1.21.11 has one cutout layer); Create Fly only honours a model's render_type for partials
		BlockRenderLayerMap.putBlocks(ChunkSectionLayer.CUTOUT,
				OffroadBlocks.BOREHEAD_BEARING_BLOCK,
				OffroadBlocks.ROCK_CUTTER_BLOCK,
				OffroadBlocks.WHEEL_MOUNT
		);

		// upstream: .renderer(..) and .visual(..) (renderer kept running)
		render(OffroadBlockEntityTypes.WHEEL_MOUNT, WheelMountRenderer::new);
		normal(OffroadBlockEntityTypes.BOREHEAD_BEARING, BoreheadBearingRenderer::new, BoreheadBearingVisual::new);
		render(OffroadBlockEntityTypes.ROCKCUTTING_WHEEL_BLOCK_ENTITY, RockCuttingWheelRenderer::new);

		// Create Fly keeps value boxes on the client (the block entity adds only the server half)
		AllBlockEntityBehaviours.add(OffroadBlockEntityTypes.WHEEL_MOUNT, WheelMountValueBehaviour::new);
		AllBlockEntityBehaviours.add(OffroadBlockEntityTypes.BOREHEAD_BEARING, BoreheadBearingTooltipBehaviour::new);

		// the rock cutting wheel's contraption rendering (upstream: the actor's renderInContraption / createVisual)
		OffroadBlocks.ROCK_CUTTER_ACTOR.attachRender = new RockCuttingWheelMovementRenderBehaviour();

		// upstream registered Create's generic ContraptionEntityRenderer/ContraptionVisual; Create Fly moved the bearing
		// transform into the controlled-contraption pair, which this entity (a ControlledContraptionEntity) needs
		EntityRenderers.register(OffroadEntityTypes.BOREHEAD_CONTRAPTION_ENTITY, ControlledContraptionEntityRenderer::new);
		SimpleEntityVisualizer.builder(OffroadEntityTypes.BOREHEAD_CONTRAPTION_ENTITY)
				.factory(ControlledContraptionVisual::new)
				.skipVanillaRender(entity -> false)
				.apply();

		// upstream's registrate tooltip modifier factory: Create's item description + kinetic stats, epic palette
		SimItemTooltips.registerAll(Offroad.getRegistrate(), SimItemTooltips.DEFAULT_FACTORY);
	}
}
