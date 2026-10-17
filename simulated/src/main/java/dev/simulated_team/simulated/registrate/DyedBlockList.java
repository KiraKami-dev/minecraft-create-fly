package dev.simulated_team.simulated.registrate;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/**
 * One block per {@link DyeColor}, indexed by color. Replaces Create's (Registrate based) {@code DyedBlockList}: entries
 * are the registered blocks themselves.
 */
public class DyedBlockList<T extends Block> implements Iterable<T> {
    private static final int COLOR_AMOUNT = DyeColor.values().length;

    private final Block[] values = new Block[COLOR_AMOUNT];

    public DyedBlockList(final Function<DyeColor, ? extends T> filler) {
        for (final DyeColor color : DyeColor.values()) {
            this.values[color.ordinal()] = filler.apply(color);
        }
    }

    @SuppressWarnings("unchecked")
    public T get(final DyeColor color) {
        return (T) this.values[color.ordinal()];
    }

    public boolean contains(final Block block) {
        for (final Block value : this.values) {
            if (value == block) {
                return true;
            }
        }
        return false;
    }

    public Block[] toArray() {
        return Arrays.copyOf(this.values, this.values.length);
    }

    @SuppressWarnings("unchecked")
    public List<T> asList() {
        return (List<T>) List.of(this.values);
    }

    @Override
    public Iterator<T> iterator() {
        return this.asList().iterator();
    }
}
