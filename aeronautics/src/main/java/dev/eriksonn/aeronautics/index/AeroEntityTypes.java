package dev.eriksonn.aeronautics.index;

import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.content.blocks.hot_air.gust.GustEntity;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.contraption.PropellerBearingContraptionEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * Entity types. Renderers (and the contraption visual) are registered on the client in {@code AeronauticsClient}.
 */
public class AeroEntityTypes {

	public static final EntityType<PropellerBearingContraptionEntity> PROPELLER_CONTROLLED_CONTRAPTION = Aeronautics.getRegistrate().entity(
			"propeller_bearing_contraption",
			EntityType.Builder.<PropellerBearingContraptionEntity>of(PropellerBearingContraptionEntity::new, MobCategory.MISC)
					.clientTrackingRange(20)
					.updateInterval(40)
					.sized(1, 1)
					.eyeHeight(0)
					.fireImmune()
	);

	public static final EntityType<GustEntity> GUST = Aeronautics.getRegistrate().entity(
			"gust",
			EntityType.Builder.<GustEntity>of(GustEntity::new, MobCategory.MISC)
					.clientTrackingRange(20)
					.sized(1, 1)
					.eyeHeight(0)
					.fireImmune()
	);

	public static void register() {}
}
