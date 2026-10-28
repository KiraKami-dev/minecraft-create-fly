package dev.ryanhcode.offroad.data;

import dev.ryanhcode.offroad.Offroad;
import com.zurrtum.create.client.catnip.lang.Lang;
import com.zurrtum.create.client.catnip.lang.LangBuilder;
import com.zurrtum.create.client.catnip.lang.LangNumberFormat;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class OffroadLang {

    public static LangBuilder builder() {
        return Lang.builder(Offroad.MOD_ID);
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

    public static List<Component> translatedOptions(final String prefix, final String... keys) {
        final List<Component> result = new ArrayList<>(keys.length);
        for (final String key : keys)
            result.add(translate((prefix != null ? prefix + "." : "") + key).component());
        return result;
    }

    public static LangBuilder kilopixelGram(final double value) {
        return kilopixelGram(value, "%.2f");
    }

    public static LangBuilder kilopixelGram(final double value, final String format) {
        return getPrefixedUnit("pg", value, format);
    }

    public static LangBuilder kilopixelNewton(final double value) {
        return kilopixelNewton(value, "%.2f");
    }

    public static LangBuilder kilopixelNewton(final double value, final String format) {
        return getPrefixedUnit("pn", value, format);
    }

    private static LangBuilder getPrefixedUnit(final String unit, double value, final String format) {
        final String[] prefixes = {"k", "m", "g"};
        int index = 0;
        while (value >= 1000 && index < prefixes.length - 1) {
            value /= 1000;
            index++;
        }

        return translate("unit." + prefixes[index] + unit, format.formatted(value));
    }
}
