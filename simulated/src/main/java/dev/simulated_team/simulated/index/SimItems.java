package dev.simulated_team.simulated.index;

import com.zurrtum.create.content.processing.sequenced.SequencedAssemblyItem;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterItem;
import dev.simulated_team.simulated.content.entities.diagram.DiagramItem;
import dev.simulated_team.simulated.content.entities.honey_glue.HoneyGlueItem;
import dev.simulated_team.simulated.content.items.plunger_launcher.PlungerLauncherItem;
import dev.simulated_team.simulated.content.items.rope.RopeItem.RopeItem;
import dev.simulated_team.simulated.content.items.spring.SpringItem;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffItem;
import dev.simulated_team.simulated.registrate.DyedBlockList;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import dev.simulated_team.simulated.registrate.simulated_tab.CreativeTabItemTransforms.VisibilityType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Simulated's items, block items included (Create Fly style). Registration order is the order of the Simulated
 * creative tab: block items in {@link SimBlocks} order first, then the plain items (as Registrate did upstream).
 */
public class SimItems {
    public static final SimulatedRegistrate REGISTRATE = Simulated.getRegistrate();

    // Block items

    public static final BlockItem PHYSICS_ASSEMBLER = REGISTRATE.blockItem(SimBlocks.PHYSICS_ASSEMBLER);
    public static final BlockItem SWIVEL_BEARING = REGISTRATE.blockItem(SimBlocks.SWIVEL_BEARING);
    public static final BlockItem ROPE_WINCH = REGISTRATE.blockItem(SimBlocks.ROPE_WINCH);
    public static final BlockItem ROPE_CONNECTOR = REGISTRATE.blockItem(SimBlocks.ROPE_CONNECTOR);
    public static final BlockItem IRON_HANDLE = REGISTRATE.blockItem(SimBlocks.IRON_HANDLE);
    public static final BlockItem COPPER_HANDLE = searchOnly(REGISTRATE.blockItem(SimBlocks.COPPER_HANDLE));
    public static final Map<DyeColor, BlockItem> DYED_HANDLES = dyedBlockItems(SimBlocks.DYED_HANDLES, null);
    public static final BlockItem DIRECTIONAL_GEARSHIFT = REGISTRATE.blockItem(SimBlocks.DIRECTIONAL_GEARSHIFT);
    public static final BlockItem TORSION_SPRING = REGISTRATE.blockItem(SimBlocks.TORSION_SPRING);
    public static final BlockItem AUGER_SHAFT = REGISTRATE.blockItem(SimBlocks.AUGER_SHAFT);
    public static final BlockItem AUGER_COG = REGISTRATE.blockItem(SimBlocks.AUGER_COG);
    public static final Map<DyeColor, BlockItem> PORTABLE_ENGINES = dyedBlockItems(SimBlocks.PORTABLE_ENGINES, DyeColor.RED);
    public static final BlockItem RED_PORTABLE_ENGINE = PORTABLE_ENGINES.get(DyeColor.RED);
    public static final BlockItem LASER_POINTER = REGISTRATE.blockItem(SimBlocks.LASER_POINTER);
    public static final BlockItem LASER_SENSOR = REGISTRATE.blockItem(SimBlocks.LASER_SENSOR);
    public static final BlockItem VELOCITY_SENSOR = REGISTRATE.blockItem(SimBlocks.VELOCITY_SENSOR);
    public static final BlockItem ALTITUDE_SENSOR = REGISTRATE.blockItem(SimBlocks.ALTITUDE_SENSOR);
    public static final BlockItem GIMBAL_SENSOR = REGISTRATE.blockItem(SimBlocks.GIMBAL_SENSOR);
    public static final BlockItem NAVIGATION_TABLE = REGISTRATE.blockItem(SimBlocks.NAVIGATION_TABLE);
    public static final BlockItem OPTICAL_SENSOR = REGISTRATE.blockItem(SimBlocks.OPTICAL_SENSOR);
    public static final BlockItem DOCKING_CONNECTOR = REGISTRATE.blockItem(SimBlocks.DOCKING_CONNECTOR);
    public static final BlockItem ANALOG_TRANSMISSION = REGISTRATE.blockItem(SimBlocks.ANALOG_TRANSMISSION);
    public static final BlockItem STEERING_WHEEL = REGISTRATE.blockItem(SimBlocks.STEERING_WHEEL);
    public static final BlockItem THROTTLE_LEVER = REGISTRATE.blockItem(SimBlocks.THROTTLE_LEVER);
    public static final LinkedTypewriterItem LINKED_TYPEWRITER = REGISTRATE.blockItem(SimBlocks.LINKED_TYPEWRITER, LinkedTypewriterItem::new);
    public static final BlockItem DIRECTIONAL_LINKED_RECEIVER = REGISTRATE.blockItem(SimBlocks.DIRECTIONAL_LINKED_RECEIVER);
    public static final BlockItem MODULATING_LINKED_RECEIVER = REGISTRATE.blockItem(SimBlocks.MODULATING_LINKED_RECEIVER);
    public static final BlockItem REDSTONE_ACCUMULATOR = REGISTRATE.blockItem(SimBlocks.REDSTONE_ACCUMULATOR);
    public static final BlockItem REDSTONE_INDUCTOR = REGISTRATE.blockItem(SimBlocks.REDSTONE_INDUCTOR);
    public static final BlockItem REDSTONE_MAGNET = REGISTRATE.blockItem(SimBlocks.REDSTONE_MAGNET);
    public static final BlockItem WHITE_SYMMETRIC_SAIL = REGISTRATE.blockItem(SimBlocks.WHITE_SYMMETRIC_SAIL);
    public static final Map<DyeColor, BlockItem> NAMEPLATES = dyedBlockItems(SimBlocks.NAMEPLATES, DyeColor.WHITE);

    // Items

    public static final DiagramItem CONTRAPTION_DIAGRAM = REGISTRATE.item("contraption_diagram", DiagramItem::new);

    public static final SpringItem SPRING = REGISTRATE.item(
            "spring",
            SpringItem::new,
            new Item.Properties().component(SimDataComponents.BOUNCINESS, 1f)
    );

    public static final RopeItem ROPE_COUPLING = REGISTRATE.item("rope_coupling", RopeItem::new);

    public static final Item GYRO_MECHANISM = ingredient("gyroscopic_mechanism");

    public static final SequencedAssemblyItem INCOMPLETE_GYRO_MECHANISM = invisible(REGISTRATE.item(
            "incomplete_gyroscopic_mechanism",
            SequencedAssemblyItem::new
    ));

    public static final Item ENGINE_ASSEMBLY = ingredient("engine_assembly");

    public static final SequencedAssemblyItem INCOMPLETE_ENGINE_ASSEMBLY = invisible(REGISTRATE.item(
            "incomplete_engine_assembly",
            SequencedAssemblyItem::new
    ));

    static {
        REGISTRATE.addExtraItem(Identifier.withDefaultNamespace("slime_ball"));
    }

    public static final HoneyGlueItem HONEY_GLUE = REGISTRATE.item(
            "honey_glue",
            HoneyGlueItem::new,
            new Item.Properties().stacksTo(1).durability(100)
    );

    public static final PhysicsStaffItem PHYSICS_STAFF = REGISTRATE.item(
            "creative_physics_staff",
            PhysicsStaffItem::new,
            new Item.Properties().rarity(Rarity.EPIC).stacksTo(1)
    );

    public static final PlungerLauncherItem PLUNGER_LAUNCHER = REGISTRATE.item(
            "plunger_launcher",
            PlungerLauncherItem::new,
            new Item.Properties().stacksTo(1).durability(200)
    );

    private static Item ingredient(final String name) {
        return REGISTRATE.item(name, Item::new);
    }

    private static <T extends Item> T invisible(final T item) {
        VisibilityType.INVISIBLE.apply(item);
        return item;
    }

    private static <T extends Item> T searchOnly(final T item) {
        VisibilityType.SEARCH_ONLY.apply(item);
        return item;
    }

    /**
     * Block items of a dyed block family; every color but {@code visibleColor} is only shown in the creative search tab.
     */
    private static <B extends Block> Map<DyeColor, BlockItem> dyedBlockItems(final DyedBlockList<B> blocks, @Nullable final DyeColor visibleColor) {
        final Map<DyeColor, BlockItem> items = new EnumMap<>(DyeColor.class);
        for (final DyeColor color : DyeColor.values()) {
            final BlockItem item = REGISTRATE.blockItem(blocks.get(color));
            if (color != visibleColor) {
                searchOnly(item);
            }
            items.put(color, item);
        }
        return Collections.unmodifiableMap(items);
    }

    public static void register() {
    }
}
