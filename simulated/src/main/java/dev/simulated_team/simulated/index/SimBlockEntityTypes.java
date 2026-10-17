package dev.simulated_team.simulated.index;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.altitude_sensor.AltitudeSensorBlockEntity;
import dev.simulated_team.simulated.content.blocks.analog_transmission.AnalogTransmissionBlockEntity;
import dev.simulated_team.simulated.content.blocks.auger_shaft.AugerShaftBlock;
import dev.simulated_team.simulated.content.blocks.auger_shaft.AugerShaftBlockEntity;
import dev.simulated_team.simulated.content.blocks.directional_gearshift.DirectionalGearshiftBlockEntity;
import dev.simulated_team.simulated.content.blocks.docking_connector.DockingConnectorBlockEntity;
import dev.simulated_team.simulated.content.blocks.gimbal_sensor.GimbalSensorBlockEntity;
import dev.simulated_team.simulated.content.blocks.handle.HandleBlockEntity;
import dev.simulated_team.simulated.content.blocks.lasers.laser_pointer.LaserPointerBlockEntity;
import dev.simulated_team.simulated.content.blocks.lasers.laser_sensor.LaserSensorBlockEntity;
import dev.simulated_team.simulated.content.blocks.lasers.optical_sensor.OpticalSensorBlockEntity;
import dev.simulated_team.simulated.content.blocks.merging_glue.MergingGlueBlockEntity;
import dev.simulated_team.simulated.content.blocks.nameplate.NameplateBlockEntity;
import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerBlockEntity;
import dev.simulated_team.simulated.content.blocks.portable_engine.PortableEngineBlockEntity;
import dev.simulated_team.simulated.content.blocks.redstone.directional_receiver.DirectionalLinkedReceiverBlockEntity;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterBlockEntity;
import dev.simulated_team.simulated.content.blocks.redstone.modulating_receiver.ModulatingLinkedReceiverBlockEntity;
import dev.simulated_team.simulated.content.blocks.redstone.redstone_accumulator.RedstoneAccumulatorBlockEntity;
import dev.simulated_team.simulated.content.blocks.redstone.redstone_inductor.RedstoneInductorBlockEntity;
import dev.simulated_team.simulated.content.blocks.redstone_magnet.RedstoneMagnetBlockEntity;
import dev.simulated_team.simulated.content.blocks.rope.rope_connector.RopeConnectorBlockEntity;
import dev.simulated_team.simulated.content.blocks.rope.rope_winch.RopeWinchBlockEntity;
import dev.simulated_team.simulated.content.blocks.spring.SpringBlockEntity;
import dev.simulated_team.simulated.content.blocks.steering_wheel.SteeringWheelBlockEntity;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.SwivelBearingBlockEntity;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.link_block.SwivelBearingPlateBlockEntity;
import dev.simulated_team.simulated.content.blocks.throttle_lever.ThrottleLeverBlockEntity;
import dev.simulated_team.simulated.content.blocks.torsion_spring.TorsionSpringBlockEntity;
import dev.simulated_team.simulated.content.blocks.velocity_sensor.VelocitySensorBlockEntity;
import dev.simulated_team.simulated.multiloader.energy.SingleBattery;
import dev.simulated_team.simulated.multiloader.inventory.AbstractContainer;
import dev.simulated_team.simulated.multiloader.tanks.SingleTank;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import dev.simulated_team.simulated.service.SimInventoryService;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.BiFunction;

/**
 * Block entity types. Renderers and Flywheel visuals are registered on the client in {@code SimBlockEntityRenders}.
 */
public class SimBlockEntityTypes {
    private static final SimulatedRegistrate REGISTRATE = Simulated.getRegistrate();

    public static final BlockEntityType<AnalogTransmissionBlockEntity> SIMPLE_BE = REGISTRATE.blockEntity(
            "simple", AnalogTransmissionBlockEntity::new, SimBlocks.ANALOG_TRANSMISSION);

    public static final BlockEntityType<TorsionSpringBlockEntity> TORSION_SPRING = REGISTRATE.blockEntity(
            "torsion_spring", TorsionSpringBlockEntity::new, SimBlocks.TORSION_SPRING);

    public static final BlockEntityType<PhysicsAssemblerBlockEntity> PHYSICS_ASSEMBLER = REGISTRATE.blockEntity(
            "physics_assembler", PhysicsAssemblerBlockEntity::new, SimBlocks.PHYSICS_ASSEMBLER);

    public static final BlockEntityType<RopeWinchBlockEntity> ROPE_WINCH = REGISTRATE.blockEntity(
            "rope_winch", RopeWinchBlockEntity::new, SimBlocks.ROPE_WINCH);

    public static final BlockEntityType<RopeConnectorBlockEntity> ROPE_CONNECTOR = REGISTRATE.blockEntity(
            "rope_connector", RopeConnectorBlockEntity::new, SimBlocks.ROPE_CONNECTOR);

    public static final BlockEntityType<DockingConnectorBlockEntity> DOCKING_CONNECTOR = REGISTRATE.blockEntity(
            "docking_connector", DockingConnectorBlockEntity::new, SimBlocks.DOCKING_CONNECTOR);

    public static final BlockEntityType<AltitudeSensorBlockEntity> ALTITUDE_SENSOR = REGISTRATE.blockEntity(
            "altitude_sensor", AltitudeSensorBlockEntity::new, SimBlocks.ALTITUDE_SENSOR);

    public static final BlockEntityType<GimbalSensorBlockEntity> GIMBAL_SENSOR = REGISTRATE.blockEntity(
            "gimbal_sensor", GimbalSensorBlockEntity::new, SimBlocks.GIMBAL_SENSOR);

    public static final BlockEntityType<HandleBlockEntity> HANDLE = REGISTRATE.blockEntity(
            "handle", HandleBlockEntity::new, concat(SimBlocks.DYED_HANDLES.toArray(), SimBlocks.IRON_HANDLE, SimBlocks.COPPER_HANDLE));

    public static final BlockEntityType<PortableEngineBlockEntity> PORTABLE_ENGINE = REGISTRATE.blockEntity(
            "portable_engine", PortableEngineBlockEntity::new, SimBlocks.PORTABLE_ENGINES.toArray());

    public static final BlockEntityType<SwivelBearingBlockEntity> SWIVEL_BEARING = REGISTRATE.blockEntity(
            "swivel_bearing", SwivelBearingBlockEntity::new, SimBlocks.SWIVEL_BEARING);

    public static final BlockEntityType<SteeringWheelBlockEntity> STEERING_WHEEL = REGISTRATE.blockEntity(
            "steering_wheel", SteeringWheelBlockEntity::new, SimBlocks.STEERING_WHEEL);

    public static final BlockEntityType<SpringBlockEntity> SPRING = REGISTRATE.blockEntity(
            "spring", SpringBlockEntity::new, SimBlocks.SPRING);

    public static final BlockEntityType<SwivelBearingPlateBlockEntity> SWIVEL_BEARING_LINK_BLOCK = REGISTRATE.blockEntity(
            "swivel_bearing_link_block", SwivelBearingPlateBlockEntity::new, SimBlocks.SWIVEL_BEARING_LINK_BLOCK);

    public static final BlockEntityType<LinkedTypewriterBlockEntity> LINKED_TYPEWRITER = REGISTRATE.blockEntity(
            "linked_typewriter", LinkedTypewriterBlockEntity::new, SimBlocks.LINKED_TYPEWRITER);

    // Lasers

    public static final BlockEntityType<OpticalSensorBlockEntity> OPTICAL_SENSOR = REGISTRATE.blockEntity(
            "optical_sensor", OpticalSensorBlockEntity::new, SimBlocks.OPTICAL_SENSOR);

    public static final BlockEntityType<LaserPointerBlockEntity> LASER_POINTER = REGISTRATE.blockEntity(
            "laser_pointer", LaserPointerBlockEntity::new, SimBlocks.LASER_POINTER);

    public static final BlockEntityType<LaserSensorBlockEntity> LASER_SENSOR = REGISTRATE.blockEntity(
            "ir_sensor", LaserSensorBlockEntity::new, SimBlocks.LASER_SENSOR);

    // Redstone

    public static final BlockEntityType<VelocitySensorBlockEntity> VELOCITY_SENSOR = REGISTRATE.blockEntity(
            "velocity_sensor", VelocitySensorBlockEntity::new, SimBlocks.VELOCITY_SENSOR);

    public static final BlockEntityType<DirectionalLinkedReceiverBlockEntity> DIRECTIONAL_LINKED_RECEIVER = REGISTRATE.blockEntity(
            "directional_linked_receiver", DirectionalLinkedReceiverBlockEntity::new, SimBlocks.DIRECTIONAL_LINKED_RECEIVER);

    public static final BlockEntityType<NameplateBlockEntity> NAMEPLATE = REGISTRATE.blockEntity(
            "nameplate", NameplateBlockEntity::new, SimBlocks.NAMEPLATES.toArray());

    public static final BlockEntityType<ModulatingLinkedReceiverBlockEntity> MODULATING_LINKED_RECEIVER = REGISTRATE.blockEntity(
            "modulating_linked_receiver", ModulatingLinkedReceiverBlockEntity::new, SimBlocks.MODULATING_LINKED_RECEIVER);

    public static final BlockEntityType<RedstoneAccumulatorBlockEntity> REDSTONE_ACCUMULATOR = REGISTRATE.blockEntity(
            "redstone_accumulator", RedstoneAccumulatorBlockEntity::new, SimBlocks.REDSTONE_ACCUMULATOR);

    public static final BlockEntityType<NavTableBlockEntity> NAVIGATION_TABLE = REGISTRATE.blockEntity(
            "navigation_table", NavTableBlockEntity::new, SimBlocks.NAVIGATION_TABLE);

    public static final BlockEntityType<RedstoneInductorBlockEntity> REDSTONE_INDUCTOR = REGISTRATE.blockEntity(
            "redstone_inductor", RedstoneInductorBlockEntity::new, SimBlocks.REDSTONE_INDUCTOR);

    public static final BlockEntityType<RedstoneMagnetBlockEntity> REDSTONE_MAGNET = REGISTRATE.blockEntity(
            "redstone_magnet", RedstoneMagnetBlockEntity::new, SimBlocks.REDSTONE_MAGNET);

    public static final BlockEntityType<ThrottleLeverBlockEntity> THROTTLE_LEVER = REGISTRATE.blockEntity(
            "throttle_lever", ThrottleLeverBlockEntity::new, SimBlocks.THROTTLE_LEVER);

    // ABSORBER is disabled upstream

    public static final BlockEntityType<AugerShaftBlockEntity> AUGER_SHAFT = REGISTRATE.blockEntity(
            "auger_shaft", AugerShaftBlockEntity::new, SimBlocks.AUGER_SHAFT, SimBlocks.AUGER_COG);

    public static final BlockEntityType<MergingGlueBlockEntity> MERGING_GLUE = REGISTRATE.blockEntity(
            "merging_glue", MergingGlueBlockEntity::new, SimBlocks.MERGING_GLUE);

    public static final BlockEntityType<DirectionalGearshiftBlockEntity> DIRECTIONAL_GEARSHIFT = REGISTRATE.blockEntity(
            "directional_gearshift", DirectionalGearshiftBlockEntity::new, SimBlocks.DIRECTIONAL_GEARSHIFT);

    private static net.minecraft.world.level.block.Block[] concat(final net.minecraft.world.level.block.Block[] blocks, final net.minecraft.world.level.block.Block... more) {
        final net.minecraft.world.level.block.Block[] result = java.util.Arrays.copyOf(more, more.length + blocks.length);
        System.arraycopy(blocks, 0, result, more.length, blocks.length);
        return result;
    }

    private static <T extends BlockEntity> void inventory(final BlockEntityType<T> type, final BiFunction<T, Direction, AbstractContainer> getter) {
        SimInventoryService.INSTANCE.registerInventory(getter).accept(type);
    }

    private static <T extends BlockEntity> void tank(final BlockEntityType<T> type, final BiFunction<T, Direction, SingleTank> getter) {
        SimInventoryService.INSTANCE.registerTank(getter).accept(type);
    }

    private static <T extends BlockEntity> void battery(final BlockEntityType<T> type, final BiFunction<T, Direction, SingleBattery> getter) {
        SimInventoryService.INSTANCE.registerBattery(getter).accept(type);
    }

    /**
     * Exposes block entity storages to other mods (upstream: Registrate {@code onRegister} capability hooks).
     */
    public static void register() {
        inventory(DOCKING_CONNECTOR, (be, dir) -> be.getInventory());
        tank(DOCKING_CONNECTOR, (be, dir) -> be.tank);
        battery(DOCKING_CONNECTOR, (be, dir) -> be.battery);

        inventory(PORTABLE_ENGINE, (be, dir) -> be.inventory);

        inventory(NAVIGATION_TABLE, (be, dir) -> be.inventory);

        inventory(AUGER_SHAFT, (be, dir) -> {
            if (dir == null) {
                return null; //don't just give our inventory to non-directional requests.
            } else {
                if (be.getBlockState().is(SimBlocks.AUGER_COG)) {
                    return null; //don't give out inventory to requests when we are a cog auger.
                }

                return dir.getAxis() == be.getBlockState().getValue(AugerShaftBlock.AXIS) ? null : be.getInventory();
            }
        });
    }
}
