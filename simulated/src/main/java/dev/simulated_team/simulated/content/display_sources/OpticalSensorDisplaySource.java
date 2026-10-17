package dev.simulated_team.simulated.content.display_sources;

import com.zurrtum.create.content.redstone.displayLink.DisplayLinkContext;
import com.zurrtum.create.content.redstone.displayLink.source.NumericSingleLineDisplaySource;
import com.zurrtum.create.content.redstone.displayLink.target.DisplayTargetStats;
import dev.simulated_team.simulated.content.blocks.lasers.optical_sensor.OpticalSensorBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class OpticalSensorDisplaySource extends NumericSingleLineDisplaySource {

    @Override
    protected MutableComponent provideLine(final DisplayLinkContext context, final DisplayTargetStats stats) {
        if (!(context.getSourceBlockEntity() instanceof final OpticalSensorBlockEntity be)) {
            return ZERO.copy();
        }

        switch (context.sourceConfig().getIntOr("OpticalSensorSelection", 0)) {
            case 0 -> {
                return be.hasHit() ? be.getHitBlock().getName() : Component.literal("No Block Detected");
            }
            case 1 -> {
                if (!be.hasHit()) {
                    return Component.literal("No Block Detected");
                }
                final float rayDistance = be.getRayDistance();
                return DisplaySourceLang.number(rayDistance)
                        .append(" ")
                        .append("block" + (rayDistance != 1 ? "s" : ""));
            }
        }

        return ZERO.copy();
    }

    // configuration widgets: SimDisplaySourceRenders (client)

    @Override
    protected String getTranslationKey() {
        return "optical_sensor.data";
    }

    @Override
    public boolean allowsLabeling(final DisplayLinkContext context) {
        return true;
    }
}
