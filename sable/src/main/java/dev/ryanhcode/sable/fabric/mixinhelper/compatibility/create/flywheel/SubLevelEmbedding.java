package dev.ryanhcode.sable.fabric.mixinhelper.compatibility.create.flywheel;

import com.zurrtum.create.client.flywheel.api.visualization.VisualEmbedding;
import com.zurrtum.create.client.flywheel.api.visualization.VisualizationContext;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

/**
 * The Flywheel embedding that a sub-level's block entity visuals live in, with the context it was created from.
 */
public record SubLevelEmbedding(VisualEmbedding embedding, VisualizationContext parent, List<BlockEntity> blockEntities) {
}
