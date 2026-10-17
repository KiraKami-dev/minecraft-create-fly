package dev.simulated_team.simulated.data;


import dev.simulated_team.simulated.Simulated;
import com.zurrtum.create.client.catnip.lang.Lang;
import com.zurrtum.create.client.catnip.lang.LangBuilder;
import com.zurrtum.create.client.catnip.lang.LangNumberFormat;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Lang helpers. Client-only: Create Fly's catnip {@link LangBuilder} lives in its client package, so server-side code
 * has to build {@link Component}s directly. (The datagen lang provider of upstream is gone, the lang JSON is shipped.)
 */
public class SimLang {

    public static LangBuilder builder() {
        return Lang.builder(Simulated.MOD_ID);
    }

    public static LangBuilder text(final String text) {
        return builder().text(text);
    }

    public static LangBuilder translate(final String key, final Object... args) {
        return builder().translate(key, args);
    }

    public static LangBuilder number(final double number) {
        return builder().text(LangNumberFormat.format(number));
    }

    public static LangBuilder space() {
        return builder().space();
    }

    public static void emptyLine(final List<Component> tooltip) {
        builder().text("").forGoggles(tooltip);
    }

    public static LangBuilder blockName(final BlockState blockState) {
        return builder().add(blockState.getBlock().getName());
    }

    public static LangBuilder kilopixelGram(final double value) {
        return kilopixelGram(value, "%.2f");
    }

    public static LangBuilder kilopixelGram(final double value, final String format) {
        return getPrefixedUnit("pg", value, format,1);
    }

    public static LangBuilder pixelNewton(final double value) {
        return pixelNewton(value, "%.2f");
    }

    public static LangBuilder pixelNewton(final double value, final String format) {
        return getPrefixedUnit("pn", value, format,0);
    }

    public static LangBuilder getPrefixedUnit(String unit, double value, final String format, final int offset) {
        final String[] prefixes = {"k", "m", "g"};
        int index = offset-1;
        while (value >= 1000 && index < prefixes.length - 1) {
            value /= 1000;
            index++;
        }
        if(index >= 0)
            unit = prefixes[index] + unit;

        return translate("unit." + unit, format.formatted(value));
    }

    public static List<Component> translatedOptions(final String prefix, final String... keys) {
        final List<Component> result = new ArrayList<>(keys.length);
        for (final String key : keys)
            result.add(translate((prefix != null ? prefix + "." : "") + key).component());
        return result;
    }

}
