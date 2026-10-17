package dev.eriksonn.aeronautics.content.display_sources;

import com.zurrtum.create.api.behaviour.display.DisplaySource;
import com.zurrtum.create.client.content.redstone.displayLink.source.SingleLineDisplaySourceRender;
import com.zurrtum.create.client.foundation.gui.ModularGuiLineBuilder;
import com.zurrtum.create.content.redstone.displayLink.DisplayLinkContext;
import dev.eriksonn.aeronautics.data.AeroLang;
import dev.eriksonn.aeronautics.index.AeroDisplaySources;

/**
 * Client-only. Create Fly moved {@code DisplaySource#initConfigurationWidgets} into client-side display source renders
 * attached to the (common) display source instances: label box on the first line (as upstream's {@code super} call),
 * the volume / total lift selection on the second.
 */
public class GasDisplaySourceRender extends SingleLineDisplaySourceRender {

    public static void registerClient() {
        AeroDisplaySources.GAS_DISPLAY.attachRender = new GasDisplaySourceRender();
    }

    @Override
    public void initConfigurationWidgets(final DisplaySource source, final DisplayLinkContext context, final ModularGuiLineBuilder builder, final boolean isFirstLine) {
        super.initConfigurationWidgets(source, context, builder, isFirstLine);
        if (isFirstLine) {
            return;
        }

        builder.addSelectionScrollInput(0, 60, (selectionScrollInput, label) -> {
            selectionScrollInput.forOptions(AeroLang.translatedOptions("display_source.lifting_gas", "volume", "total_lift"));
        }, "GasDataSelection");
    }
}
