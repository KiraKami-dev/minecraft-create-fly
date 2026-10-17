package dev.eriksonn.aeronautics.content.display_sources;

import com.zurrtum.create.content.redstone.displayLink.DisplayLinkContext;
import com.zurrtum.create.content.redstone.displayLink.source.NumericSingleLineDisplaySource;
import com.zurrtum.create.content.redstone.displayLink.target.DisplayTargetStats;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.ServerBalloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.BlockEntityLiftingGasProvider;
import joptsimple.internal.Strings;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * The configuration widgets (upstream {@code initConfigurationWidgets}) are client-side, see
 * {@link GasDisplaySourceRender}.
 */
public class GasDisplaySource extends NumericSingleLineDisplaySource {

    /**
     * Server-safe stand-in for catnip's {@code LangNumberFormat} (client-only in Create Fly): same settings, root locale
     */
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.ROOT);

    static {
        NUMBER_FORMAT.setMaximumFractionDigits(2);
        NUMBER_FORMAT.setMinimumFractionDigits(0);
        NUMBER_FORMAT.setGroupingUsed(true);
    }

    @Override
    protected MutableComponent provideLine(final DisplayLinkContext displayLinkContext, final DisplayTargetStats displayTargetStats) {
        if (!(displayLinkContext.getSourceBlockEntity() instanceof final BlockEntityLiftingGasProvider provider)) {
            return ZERO.copy();
        }

        if (!(provider.getBalloon() instanceof final ServerBalloon info)) {
            return noBalloon();
        }

        switch (displayLinkContext.sourceConfig().getIntOr("GasDataSelection", 0)) {
            case 0 -> { // volume
                final int totalBar = 15;
                final int capacity = info.getCapacity();
                final int targetBar = (int) Math.ceil(totalBar * info.getTotalTargetVolume() / capacity);
                final int volume = Mth.clamp((int) Math.ceil(totalBar * (info.getTotalFilledVolume() + info.getTotalVolumeChange()) / capacity), 0, totalBar);

                return barComponent(volume, targetBar, totalBar);
            }
            case (1) -> { // total lift
                // upstream AeroLang.text(LangNumberFormat.format(lift)).component()
                return Component.literal(formatNumber(info.getTotalLift()));
            }
        }

        return ZERO.copy();
    }

    private static String formatNumber(double value) {
        if (Mth.equal(value, 0)) {
            value = 0;
        }
        return NUMBER_FORMAT.format(value).replace(" ", " ");
    }

    private static MutableComponent noBalloon() {
        return Component.literal("No Balloon above");
    }

    static MutableComponent barComponent(final int amount, final int target, final int total) {
        final int lower = Math.min(amount, target - 1);
        final int upper = Math.max(amount - target, 0);
        final char filledChar = '█';
        final char halfFillChar = '▒';
        final char emptyChar = '░';
        return Component.empty()
                .append(bars(Math.max(0, lower), ChatFormatting.DARK_AQUA, filledChar))
                .append(bars(Math.max(0, target - lower - 1), ChatFormatting.DARK_GRAY, halfFillChar))/*
                .append(bars(upper, ChatFormatting.DARK_AQUA, filledChar))
                .append(bars(Math.max(0, total - target - upper), ChatFormatting.DARK_GRAY, filledChar))*/;

    }

    private static MutableComponent bars(final int count, final ChatFormatting format, final char ch) {
        return Component.literal(Strings.repeat(ch, count))
                .withStyle(format);
    }

    @Override
    public boolean allowsLabeling(final DisplayLinkContext displayLinkContext) {
        return true;
    }

    @Override
    protected String getTranslationKey() {
        return "lifting_gas.data";
    }
}
