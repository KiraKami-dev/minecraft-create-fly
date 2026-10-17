package dev.eriksonn.aeronautics.index;

import com.zurrtum.create.api.behaviour.display.DisplaySource;
import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.content.display_sources.GasDisplaySource;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

public class AeroDisplaySources {

    public static final GasDisplaySource GAS_DISPLAY = simple("gas_display", GasDisplaySource::new);

    private static <T extends DisplaySource> T simple(final String name, final Supplier<T> supplier) {
        return Aeronautics.getRegistrate().displaySource(name, supplier);
    }

    /**
     * Attaches a display source to blocks (Registrate's {@code DisplaySource.displaySource(...)} transform).
     */
    public static void assign(final DisplaySource source, final Block... blocks) {
        for (final Block block : blocks) {
            DisplaySource.BY_BLOCK.add(block, source);
        }
    }

    public static void register() {
    }
}
