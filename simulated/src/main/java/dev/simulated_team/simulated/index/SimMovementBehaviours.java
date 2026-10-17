package dev.simulated_team.simulated.index;

import com.zurrtum.create.api.behaviour.movement.MovementBehaviour;
import dev.simulated_team.simulated.content.blocks.altitude_sensor.AltitudeSensorMovementBehaviour;
import net.minecraft.world.level.block.Block;

/**
 * Movement behaviours of Simulated blocks (Create Fly style, see {@code AllMovementBehaviours}). Client renders of a
 * behaviour are attached separately through {@code behaviour.attachRender} (see {@code AllMovementRenders}).
 */
public class SimMovementBehaviours {
    public static final AltitudeSensorMovementBehaviour ALTITUDE_SENSOR = new AltitudeSensorMovementBehaviour();

    public static void register(final MovementBehaviour behaviour, final Block... blocks) {
        for (final Block block : blocks) {
            MovementBehaviour.REGISTRY.register(block, behaviour);
        }
    }

    public static void register() {
        register(ALTITUDE_SENSOR, SimBlocks.ALTITUDE_SENSOR);
    }
}
