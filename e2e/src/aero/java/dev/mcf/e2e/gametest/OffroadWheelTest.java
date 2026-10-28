package dev.mcf.e2e.gametest;

import com.zurrtum.create.AllBlocks;
import com.zurrtum.create.content.kinetics.motor.CreativeMotorBlockEntity;
import dev.mcf.e2e.E2E;
import dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountBlock;
import dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountBlockEntity;
import dev.ryanhcode.offroad.index.OffroadBlocks;
import dev.ryanhcode.offroad.index.OffroadItems;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

import static dev.mcf.e2e.gametest.SableTestHelper.*;
import static dev.mcf.e2e.gametest.SimulatedTestHelper.*;

/**
 * Create Offroad wheels (Phase 3): a glued 3x1x3 car with a wheel mount on each corner, each holding a tire, built on
 * the empty 5x4x5 floor structure {@code sable:assemblytest.allblocks} (floor top face at local y 1). Both axles are
 * driven by creative motors. Built only with {@code -Pe2eAero} (see e2e/build.gradle).
 * <p>A gearbox between two wheel mounts hands them opposite speeds, and the car then spins on the spot; that is
 * upstream behaviour (the mount pushes along its rolling direction by its signed speed), not a port bug.
 */
public final class OffroadWheelTest {

    private static final BlockPos ASSEMBLER = new BlockPos(2, 3, 2);
    // each axle is a cogwheel between its two mounts, turned by a cogwheel above it and a creative motor next to that
    private static final int[] AXLES_Z = {1, 3};
    private static final BlockPos[] WEST_MOUNTS = {new BlockPos(1, 2, 1), new BlockPos(1, 2, 3)};
    private static final BlockPos[] EAST_MOUNTS = {new BlockPos(3, 2, 1), new BlockPos(3, 2, 3)};
    private static final int RPM = 128;

    /**
     * Deck one block above the floor: wheel mounts on the corners facing out (their wheels hang beside the deck), an
     * axle cogwheel between each pair turning both mounts the same way, driven through a cogwheel above by a stopped
     * creative motor, planks elsewhere.
     */
    private static void buildCar(final GameTestHelper helper, final boolean tires) {
        fill(helper, new BlockPos(1, 2, 1), new BlockPos(3, 2, 3), Blocks.OAK_PLANKS.defaultBlockState());
        for (final BlockPos pos : WEST_MOUNTS) {
            placeMount(helper, pos, Direction.WEST, tires);
        }
        for (final BlockPos pos : EAST_MOUNTS) {
            placeMount(helper, pos, Direction.EAST, tires);
        }
        final BlockState cog = AllBlocks.COGWHEEL.defaultBlockState().setValue(BlockStateProperties.AXIS, absoluteFacing(helper, Direction.EAST).getAxis());
        for (final int z : AXLES_Z) {
            helper.setBlock(new BlockPos(2, 2, z), cog);
            helper.setBlock(new BlockPos(2, 3, z), cog);
            placeMotor(helper, new BlockPos(1, 3, z), Direction.EAST, 0);
        }
        placeAssembler(helper, ASSEMBLER);
        glue(helper, new BlockPos(1, 2, 1), new BlockPos(3, 3, 3));
    }

    private static void placeMount(final GameTestHelper helper, final BlockPos pos, final Direction facing, final boolean tire) {
        helper.setBlock(pos, OffroadBlocks.WHEEL_MOUNT.defaultBlockState().setValue(WheelMountBlock.HORIZONTAL_FACING, absoluteFacing(helper, facing)));
        if (tire) {
            helper.getBlockEntity(pos, WheelMountBlockEntity.class).getInventory().setItem(0, new ItemStack(OffroadItems.TIRE));
        }
    }

    /**
     * With tires the suspension carries the car: within 3 s it settles upright with its deck clearly above where it would
     * rest without wheels (on the floor, bottom at y 1), bounces less than 0.05 blocks after that, and stays loaded. A car
     * without tires is the control.
     */
    @GameTest(structure = "sable:assemblytest.allblocks", maxTicks = 300)
    public void tiresHoldCarUp(final GameTestHelper helper) {
        buildCar(helper, true);
        final SubLevelProbe[] probe = new SubLevelProbe[1];
        final double[] settledY = new double[1];
        final double[] bounce = new double[1];

        helper.startSequence()
                .thenExecuteAfter(1, () -> {
                    final ServerSubLevel car = assembleWithAssembler(helper, ASSEMBLER);
                    helper.assertValueEqual(4, findInPlot(car, state -> state.is(OffroadBlocks.WHEEL_MOUNT)).size(), "wheel mounts on the car");
                    probe[0] = new SubLevelProbe(helper, car).sampleEveryTick();
                })
                .thenExecuteAfter(60, () -> {
                    probe[0].sample();
                    settledY[0] = probe[0].bottom();
                })
                .thenExecuteFor(40, () -> {
                    probe[0].sample();
                    bounce[0] = Math.max(bounce[0], Math.abs(probe[0].bottom() - settledY[0]));
                })
                .thenExecute(() -> {
                    probe[0].assertLoaded();
                    log("tiresHoldCarUp: bounce %.4f over 2 s after settling".formatted(bounce[0]));
                    if (bounce[0] > 0.05) {
                        helper.fail("The parked car keeps bouncing on its suspension (%.3f blocks): %s".formatted(bounce[0], probe[0]));
                    }
                    log("tiresHoldCarUp: rested with bottom at y %.3f: %s".formatted(probe[0].bottom(), probe[0]));
                    if (probe[0].bottom() < 1.25) {
                        helper.fail("The car rests on its hull instead of its tires (bottom at y %.3f): %s".formatted(probe[0].bottom(), probe[0]));
                    }
                    final Vector3d moved = probe[0].displacement();
                    if (Math.abs(moved.x) > 0.5 || Math.abs(moved.z) > 0.5) {
                        helper.fail("The parked car rolled away: " + probe[0]);
                    }
                })
                .thenSucceed();
    }

    /**
     * Control for {@link #tiresHoldCarUp}: without tires the wheel mounts do nothing and the car sits on the floor.
     */
    @GameTest(structure = "sable:assemblytest.allblocks", maxTicks = 300)
    public void noTiresRestsOnFloor(final GameTestHelper helper) {
        buildCar(helper, false);
        final SubLevelProbe[] probe = new SubLevelProbe[1];

        helper.startSequence()
                .thenExecuteAfter(1, () -> probe[0] = new SubLevelProbe(helper, assembleWithAssembler(helper, ASSEMBLER)).sampleEveryTick())
                .thenWaitUntil(() -> {
                    probe[0].sample();
                    probe[0].assertResting(20);
                })
                .thenExecute(() -> {
                    log("noTiresRestsOnFloor: rested with bottom at y %.3f".formatted(probe[0].bottom()));
                    if (Math.abs(probe[0].bottom() - 1.0) > 0.1) {
                        helper.fail("Without tires the car should rest on the floor (bottom at y %.3f): %s".formatted(probe[0].bottom(), probe[0]));
                    }
                })
                .thenSucceed();
    }

    /**
     * Driving: once the car rests on its tires, the motors turn all four wheels the same way and the car moves along its length
     * (local z, the wheels' rolling direction), not sideways, and stays on its wheels.
     */
    @GameTest(structure = "sable:assemblytest.allblocks", maxTicks = 400)
    public void motorDrivesCar(final GameTestHelper helper) {
        buildCar(helper, true);
        final ServerSubLevel[] car = new ServerSubLevel[1];
        final SubLevelProbe[] probe = new SubLevelProbe[1];
        final List<CreativeMotorBlockEntity> motors = new ArrayList<>();
        final Vector3d[] parked = new Vector3d[1];

        helper.startSequence()
                .thenExecuteAfter(1, () -> {
                    car[0] = assembleWithAssembler(helper, ASSEMBLER);
                    for (final BlockPos pos : findInPlot(car[0], state -> state.is(AllBlocks.CREATIVE_MOTOR))) {
                        motors.add(plotBlockEntity(car[0], pos, CreativeMotorBlockEntity.class));
                    }
                    helper.assertValueEqual(2, motors.size(), "creative motors on the car");
                    probe[0] = new SubLevelProbe(helper, car[0]).sampleEveryTick();
                })
                .thenExecuteAfter(60, () -> {
                    parked[0] = localPosition(helper, car[0]);
                    motors.forEach(motor -> motor.generatedSpeed.setValue(RPM));
                })
                .thenExecuteAfter(2, () -> {
                    // all mounts must turn the same way (each pushes along +-z by the sign of its speed)
                    final List<Float> speeds = new ArrayList<>();
                    for (final BlockPos mount : findInPlot(car[0], state -> state.is(OffroadBlocks.WHEEL_MOUNT))) {
                        final float speed = plotBlockEntity(car[0], mount, WheelMountBlockEntity.class).getSpeed();
                        if (speed != 0) {
                            speeds.add(speed);
                        }
                    }
                    helper.assertValueEqual(4, speeds.size(), "driven wheel mounts");
                    if (speeds.stream().map(Math::signum).distinct().count() != 1) {
                        helper.fail("The axles turn the wheel mounts different ways: " + speeds);
                    }
                })
                .thenExecuteAfter(30, () -> {
                    probe[0].assertLoaded();
                    final Vector3d moved = localPosition(helper, car[0]).sub(parked[0]);
                    log("motorDrivesCar: moved %s in 1.5 s at %d RPM: %s".formatted(SubLevelProbe.format(moved), RPM, probe[0]));
                    if (Math.abs(moved.z) < 0.3) {
                        helper.fail("The driven car did not move along its length: moved %s, %s".formatted(SubLevelProbe.format(moved), probe[0]));
                    }
                    if (Math.abs(moved.x) > Math.abs(moved.z)) {
                        helper.fail("The driven car slid sideways: moved %s".formatted(SubLevelProbe.format(moved)));
                    }
                    if (probe[0].bottom() < 1.1) {
                        helper.fail("The driven car dropped onto its hull: " + probe[0]);
                    }
                })
                .thenSucceed();
    }

    private static void log(final String message) {
        E2E.LOG.info("E2E: offroad " + message);
    }
}
