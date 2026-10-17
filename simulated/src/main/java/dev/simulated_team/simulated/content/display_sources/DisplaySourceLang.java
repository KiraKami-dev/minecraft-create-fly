package dev.simulated_team.simulated.content.display_sources;

import dev.simulated_team.simulated.Simulated;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Server-safe replacement for the {@code SimLang} calls display sources used upstream.
 * Create Fly's catnip {@code Lang}/{@code LangBuilder} are client-only, while display sources run on the server.
 * Produces the same component structure as {@code LangBuilder} (first part as root, the rest appended).
 */
final class DisplaySourceLang {
    private static final NumberFormat FORMAT = NumberFormat.getNumberInstance(Locale.ROOT);

    static {
        FORMAT.setMaximumFractionDigits(2);
        FORMAT.setMinimumFractionDigits(0);
        FORMAT.setGroupingUsed(true);
    }

    private DisplaySourceLang() {
    }

    /**
     * Equivalent of {@code SimLang.number(value).component()}
     */
    static MutableComponent number(double value) {
        if (Mth.equal(value, 0)) {
            value = 0;
        }
        return Component.literal(FORMAT.format(value).replace(" ", " "));
    }

    /**
     * Equivalent of {@code SimLang.translate(key, args).component()}
     */
    static MutableComponent translate(final String key, final Object... args) {
        return Component.translatable(Simulated.MOD_ID + "." + key, args);
    }

    /**
     * Equivalent of {@code SimLang.translatedOptions(prefix, keys)}
     */
    static List<Component> translatedOptions(final String prefix, final String... keys) {
        final List<Component> result = new ArrayList<>(keys.length);
        for (final String key : keys) {
            result.add(translate((prefix != null ? prefix + "." : "") + key));
        }
        return result;
    }
}
