package dev.simulated_team.simulated.registrate.simulated_tab;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * A class for easy invisibility marking and custom NBT in creative tabs
 *
 * @author Cyvack
 */
public class CreativeTabItemTransforms {

    private static final Map<Item, VisibilityType> ITEM_VISIBILITY = new ConcurrentHashMap<>();
    private static final Map<Item, Function<Item, ItemStack>> STACK_TRANSFORM = new ConcurrentHashMap<>();

    public static ItemStack applyTransform(final ItemStack item) {
        final Function<Item, ItemStack> transform = STACK_TRANSFORM.get(item.getItem());
        if(transform == null) {
            return item;
        }
        return transform.apply(item.getItem());
    }

    public enum VisibilityType {
        INVISIBLE, SEARCH_ONLY;

        public boolean has(final Item item) {
            return this == ITEM_VISIBILITY.get(item);
        }

        /**
         * Marks the item (or the item of a block) with this visibility. Call after the item is registered.
         */
        public void apply(final ItemLike item) {
            ITEM_VISIBILITY.put(item.asItem(), this);
        }

        public void applyIf(final ItemLike item, final boolean condition) {
            if (condition) {
                this.apply(item);
            }
        }
    }

    /**
     * Replaces the stack shown in the creative tab for the given item. Call after the item is registered.
     */
    public static void transformItem(final ItemLike item, final Function<Item, ItemStack> func) {
        STACK_TRANSFORM.put(item.asItem(), func);
    }
}
