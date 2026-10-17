package dev.simulated_team.simulated.content.blocks.altitude_sensor;

import com.zurrtum.create.api.behaviour.movement.MovementBehaviour;
import com.zurrtum.create.content.contraptions.behaviour.MovementContext;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import net.minecraft.util.Tuple;

/**
 * Keeps the dial height of a sensor on a contraption; the rendering is {@link AltitudeSensorMovementRenderBehaviour}
 * (client, attached in {@code SimRedstoneBlocksClient}).
 */
public class AltitudeSensorMovementBehaviour extends MovementBehaviour {

    @Override
    public boolean disableBlockEntityRendering() {
        return true;
    }

    @Override
    public void tick(final MovementContext context) {
        super.tick(context);

        // temporaryData <- (previousVisualHeight, visualHeight)
        final float yPos = (float) Sable.HELPER.projectOutOfSubLevel(context.world, JOMLConversion.toJOML(context.position)).y;
        if (context.temporaryData instanceof final Tuple<?, ?> heights) {
            context.temporaryData = new Tuple<>(heights.getB(), yPos);
        } else {
            context.temporaryData = new Tuple<>(yPos, yPos);
        }
    }
}
