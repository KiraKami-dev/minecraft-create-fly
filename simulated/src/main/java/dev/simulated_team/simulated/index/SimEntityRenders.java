package dev.simulated_team.simulated.index;

import dev.simulated_team.simulated.content.entities.diagram.DiagramEntityRenderer;
import dev.simulated_team.simulated.content.entities.honey_glue.HoneyGlueRenderer;
import dev.simulated_team.simulated.content.entities.launched_plunger.LaunchedPlungerEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * Client: entity renderers (Create Fly's {@code AllEntityRenders}).
 */
public class SimEntityRenders {

    public static <T extends Entity> void render(final EntityType<? extends T> type, final EntityRendererProvider<T> factory) {
        EntityRenderers.register(type, factory);
    }

    public static void register() {
        render(SimEntityTypes.HONEY_GLUE, HoneyGlueRenderer::new);
        render(SimEntityTypes.PLUNGER, LaunchedPlungerEntityRenderer::new);
        render(SimEntityTypes.CONTRAPTION_DIAGRAM, DiagramEntityRenderer::new);
    }
}
