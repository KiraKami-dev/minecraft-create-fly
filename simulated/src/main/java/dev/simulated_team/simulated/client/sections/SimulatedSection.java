package dev.simulated_team.simulated.client.sections;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.simulated_team.simulated.Simulated;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public record SimulatedSection(int priority, Title title, Identifier sprite, boolean animateOnHover) implements Comparable<SimulatedSection> {
    private static final Identifier DEFAULT_BANNER = Simulated.path("default_banner");

    public static final Codec<SimulatedSection> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ExtraCodecs.POSITIVE_INT.fieldOf("priority").orElse(0).forGetter(SimulatedSection::priority),
            Title.CODEC.fieldOf("title").forGetter(SimulatedSection::title),
            Identifier.CODEC.fieldOf("sprite").orElse(DEFAULT_BANNER).forGetter(SimulatedSection::sprite),
            Codec.BOOL.fieldOf("only_animate_on_hover").orElse(false).forGetter(SimulatedSection::animateOnHover)
    ).apply(instance, SimulatedSection::new));

    @Override
    public int compareTo(@NotNull final SimulatedSection other) {
        return (int) Math.signum(this.priority() - other.priority());
    }

    /**
     * Colours are ARGB ints (Veil's Colorc before the port; the shim has no colour API).
     */
    public record Title(Component text, int color, Optional<Integer> secondaryColor, int background) {
        /**
         * ARGB colour: a number, a "#AARRGGBB" / "0xAARRGGBB" / decimal string, or an [r, g, b(, a)] list (as Veil's
         * ARGB codec accepted).
         */
        public static final Codec<Integer> COLOR_CODEC = Codec.withAlternative(
                Codec.withAlternative(Codec.INT, Codec.STRING.comapFlatMap(Title::parseColor, i -> String.format("#%08x", i))),
                Codec.INT.listOf(3, 4).xmap(Title::fromComponents, i -> List.of((i >> 16) & 0xFF, (i >> 8) & 0xFF, i & 0xFF, (i >>> 24)))
        );
        public static final Codec<Title> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ComponentSerialization.CODEC.fieldOf("text").forGetter(Title::text),
            COLOR_CODEC.fieldOf("color").orElse(0xffffffff).forGetter(Title::color),
            COLOR_CODEC.optionalFieldOf("secondary_color").forGetter(Title::secondaryColor),
            COLOR_CODEC.fieldOf("background").orElse(0xaa000000).forGetter(Title::background)
        ).apply(instance, Title::new));

        private static DataResult<Integer> parseColor(final String string) {
            try {
                if (string.startsWith("#")) {
                    return DataResult.success(Integer.parseUnsignedInt(string.substring(1), 16));
                }
                if (string.startsWith("0x") || string.startsWith("0X")) {
                    return DataResult.success(Integer.parseUnsignedInt(string.substring(2), 16));
                }
                return DataResult.success(Integer.parseUnsignedInt(string));
            } catch (final NumberFormatException e) {
                return DataResult.error(() -> "Invalid color: " + string + ". " + e.getMessage());
            }
        }

        private static int fromComponents(final List<Integer> components) {
            int color = (components.get(0) & 0xFF) << 16 | (components.get(1) & 0xFF) << 8 | (components.get(2) & 0xFF);
            return color | (components.size() == 4 ? (components.get(3) & 0xFF) << 24 : 0xFF000000);
        }
    }
}
