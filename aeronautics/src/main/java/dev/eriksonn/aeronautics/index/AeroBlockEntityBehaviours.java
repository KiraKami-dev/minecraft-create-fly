package dev.eriksonn.aeronautics.index;

import com.zurrtum.create.client.AllBlockEntityBehaviours;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.audio.KineticAudioBehaviour;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.tooltip.KineticTooltipBehaviour;
import com.zurrtum.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Client: client-only block entity behaviours (Create Fly moved them out of the block entities, see
 * {@link AllBlockEntityBehaviours}). Every kinetic block entity type gets Create's kinetic audio and goggle tooltip;
 * the content areas register their own extra client behaviours after these (last registration wins).
 */
public class AeroBlockEntityBehaviours {

    private static <T extends KineticBlockEntity> void kinetic(final BlockEntityType<T> type) {
        AllBlockEntityBehaviours.add(type, KineticAudioBehaviour::new, KineticTooltipBehaviour::new);
    }

    public static void register() {
        kinetic(AeroBlockEntityTypes.PROPELLER_BEARING);
        kinetic(AeroBlockEntityTypes.GYROSCOPIC_PROPELLER_BEARING);
        kinetic(AeroBlockEntityTypes.ENVELOPE_ENCASED_SHAFT);
        kinetic(AeroBlockEntityTypes.ANDESITE_PROPELLER);
        kinetic(AeroBlockEntityTypes.WOODEN_PROPELLER);
        kinetic(AeroBlockEntityTypes.SMART_PROPELLER);
        kinetic(AeroBlockEntityTypes.MOUNTED_POTATO_CANNON);
    }
}
