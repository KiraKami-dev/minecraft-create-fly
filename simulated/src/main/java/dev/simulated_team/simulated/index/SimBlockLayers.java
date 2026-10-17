package dev.simulated_team.simulated.index;

import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.world.level.block.Block;

/**
 * Client: chunk render layers (upstream Registrate {@code .addLayer(() -> RenderType::cutoutMipped)}; 1.21.11 has no
 * separate mipped cutout layer, mipmapping is per atlas). Partial models that need cutout declare {@code render_type}
 * in their model JSON instead (read by Create Fly's model loader).
 */
public class SimBlockLayers {

    private static void cutout(final Block... blocks) {
        BlockRenderLayerMap.putBlocks(ChunkSectionLayer.CUTOUT, blocks);
    }

    public static void register() {
        cutout(
                SimBlocks.SWIVEL_BEARING,
                SimBlocks.MERGING_GLUE,
                SimBlocks.ROPE_WINCH,
                SimBlocks.ROPE_CONNECTOR,
                SimBlocks.DIRECTIONAL_GEARSHIFT,
                SimBlocks.TORSION_SPRING,
                SimBlocks.AUGER_SHAFT,
                SimBlocks.AUGER_COG,
                SimBlocks.LASER_POINTER,
                SimBlocks.VELOCITY_SENSOR,
                SimBlocks.ALTITUDE_SENSOR,
                SimBlocks.GIMBAL_SENSOR,
                SimBlocks.NAVIGATION_TABLE,
                SimBlocks.DOCKING_CONNECTOR,
                SimBlocks.ANALOG_TRANSMISSION,
                SimBlocks.STEERING_WHEEL,
                SimBlocks.THROTTLE_LEVER,
                SimBlocks.LINKED_TYPEWRITER,
                SimBlocks.DIRECTIONAL_LINKED_RECEIVER,
                SimBlocks.MODULATING_LINKED_RECEIVER,
                SimBlocks.REDSTONE_ACCUMULATOR,
                SimBlocks.REDSTONE_INDUCTOR,
                SimBlocks.REDSTONE_MAGNET
        );
        cutout(SimBlocks.PORTABLE_ENGINES.toArray());
        cutout(SimBlocks.NAMEPLATES.toArray());
    }
}
