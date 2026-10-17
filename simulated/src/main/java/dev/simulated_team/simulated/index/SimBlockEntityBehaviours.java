package dev.simulated_team.simulated.index;

import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.client.AllBlockEntityBehaviours;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.audio.KineticAudioBehaviour;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.tooltip.KineticTooltipBehaviour;
import com.zurrtum.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Client: client-only block entity behaviours (Create Fly moved them out of the block entities into
 * {@link BlockEntityBehaviour#CLIENT_REGISTRY}, see {@link AllBlockEntityBehaviours}). Every kinetic block entity
 * type gets Create's kinetic audio and goggle tooltip.
 */
public class SimBlockEntityBehaviours {

    private static <T extends KineticBlockEntity> void kinetic(final BlockEntityType<T> type) {
        AllBlockEntityBehaviours.add(type, KineticAudioBehaviour::new, KineticTooltipBehaviour::new);
    }

    public static void register() {
        kinetic(SimBlockEntityTypes.SIMPLE_BE);
        kinetic(SimBlockEntityTypes.TORSION_SPRING);
        kinetic(SimBlockEntityTypes.ROPE_WINCH);
        kinetic(SimBlockEntityTypes.PORTABLE_ENGINE);
        kinetic(SimBlockEntityTypes.SWIVEL_BEARING);
        kinetic(SimBlockEntityTypes.STEERING_WHEEL);
        kinetic(SimBlockEntityTypes.SWIVEL_BEARING_LINK_BLOCK);
        kinetic(SimBlockEntityTypes.AUGER_SHAFT);
        kinetic(SimBlockEntityTypes.DIRECTIONAL_GEARSHIFT);
    }
}
