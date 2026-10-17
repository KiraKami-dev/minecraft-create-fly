package dev.ryanhcode.sable.fabric.mixinterface.compatibility.create.flywheel;

import com.zurrtum.create.client.flywheel.api.visualization.VisualizationContext;
import dev.ryanhcode.sable.fabric.mixinhelper.compatibility.create.flywheel.SubLevelEmbedding;
import dev.ryanhcode.sable.sublevel.SubLevel;

public interface BlockEntityStorageExtension {
    void sable$setPlanVisualizationContext(VisualizationContext visualizationContext);

    SubLevelEmbedding sable$getEmbeddingInfo(SubLevel subLevel);

    void sable$preFlywheelFrame();
}
