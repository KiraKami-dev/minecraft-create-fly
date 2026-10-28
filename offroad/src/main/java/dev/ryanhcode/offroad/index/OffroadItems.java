package dev.ryanhcode.offroad.index;

import com.zurrtum.create.content.contraptions.actors.roller.RollerBlockItem;
import dev.ryanhcode.offroad.Offroad;
import dev.ryanhcode.offroad.content.components.TireLike;
import dev.ryanhcode.offroad.content.items.tire.TireItem;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

/**
 * Offroad's items, block items included (Create Fly style), in upstream's Registrate order: block items in
 * {@link OffroadBlocks} order, then the tires.
 */
public class OffroadItems {
    private static final SimulatedRegistrate REGISTRATE = Offroad.getRegistrate();

    // Block items

    public static final BlockItem BOREHEAD_BEARING = REGISTRATE.blockItem(OffroadBlocks.BOREHEAD_BEARING_BLOCK);
    public static final BlockItem ROCK_CUTTER = REGISTRATE.blockItem(OffroadBlocks.ROCK_CUTTER_BLOCK, BlockItem::new,
            new Item.Properties().component(OffroadDataComponents.TIRE, TireLike.ROCKCUTTING_WHEEL));
    public static final RollerBlockItem WHEEL_MOUNT = REGISTRATE.blockItem(OffroadBlocks.WHEEL_MOUNT, RollerBlockItem::new);

    // Items

    public static final TireItem SMALL_TIRE = tire("small_tire", TireLike.SMALL_TIRE);
    public static final TireItem TIRE = tire("tire", TireLike.TIRE);
    public static final TireItem LARGE_TIRE = tire("large_tire", TireLike.LARGE_TIRE);
    public static final TireItem MONSTROUS_TIRE = tire("monstrous_tire", TireLike.MONSTROUS_TIRE);

    private static TireItem tire(final String name, final TireLike tire) {
        return REGISTRATE.item(name, TireItem::new, new Item.Properties().component(OffroadDataComponents.TIRE, tire));
    }

    public static void register() {
    }
}
