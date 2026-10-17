package dev.simulated_team.simulated.content.display_sources;

import com.zurrtum.create.content.redstone.displayLink.DisplayLinkContext;
import com.zurrtum.create.content.redstone.displayLink.source.NumericSingleLineDisplaySource;
import com.zurrtum.create.content.redstone.displayLink.target.DisplayTargetStats;
import dev.simulated_team.simulated.content.blocks.altitude_sensor.AltitudeSensorBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class AltitudeSensorDisplaySource extends NumericSingleLineDisplaySource {

    @Override
    protected MutableComponent provideLine(final DisplayLinkContext context, final DisplayTargetStats stats) {
        if (!(context.getSourceBlockEntity() instanceof final AltitudeSensorBlockEntity be)) {
            return ZERO.copy();
        }

        switch (context.sourceConfig().getIntOr("AltitudeSensorSelection", 0)) {
            case 0 -> {
                assert be.hasLevel();
                final float airPressure = (float) be.getAirPressure() * 100.0f;
                return Component.literal(String.format("%.2f%%", airPressure));
            }
            case 1 -> {
                return Component.literal(String.format("%.2f", be.getWorldHeight()));
            }
        }
        return EMPTY_LINE.copy();
    }

    // configuration widgets: SimDisplaySourceRenders (client)

    @Override
    protected String getTranslationKey() {
        return "altitude_sensor.data";
    }

    @Override
    public boolean allowsLabeling(final DisplayLinkContext context) {
        return true;
    }
}
