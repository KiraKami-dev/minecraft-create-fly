package dev.eriksonn.aeronautics.index;

import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

/**
 * Client: chunk render layers. Upstream declared {@code render_type: cutout} in these blocks' model JSON (NeoForge
 * uses it for chunk meshing); Create Fly only honours it for partial models, so the chunk layer is set here.
 * The Levitite blocks' own layers belong to the levitite renderer ({@code AeroRenderTypes}).
 */
public class AeroBlockLayers {

    public static void register() {
        BlockRenderLayerMap.putBlocks(ChunkSectionLayer.CUTOUT,
                AeroBlocks.HOT_AIR_BURNER,
                AeroBlocks.STEAM_VENT,
                AeroBlocks.MOUNTED_POTATO_CANNON
        );
    }
}
