package dev.simulated_team.simulated.content.display_sources;

import com.zurrtum.create.api.behaviour.display.DisplaySource;
import com.zurrtum.create.client.api.behaviour.display.DisplaySourceRender;
import com.zurrtum.create.client.content.redstone.displayLink.source.ItemThroughputDisplaySourceRender;
import com.zurrtum.create.client.content.redstone.displayLink.source.SingleLineDisplaySourceRender;
import com.zurrtum.create.client.foundation.gui.ModularGuiLineBuilder;
import com.zurrtum.create.content.redstone.displayLink.DisplayLinkContext;
import dev.simulated_team.simulated.index.SimDisplaySources;

/**
 * Client-only. Create Fly moved {@code DisplaySource#initConfigurationWidgets} into client-side
 * {@link DisplaySourceRender}s attached to the (common) display source instances.
 */
public class SimDisplaySourceRenders {

    public static void registerClient() {
        attach(SimDisplaySources.AUGER_DISPLAY, new ItemThroughputDisplaySourceRender());
        attach(SimDisplaySources.VELO_DISPLAY, new NumericRender());
        attach(SimDisplaySources.GIMBAL_DISPLAY, new NumericRender());
        attach(SimDisplaySources.PORTABLE_ENGINE_DISPLAY, new NumericRender());
        attach(SimDisplaySources.OPTICAL_SENSOR_DISPLAY, new SelectionRender(85, "OpticalSensorSelection",
                "display_source.optical_sensor", "detected_block", "block_distance"));
        attach(SimDisplaySources.LASER_SENSOR_DISPLAY, new SelectionRender(88, "LaserSensorSelection",
                "display_source.laser_sensor", "laser_distance"));
        attach(SimDisplaySources.ALTITUDE_SENSOR_DISPLAY, new SelectionRender(95, "AltitudeSensorSelection",
                "display_source.altitude_sensor", "air_pressure", "height"));
        attach(SimDisplaySources.NAV_TABLE_DISPLAY, new SelectionRender(95, "NavTableSelection",
                "display_source.navigation_table", "distance", "eta_real"));
        attach(SimDisplaySources.DOCKING_CONNECTOR_DISPLAY, new SingleLineDisplaySourceRender());
        attach(SimDisplaySources.LINKED_TYPEWRITER__DISPLAY, new SingleLineDisplaySourceRender());
    }

    private static void attach(final DisplaySource source, final DisplaySourceRender render) {
        source.attachRender = render;
    }

    /**
     * Label box on the first line, selection of {@link AbstractNumericDisplaysource#getOptions()} on the second
     */
    public static class NumericRender extends SingleLineDisplaySourceRender {
        @Override
        public void initConfigurationWidgets(final DisplaySource source, final DisplayLinkContext context, final ModularGuiLineBuilder builder, final boolean isFirstLine) {
            super.initConfigurationWidgets(source, context, builder, isFirstLine);
            if (isFirstLine) {
                return;
            }

            final AbstractNumericDisplaysource numeric = (AbstractNumericDisplaysource) source;
            builder.addSelectionScrollInput(0, numeric.getWidth(), ((selectionScrollInput, label) -> selectionScrollInput.forOptions(numeric.getOptions())), numeric.getSelectionKey());
        }
    }

    /**
     * Label box on the first line, a selection of translated options on the second
     */
    public static class SelectionRender extends SingleLineDisplaySourceRender {
        private final int width;
        private final String selectionKey;
        private final String optionPrefix;
        private final String[] options;

        public SelectionRender(final int width, final String selectionKey, final String optionPrefix, final String... options) {
            this.width = width;
            this.selectionKey = selectionKey;
            this.optionPrefix = optionPrefix;
            this.options = options;
        }

        @Override
        public void initConfigurationWidgets(final DisplaySource source, final DisplayLinkContext context, final ModularGuiLineBuilder builder, final boolean isFirstLine) {
            super.initConfigurationWidgets(source, context, builder, isFirstLine);
            if (isFirstLine) {
                return;
            }

            builder.addSelectionScrollInput(0, this.width, (selectionScrollInput, label) -> {
                selectionScrollInput.forOptions(DisplaySourceLang.translatedOptions(this.optionPrefix, this.options));
            }, this.selectionKey);
        }
    }
}
