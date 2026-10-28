package dev.ryanhcode.offroad.index;

import dev.ryanhcode.offroad.Offroad;
import dev.ryanhcode.offroad.content.entities.BoreheadContraptionEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * Entity types. The renderer and contraption visual are registered on the client in {@code OffroadClient}.
 */
public class OffroadEntityTypes {

    public static final EntityType<BoreheadContraptionEntity> BOREHEAD_CONTRAPTION_ENTITY = Offroad.getRegistrate().entity(
            "borehead_contraption_entity",
            EntityType.Builder.<BoreheadContraptionEntity>of(BoreheadContraptionEntity::new, MobCategory.MISC)
                    .clientTrackingRange(20)
                    .updateInterval(40)
                    .sized(1, 1)
                    .eyeHeight(0)
                    .fireImmune()
    );

    public static void register() {
    }
}
