package dev.ryanhcode.sable.fabric.compatibility.create;

import com.zurrtum.create.client.flywheel.lib.visualization.VisualizationHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.level.block.entity.BlockEntity;

public class FlywheelCompatFabric {
    public static final boolean FLYWHEEL_LOADED = FabricLoader.getInstance().isModLoaded("create");

    /**
     * Tries to add a flywheel visual for a block-entity.
     * Done for single block sub-level renderers, because we're not compiling their sections.
     */
    public static void tryAddVisual(final BlockEntity blockEntity) {
        VisualizationHelper.tryAddBlockEntity(blockEntity);
    }
}
