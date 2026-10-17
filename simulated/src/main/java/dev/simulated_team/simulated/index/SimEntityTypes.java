package dev.simulated_team.simulated.index;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.entities.diagram.DiagramEntity;
import dev.simulated_team.simulated.content.entities.honey_glue.HoneyGlueEntity;
import dev.simulated_team.simulated.content.entities.launched_plunger.LaunchedPlungerEntity;
import dev.simulated_team.simulated.service.SimEntityService;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * Entity types. Renderers are registered on the client in {@code SimEntityRenders}.
 */
public class SimEntityTypes {

    public static final EntityType<HoneyGlueEntity> HONEY_GLUE = register(
            "honey_glue",
            EntityType.Builder.of(HoneyGlueEntity::create, MobCategory.MISC),
            new EntityLoaderData(10, Integer.MAX_VALUE, 0.1f, 0.1f, 0, false, true, false)
    );

    public static final EntityType<LaunchedPlungerEntity> PLUNGER = register(
            "launched_plunger",
            EntityType.Builder.of(LaunchedPlungerEntity::create, MobCategory.MISC),
            new EntityLoaderData(10, 5, 0.5f, 0.5f, 0.25f, true, true, false)
    );

    public static final EntityType<DiagramEntity> CONTRAPTION_DIAGRAM = register(
            "contraption_diagram",
            EntityType.Builder.of(DiagramEntity::create, MobCategory.MISC),
            new EntityLoaderData(10, Integer.MAX_VALUE, 0.1f, 0.1f, 0.0f, false, true, true)
    );

    private static <T extends Entity> EntityType<T> register(final String name, final EntityType.Builder<T> builder, final EntityLoaderData data) {
        return Simulated.getRegistrate().entity(name, applyLoaderSpecificTransform(builder, data));
    }

    public static <T extends Entity> EntityType.Builder<T> applyLoaderSpecificTransform(final EntityType.Builder<T> builder, final EntityLoaderData data) {
        return SimEntityService.INSTANCE.loaderEntityTransform(builder, data);
    }

    public record EntityLoaderData(int clientTrackingRange, int updateFrequency, float width, float height, float eyeHeight, boolean sendVelocity, boolean immuneToFire, boolean fixed) {}

    public static void register() {
    }
}
