package dev.eriksonn.aeronautics.index;

import com.zurrtum.create.client.flywheel.lib.visualization.SimpleEntityVisualizer;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.contraption.PropellerBearingContraptionEntityRenderer;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.contraption.PropellerBearingContraptionVisual;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.NoopRenderer;

/**
 * Client: entity renderers (Create Fly's {@code AllEntityRenders}). Upstream registered Create's generic
 * {@code ContraptionEntityRenderer}/{@code ContraptionVisual}, whose transform came from the entity's
 * {@code applyLocalTransforms}; Create Fly moved that transform into the renderer/visual, so the propeller contraption
 * has its own pair (bearing spin + gyroscopic tilt, like Create Fly's controlled-contraption pair).
 */
public class AeroEntityRenders {

    public static void register() {
        EntityRenderers.register(AeroEntityTypes.PROPELLER_CONTROLLED_CONTRAPTION, PropellerBearingContraptionEntityRenderer::new);
        SimpleEntityVisualizer.builder(AeroEntityTypes.PROPELLER_CONTROLLED_CONTRAPTION)
                .factory(PropellerBearingContraptionVisual::new)
                .skipVanillaRender(entity -> false)
                .apply();

        EntityRenderers.register(AeroEntityTypes.GUST, NoopRenderer::new);
    }
}
