package dev.ryanhcode.offroad.index;

import com.zurrtum.create.api.behaviour.movement.MovementBehaviour;
import dev.ryanhcode.offroad.Offroad;
import dev.ryanhcode.offroad.config.server.OffroadStress;
import dev.ryanhcode.offroad.content.blocks.borehead_bearing.BoreheadBearingBlock;
import dev.ryanhcode.offroad.content.blocks.rock_cutting_wheel.RockCuttingWheelActor;
import dev.ryanhcode.offroad.content.blocks.rock_cutting_wheel.RockCuttingWheelBlock;
import dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountBlock;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.function.Function;

/**
 * Offroad's blocks (Create Fly style: the fields are the blocks). Block items live in {@link OffroadItems}, block
 * entity types in {@link OffroadBlockEntityTypes}, render layers in {@code OffroadClient}. Blockstates, models, loot
 * tables, tags and recipes are shipped as JSON (upstream generated them from here).
 */
@SuppressWarnings("deprecation")
public class OffroadBlocks {
    private static final SimulatedRegistrate REGISTRATE = Offroad.getRegistrate();

    public static final BoreheadBearingBlock BOREHEAD_BEARING_BLOCK = register(
            "borehead_bearing",
            BoreheadBearingBlock::new,
            softMetal().mapColor(MapColor.COLOR_GRAY)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .isRedstoneConductor((state, level, pos) -> false)
    );

    public static final RockCuttingWheelBlock ROCK_CUTTER_BLOCK = register(
            "rockcutting_wheel",
            RockCuttingWheelBlock::new,
            softMetal().mapColor(MapColor.COLOR_GRAY)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
    );

    public static final WheelMountBlock WHEEL_MOUNT = register(
            "wheel_mount",
            WheelMountBlock::new,
            stone().mapColor(MapColor.COLOR_GRAY)
                    .noOcclusion()
                    .isRedstoneConductor((state, level, pos) -> false)
    );

    /**
     * The rock cutting wheel's contraption actor; its renderer is attached on the client ({@code OffroadClient}).
     */
    public static final RockCuttingWheelActor ROCK_CUTTER_ACTOR = new RockCuttingWheelActor();

    // Create's SharedProperties

    private static BlockBehaviour.Properties stone() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.ANDESITE);
    }

    private static BlockBehaviour.Properties softMetal() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.GOLD_BLOCK);
    }

    private static <T extends Block> T register(final String name, final Function<BlockBehaviour.Properties, T> factory, final BlockBehaviour.Properties properties) {
        return REGISTRATE.block(name, factory, properties);
    }

    /**
     * Everything Registrate did per block besides registering it: stress and movement behaviours.
     * Must run before the server config is created (stress defaults).
     */
    public static void register() {
        OffroadStress.setImpact(BOREHEAD_BEARING_BLOCK, 8.0);
        OffroadStress.setImpact(WHEEL_MOUNT, 16.0);

        MovementBehaviour.REGISTRY.register(ROCK_CUTTER_BLOCK, ROCK_CUTTER_ACTOR);
    }
}
