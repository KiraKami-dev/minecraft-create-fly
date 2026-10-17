package dev.simulated_team.simulated.index;

import com.zurrtum.create.api.behaviour.display.DisplayTarget;
import com.zurrtum.create.api.stress.BlockStressValues;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.config.server.blocks.SimStress;
import dev.simulated_team.simulated.content.blocks.altitude_sensor.AltitudeSensorBlock;
import dev.simulated_team.simulated.content.blocks.analog_transmission.AnalogTransmissionBlock;
import dev.simulated_team.simulated.content.blocks.auger_shaft.AugerCogBlock;
import dev.simulated_team.simulated.content.blocks.auger_shaft.AugerShaftBlock;
import dev.simulated_team.simulated.content.blocks.directional_gearshift.DirectionalGearshiftBlock;
import dev.simulated_team.simulated.content.blocks.docking_connector.DockingConnectorBlock;
import dev.simulated_team.simulated.content.blocks.docking_connector.PairedDockingConnectorBlock;
import dev.simulated_team.simulated.content.blocks.gimbal_sensor.GimbalSensorBlock;
import dev.simulated_team.simulated.content.blocks.handle.HandleBlock;
import dev.simulated_team.simulated.content.blocks.lasers.laser_pointer.LaserPointerBlock;
import dev.simulated_team.simulated.content.blocks.lasers.laser_sensor.LaserSensorBlock;
import dev.simulated_team.simulated.content.blocks.lasers.optical_sensor.OpticalSensorBlock;
import dev.simulated_team.simulated.content.blocks.merging_glue.MergingGlueBlock;
import dev.simulated_team.simulated.content.blocks.nameplate.NameplateBlock;
import dev.simulated_team.simulated.content.blocks.nameplate.NameplateBlockTarget;
import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlock;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerBlock;
import dev.simulated_team.simulated.content.blocks.portable_engine.PortableEngineBlock;
import dev.simulated_team.simulated.content.blocks.redstone.directional_receiver.DirectionalLinkedReceiverBlock;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterBlock;
import dev.simulated_team.simulated.content.blocks.redstone.modulating_receiver.ModulatingLinkedReceiverBlock;
import dev.simulated_team.simulated.content.blocks.redstone.redstone_accumulator.RedstoneAccumulatorBlock;
import dev.simulated_team.simulated.content.blocks.redstone.redstone_inductor.RedstoneInductorBlock;
import dev.simulated_team.simulated.content.blocks.redstone_magnet.RedstoneMagnetBlock;
import dev.simulated_team.simulated.content.blocks.rope.rope_connector.RopeConnectorBlock;
import dev.simulated_team.simulated.content.blocks.rope.rope_winch.RopeWinchBlock;
import dev.simulated_team.simulated.content.blocks.spring.SpringBlock;
import dev.simulated_team.simulated.content.blocks.steering_wheel.SteeringWheelBlock;
import dev.simulated_team.simulated.content.blocks.steering_wheel.SteeringWheelBlockEntity;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.SwivelBearingBlock;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.link_block.SwivelBearingPlateBlock;
import dev.simulated_team.simulated.content.blocks.symmetric_sail.SymmetricSailBlock;
import dev.simulated_team.simulated.content.blocks.throttle_lever.ThrottleLeverBlock;
import dev.simulated_team.simulated.content.blocks.torsion_spring.TorsionSpringBlock;
import dev.simulated_team.simulated.content.blocks.velocity_sensor.VelocitySensorBlock;
import dev.simulated_team.simulated.registrate.DyedBlockList;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Simulated's blocks. Block items live in {@link SimItems}, block entity types in {@link SimBlockEntityTypes}, render
 * layers in {@code SimBlockLayers} (client). Loot tables, models, tags and recipes are shipped as JSON.
 */
@SuppressWarnings("deprecation")
public class SimBlocks {
    private static final SimulatedRegistrate REGISTRATE = Simulated.getRegistrate();

    public static final PhysicsAssemblerBlock PHYSICS_ASSEMBLER = register(
            "physics_assembler",
            PhysicsAssemblerBlock::new,
            wooden()
    );

    public static final SwivelBearingBlock SWIVEL_BEARING = register(
            "swivel_bearing",
            SwivelBearingBlock::new,
            netheriteMetal().destroyTime(5f).noOcclusion()
    );

    public static final SwivelBearingPlateBlock SWIVEL_BEARING_LINK_BLOCK = register(
            "swivel_bearing_link_block",
            SwivelBearingPlateBlock::new,
            netheriteMetal().destroyTime(5f)
    );

    public static final MergingGlueBlock MERGING_GLUE = register(
            "merging_glue",
            MergingGlueBlock::new,
            BlockBehaviour.Properties.of().noOcclusion().instabreak().mapColor(MapColor.GRASS).friction(0.8F).sound(SoundType.SLIME_BLOCK)
    );

    public static final RopeWinchBlock ROPE_WINCH = register(
            "rope_winch",
            RopeWinchBlock::new,
            stone().noOcclusion()
    );

    public static final RopeConnectorBlock ROPE_CONNECTOR = register(
            "rope_connector",
            RopeConnectorBlock::new,
            stone().noOcclusion().forceSolidOn()
    );

    public static final HandleBlock IRON_HANDLE = handle(null, HandleBlock.Variant.IRON);

    public static final HandleBlock COPPER_HANDLE = handle(null, HandleBlock.Variant.COPPER);

    public static final DyedBlockList<HandleBlock> DYED_HANDLES = new DyedBlockList<>(color -> handle(color, HandleBlock.Variant.DYED));

    public static final DirectionalGearshiftBlock DIRECTIONAL_GEARSHIFT = register(
            "directional_gearshift",
            DirectionalGearshiftBlock::new,
            stone().noOcclusion().isRedstoneConductor(SimBlocks::never).mapColor(MapColor.PODZOL)
    );

    public static final TorsionSpringBlock TORSION_SPRING = register(
            "torsion_spring",
            TorsionSpringBlock::new,
            stone().noOcclusion()
    );

    public static final AugerShaftBlock AUGER_SHAFT = register(
            "auger_shaft",
            AugerShaftBlock::new,
            softMetal().sound(SoundType.NETHERITE_BLOCK).noOcclusion()
    );

    public static final AugerCogBlock AUGER_COG = register(
            "auger_cog",
            AugerCogBlock::new,
            softMetal().sound(SoundType.NETHERITE_BLOCK).noOcclusion()
    );

    public static final DyedBlockList<PortableEngineBlock> PORTABLE_ENGINES = new DyedBlockList<>(color -> register(
            color.getSerializedName() + "_portable_engine",
            p -> new PortableEngineBlock(p, color),
            stone().sound(SoundType.NETHERITE_BLOCK).lightLevel(state -> PortableEngineBlock.isLitState(state) ? 6 : 0).noOcclusion()
    ));

    public static final PortableEngineBlock RED_PORTABLE_ENGINE = PORTABLE_ENGINES.get(DyeColor.RED);

    public static final LaserPointerBlock LASER_POINTER = register(
            "laser_pointer",
            LaserPointerBlock::new,
            softMetal()
    );

    public static final LaserSensorBlock LASER_SENSOR = register(
            "laser_sensor",
            LaserSensorBlock::new,
            wooden().noOcclusion()
    );

    public static final VelocitySensorBlock VELOCITY_SENSOR = register(
            "velocity_sensor",
            VelocitySensorBlock::new,
            wooden().noOcclusion()
    );

    public static final AltitudeSensorBlock ALTITUDE_SENSOR = register(
            "altitude_sensor",
            AltitudeSensorBlock::new,
            wooden().noOcclusion()
    );

    public static final GimbalSensorBlock GIMBAL_SENSOR = register(
            "gimbal_sensor",
            GimbalSensorBlock::new,
            softMetal().noOcclusion().mapColor(MapColor.TERRACOTTA_YELLOW)
    );

    public static final NavTableBlock NAVIGATION_TABLE = register(
            "navigation_table",
            NavTableBlock::new,
            wooden().noOcclusion()
    );

    public static final OpticalSensorBlock OPTICAL_SENSOR = register(
            "optical_sensor",
            OpticalSensorBlock::new,
            softMetal().noOcclusion()
    );

    public static final DockingConnectorBlock DOCKING_CONNECTOR = register(
            "docking_connector",
            DockingConnectorBlock::new,
            stone().sound(SoundType.NETHERITE_BLOCK).isRedstoneConductor(SimBlocks::never).forceSolidOn().noOcclusion().dynamicShape()
    );

    public static final PairedDockingConnectorBlock PAIRED_DOCKING_CONNECTOR = register(
            "paired_docking_connector",
            PairedDockingConnectorBlock::new,
            stone().sound(SoundType.NETHERITE_BLOCK).noOcclusion().noLootTable().pushReaction(PushReaction.BLOCK).forceSolidOff()
    );

    public static final AnalogTransmissionBlock ANALOG_TRANSMISSION = register(
            "analog_transmission",
            AnalogTransmissionBlock::new,
            stone().noOcclusion().isRedstoneConductor(SimBlocks::never)
    );

    public static final SteeringWheelBlock STEERING_WHEEL = register(
            "steering_wheel",
            SteeringWheelBlock::new,
            wooden().mapColor(MapColor.PODZOL)
    );

    public static final ThrottleLeverBlock THROTTLE_LEVER = register(
            "throttle_lever",
            ThrottleLeverBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.LEVER)
    );

    public static final LinkedTypewriterBlock LINKED_TYPEWRITER = register(
            "linked_typewriter",
            LinkedTypewriterBlock::new,
            softMetal()
    );

    public static final DirectionalLinkedReceiverBlock DIRECTIONAL_LINKED_RECEIVER = register(
            "directional_linked_receiver",
            DirectionalLinkedReceiverBlock::new,
            stone().noOcclusion().forceSolidOn()
    );

    public static final ModulatingLinkedReceiverBlock MODULATING_LINKED_RECEIVER = register(
            "modulating_linked_receiver",
            ModulatingLinkedReceiverBlock::new,
            stone().noOcclusion().forceSolidOn()
    );

    public static final RedstoneAccumulatorBlock REDSTONE_ACCUMULATOR = register(
            "redstone_accumulator",
            RedstoneAccumulatorBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.REPEATER)
    );

    public static final RedstoneInductorBlock REDSTONE_INDUCTOR = register(
            "redstone_inductor",
            RedstoneInductorBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.REPEATER)
    );

    // ABSORBER is disabled upstream (copper metal, no occlusion, cutout layer, has a block item)

    public static final RedstoneMagnetBlock REDSTONE_MAGNET = register(
            "redstone_magnet",
            RedstoneMagnetBlock::new,
            stone().sound(SoundType.NETHERITE_BLOCK).noOcclusion()
    );

    public static final SymmetricSailBlock WHITE_SYMMETRIC_SAIL = register(
            "white_symmetric_sail",
            p -> SymmetricSailBlock.withCanvas(p, DyeColor.WHITE),
            wooden().sound(SoundType.SCAFFOLDING).noOcclusion()
    );

    public static final DyedBlockList<SymmetricSailBlock> DYED_SYMMETRIC_SAILS = new DyedBlockList<>(colour -> {
        if (colour == DyeColor.WHITE) {
            return WHITE_SYMMETRIC_SAIL;
        }
        return register(
                colour.getSerializedName() + "_symmetric_sail",
                p -> SymmetricSailBlock.withCanvas(p, colour),
                wooden().sound(SoundType.SCAFFOLDING)
        );
    });

    public static final DyedBlockList<NameplateBlock> NAMEPLATES = new DyedBlockList<>(color -> register(
            color.getSerializedName() + "_nameplate",
            p -> new NameplateBlock(p, color),
            wooden().forceSolidOn()
    ));

    public static final SpringBlock SPRING = register(
            "spring",
            SpringBlock::new,
            softMetal().forceSolidOn()
    );

    private static HandleBlock handle(@Nullable final DyeColor color, final HandleBlock.Variant variant) {
        final String prefix;
        if (variant == HandleBlock.Variant.DYED && color != null) {
            prefix = color.getSerializedName();
        } else {
            prefix = variant.getSerializedName();
        }

        return register(
                prefix + "_handle",
                p -> new HandleBlock(p, color, variant),
                stone().sound(SoundType.COPPER).noOcclusion().forceSolidOn()
        );
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

    private static BlockBehaviour.Properties netheriteMetal() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK);
    }

    private static boolean never(final BlockState state, final BlockGetter blockGetter, final BlockPos pos) {
        return false;
    }

    private static <T extends Block> T register(final String name, final Function<BlockBehaviour.Properties, T> factory, final BlockBehaviour.Properties properties) {
        return REGISTRATE.block(name, factory, properties);
    }

    /**
     * Everything Registrate did per block besides registering it: stress, generators, display sources and targets,
     * movement behaviours and item use overrides. Must run before the server config is created (stress defaults).
     */
    public static void register() {
        SimStress.setImpact(SWIVEL_BEARING, 4.0);
        SimStress.setImpact(ROPE_WINCH, 4.0);
        SimStress.setImpact(TORSION_SPRING, 16.0);
        SimStress.setCapacity(TORSION_SPRING, 8.0);
        SimStress.setImpact(AUGER_SHAFT, 0.5);
        SimStress.setImpact(AUGER_COG, 0.5);
        for (final PortableEngineBlock engine : PORTABLE_ENGINES) {
            SimStress.setCapacity(engine, 64.0);
            BlockStressValues.setGeneratorSpeed(engine, 32);
        }
        SimStress.setCapacity(STEERING_WHEEL, 16.0);
        BlockStressValues.setGeneratorSpeed(STEERING_WHEEL, SteeringWheelBlockEntity.RPM);

        SimDisplaySources.assign(SimDisplaySources.AUGER_DISPLAY, AUGER_SHAFT, AUGER_COG);
        SimDisplaySources.assign(SimDisplaySources.PORTABLE_ENGINE_DISPLAY, PORTABLE_ENGINES.toArray());
        SimDisplaySources.assign(SimDisplaySources.LASER_SENSOR_DISPLAY, LASER_SENSOR);
        SimDisplaySources.assign(SimDisplaySources.VELO_DISPLAY, VELOCITY_SENSOR);
        SimDisplaySources.assign(SimDisplaySources.ALTITUDE_SENSOR_DISPLAY, ALTITUDE_SENSOR);
        SimDisplaySources.assign(SimDisplaySources.GIMBAL_DISPLAY, GIMBAL_SENSOR);
        SimDisplaySources.assign(SimDisplaySources.NAV_TABLE_DISPLAY, NAVIGATION_TABLE);
        SimDisplaySources.assign(SimDisplaySources.OPTICAL_SENSOR_DISPLAY, OPTICAL_SENSOR);
        SimDisplaySources.assign(SimDisplaySources.DOCKING_CONNECTOR_DISPLAY, DOCKING_CONNECTOR);
        SimDisplaySources.assign(SimDisplaySources.LINKED_TYPEWRITER__DISPLAY, LINKED_TYPEWRITER);

        for (final NameplateBlock nameplate : NAMEPLATES) {
            DisplayTarget.BY_BLOCK.register(nameplate, NameplateBlockTarget.NAMEPLATE);
        }

        SimItemUseOverrides.addBlock(IRON_HANDLE);
        SimItemUseOverrides.addBlock(COPPER_HANDLE);
        for (final HandleBlock handle : DYED_HANDLES) {
            SimItemUseOverrides.addBlock(handle);
        }
        SimItemUseOverrides.addBlock(STEERING_WHEEL);

        SimMovementBehaviours.register();
    }

}
