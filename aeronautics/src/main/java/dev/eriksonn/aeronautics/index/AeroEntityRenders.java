package dev.eriksonn.aeronautics.index;

import com.zurrtum.create.client.content.contraptions.render.ControlledContraptionEntityRenderer;
import com.zurrtum.create.client.content.contraptions.render.ControlledContraptionVisual;
import com.zurrtum.create.client.flywheel.lib.visualization.SimpleEntityVisualizer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.NoopRenderer;

/**
 * Client: entity renderers (Create Fly's {@code AllEntityRenders}). Upstream registered Create's generic
 * {@code ContraptionEntityRenderer}/{@code ContraptionVisual}; Create Fly's controlled-contraption pair is the
 * equivalent for {@code ControlledContraptionEntity} subclasses.
 */
public class AeroEntityRenders {

    public static void register() {
        EntityRenderers.register(AeroEntityTypes.PROPELLER_CONTROLLED_CONTRAPTION, ControlledContraptionEntityRenderer::new);
        SimpleEntityVisualizer.builder(AeroEntityTypes.PROPELLER_CONTROLLED_CONTRAPTION)
                .factory(ControlledContraptionVisual::new)
                .skipVanillaRender(entity -> false)
                .apply();

        EntityRenderers.register(AeroEntityTypes.GUST, NoopRenderer::new);
    }
}
