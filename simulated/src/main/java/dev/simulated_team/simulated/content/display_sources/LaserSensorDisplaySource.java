package dev.simulated_team.simulated.content.display_sources;

import com.zurrtum.create.content.redstone.displayLink.DisplayLinkContext;
import com.zurrtum.create.content.redstone.displayLink.source.NumericSingleLineDisplaySource;
import com.zurrtum.create.content.redstone.displayLink.target.DisplayTargetStats;
import dev.simulated_team.simulated.content.blocks.lasers.laser_sensor.LaserSensorBlockEntity;
import net.minecraft.network.chat.MutableComponent;

public class LaserSensorDisplaySource extends NumericSingleLineDisplaySource {
    @Override
    protected MutableComponent provideLine(final DisplayLinkContext context, final DisplayTargetStats displayTargetStats) {
        if (!(context.getSourceBlockEntity() instanceof final LaserSensorBlockEntity be)) {
            return EMPTY.getFirst();
        }

        return (be.closestHitDistance == Double.MAX_VALUE) ?
                EMPTY.getFirst() :
                DisplaySourceLang.number(be.closestHitDistance)
                        .append(" ")
                        .append("block" + (be.closestHitDistance != 1 ? "s" : ""));
    }

    // configuration widgets: SimDisplaySourceRenders (client)

    @Override
    protected String getTranslationKey() {
        return "laser_sensor.data";
    }

    @Override
    public boolean allowsLabeling(final DisplayLinkContext displayLinkContext) {
        return true;
    }
}
