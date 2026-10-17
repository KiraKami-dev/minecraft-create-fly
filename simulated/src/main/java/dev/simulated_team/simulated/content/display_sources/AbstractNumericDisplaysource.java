package dev.simulated_team.simulated.content.display_sources;

import com.zurrtum.create.content.redstone.displayLink.DisplayLinkContext;
import com.zurrtum.create.content.redstone.displayLink.source.NumericSingleLineDisplaySource;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * The configuration widgets (selection scroll input built from {@link #getOptions()}, {@link #getWidth()} and
 * {@link #getSelectionKey()}) are client-side, see {@link SimDisplaySourceRenders}.
 */
public abstract class AbstractNumericDisplaysource extends NumericSingleLineDisplaySource {

    public int getWidth() {
        return 100;
    }

    abstract List<Component> getOptions();
    abstract String getKey();
    abstract String getSelectionKey();

    @Override
    protected String getTranslationKey() {
        return this.getKey();
    }

    @Override
    public boolean allowsLabeling(final DisplayLinkContext displayLinkContext) {
        return true;
    }
}
