package dev.eriksonn.aeronautics.index;

import com.zurrtum.create.AllBlocks;
import com.zurrtum.create.content.decoration.encasing.EncasingRegistry;
import com.zurrtum.create.infrastructure.fluids.FlowableFluid;
import com.zurrtum.create.infrastructure.fluids.FluidBlock;
import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.config.server.AeroStress;
import dev.eriksonn.aeronautics.content.blocks.hot_air.envelope.EnvelopeBlock;
import dev.eriksonn.aeronautics.content.blocks.hot_air.envelope.EnvelopeEncasedShaftBlock;
import dev.eriksonn.aeronautics.content.blocks.hot_air.hot_air_burner.HotAirBurnerBlock;
import dev.eriksonn.aeronautics.content.blocks.hot_air.steam_vent.SteamVentBlock;
import dev.eriksonn.aeronautics.content.blocks.mounted_potato_cannon.MountedPotatoCannonBlock;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.gyroscopic_propeller_bearing.GyroscopicPropellerBearingBlock;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.propeller_bearing.PropellerBearingBlock;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.andesite.AndesitePropellerBlock;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.smart_propeller.SmartPropellerBlock;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.wooden.WoodenPropellerBlock;
import dev.simulated_team.simulated.index.sounds.SimLazySoundType;
import dev.simulated_team.simulated.registrate.DyedBlockList;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Function;

/**
 * Aeronautics' blocks (Create Fly style: the fields are the blocks). Block items live in {@link AeroItems}, block
 * entity types in {@link AeroBlockEntityTypes}, render layers and connected textures in {@code AeronauticsClient}.
 * Blockstates, models, loot tables, tags and recipes are shipped as JSON (upstream generated them from here).
 */
@SuppressWarnings("deprecation")
public class AeroBlocks {
    private static final SimulatedRegistrate REGISTRATE = Aeronautics.getRegistrate();

    private static boolean neverSpawn(final BlockState state, final BlockGetter blockGetter, final BlockPos pos, final EntityType<?> entity) {
        return false;
    }

    public static final EnvelopeBlock WHITE_ENVELOPE_BLOCK = envelope(DyeColor.WHITE);

    public static final DyedBlockList<EnvelopeBlock> DYED_ENVELOPE_BLOCKS = new DyedBlockList<>(color -> {
        if (color == DyeColor.WHITE) {
            return WHITE_ENVELOPE_BLOCK;
        }
        return envelope(color);
    });

    public static final DyedBlockList<EnvelopeEncasedShaftBlock> ENVELOPE_ENCASED_SHAFTS = new DyedBlockList<>(color -> register(
            color.getSerializedName() + "_envelope_encased_shaft",
            p -> EnvelopeEncasedShaftBlock.withCanvas(p, color),
            wooden().noOcclusion().sound(envelopeSound()).mapColor(color)
    ));

    public static final HotAirBurnerBlock HOT_AIR_BURNER = register(
            "adjustable_burner",
            HotAirBurnerBlock::new,
            stone().sound(SoundType.NETHERITE_BLOCK).noOcclusion().lightLevel(HotAirBurnerBlock::getLightPower)
    );

    public static final SteamVentBlock STEAM_VENT = register(
            "steam_vent",
            SteamVentBlock::new,
            stone().sound(SoundType.NETHERITE_BLOCK).noOcclusion()
    );

    public static final PropellerBearingBlock PROPELLER_BEARING = register(
            "propeller_bearing",
            PropellerBearingBlock::new,
            stone().sound(SoundType.COPPER).noOcclusion()
    );

    public static final GyroscopicPropellerBearingBlock GYROSCOPIC_PROPELLER_BEARING = register(
            "gyroscopic_propeller_bearing",
            GyroscopicPropellerBearingBlock::new,
            stone().sound(SoundType.COPPER).noOcclusion()
    );

    public static final SmartPropellerBlock SMART_PROPELLER = register(
            "smart_propeller",
            SmartPropellerBlock::new,
            softMetal()
    );

    public static final AndesitePropellerBlock ANDESITE_PROPELLER = register(
            "andesite_propeller",
            AndesitePropellerBlock::new,
            wooden().sound(SoundType.WOOD)
    );

    public static final WoodenPropellerBlock WOODEN_PROPELLER = register(
            "wooden_propeller",
            WoodenPropellerBlock::new,
            wooden().sound(SoundType.WOOD)
    );

    public static final MountedPotatoCannonBlock MOUNTED_POTATO_CANNON = register(
            "mounted_potato_cannon",
            MountedPotatoCannonBlock::new,
            stone().noOcclusion()
    );

    public static final Block LEVITITE = register(
            "levitite",
            Block::new,
            levitite()
    );

    public static final Block PEARLESCENT_LEVITITE = register(
            "pearlescent_levitite",
            Block::new,
            levitite()
    );

    /**
     * The Levitite Blend fluid block (upstream: Registrate's {@code standardFluid} default block, a copy of water).
     */
    public static final FluidBlock LEVITITE_BLEND = fluid(AeroFluids.LEVITITE_BLEND, BlockBehaviour.Properties.ofFullCopy(Blocks.WATER));

    private static EnvelopeBlock envelope(final DyeColor color) {
        return register(
                color.getSerializedName() + "_envelope",
                p -> new EnvelopeBlock(p, color),
                wooden().isValidSpawn(AeroBlocks::neverSpawn).sound(envelopeSound()).mapColor(color)
        );
    }

    private static SoundType envelopeSound() {
        return new SimLazySoundType(1.0f, 1.0f,
                AeroSoundEvents.ENVELOPE_BREAK::event,
                () -> SoundEvents.WOOL_STEP,
                AeroSoundEvents.ENVELOPE_PLACE::event,
                AeroSoundEvents.ENVELOPE_HIT::event,
                () -> SoundEvents.WOOL_FALL);
    }

    private static BlockBehaviour.Properties levitite() {
        return BlockBehaviour.Properties.of()
                .lightLevel($ -> 10)
                .noLootTable()
                .strength(7, 20)
                .sound(new SimLazySoundType(1.0f, 1.0f,
                        AeroSoundEvents.LEVITITE_BREAK::event,
                        () -> SoundEvents.AMETHYST_BLOCK_STEP,
                        AeroSoundEvents.LEVITITE_PLACE::event,
                        () -> SoundEvents.AMETHYST_BLOCK_HIT,
                        () -> SoundEvents.AMETHYST_BLOCK_FALL));
    }

    // Create's SharedProperties

    private static BlockBehaviour.Properties wooden() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_SPRUCE_WOOD);
    }

    private static BlockBehaviour.Properties stone() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.ANDESITE);
    }

    private static BlockBehaviour.Properties softMetal() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.GOLD_BLOCK);
    }

    private static <T extends Block> T register(final String name, final Function<BlockBehaviour.Properties, T> factory, final BlockBehaviour.Properties properties) {
        return REGISTRATE.block(name, factory, properties);
    }

    private static FluidBlock fluid(final FlowableFluid fluid, final BlockBehaviour.Properties properties) {
        final FluidBlock block = register(BuiltInRegistries.FLUID.getKey(fluid).getPath(), p -> new FluidBlock(fluid, p), properties);
        fluid.getEntry().block = block;
        return block;
    }

    /**
     * Everything Registrate did per block besides registering it: stress, display sources, encasing, flammability.
     * Must run before the server config is created (stress defaults).
     */
    public static void register() {
        AeroStress.setImpact(PROPELLER_BEARING, 2.0);
        AeroStress.setImpact(GYROSCOPIC_PROPELLER_BEARING, 2.0);
        AeroStress.setImpact(SMART_PROPELLER, 4.0);
        AeroStress.setImpact(ANDESITE_PROPELLER, 4.0);
        AeroStress.setImpact(WOODEN_PROPELLER, 4.0);
        AeroStress.setImpact(MOUNTED_POTATO_CANNON, 2.0);

        AeroDisplaySources.assign(AeroDisplaySources.GAS_DISPLAY, HOT_AIR_BURNER, STEAM_VENT);

        for (final EnvelopeEncasedShaftBlock shaft : ENVELOPE_ENCASED_SHAFTS) {
            EncasingRegistry.addVariant(AllBlocks.SHAFT, shaft);
        }

        // only the plain envelopes were flammable upstream (encouragement 30, flammability 60)
        final FireBlock fire = (FireBlock) Blocks.FIRE;
        for (final EnvelopeBlock envelope : DYED_ENVELOPE_BLOCKS) {
            fire.setFlammable(envelope, 30, 60);
        }
    }

}
