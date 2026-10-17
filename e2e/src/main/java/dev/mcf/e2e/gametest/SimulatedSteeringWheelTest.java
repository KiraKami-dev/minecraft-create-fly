package dev.mcf.e2e.gametest;

import com.zurrtum.create.AllBlocks;
import com.zurrtum.create.content.kinetics.base.RotatedPillarKineticBlock;
import dev.simulated_team.simulated.content.blocks.steering_wheel.SteeringWheelBlock;
import dev.simulated_team.simulated.content.blocks.steering_wheel.SteeringWheelBlockEntity;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.network.packets.SteeringWheelPacket;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;

import static dev.mcf.e2e.gametest.SableTestHelper.*;
import static dev.mcf.e2e.gametest.SimulatedTestHelper.*;

/**
 * The steering wheel is a kinetic generator the player turns: holding "use" on it sends {@link SteeringWheelPacket}s
 * with the wanted wheel angle, and the wheel then outputs {@link SteeringWheelBlockEntity#RPM} on its shaft (down for a
 * floor wheel) until its angle reaches the target, like a sequenced gearshift's "turn angle". Releasing it sends the
 * final angle; here the release targets 0, so the wheel winds back with the opposite rotation.
 */
public final class SimulatedSteeringWheelTest {

    private static final BlockPos WHEEL = new BlockPos(2, 2, 2);
    private static final BlockPos SHAFT = new BlockPos(2, 1, 2);

    @GameTest(structure = "sable:assemblytest.allblocks", maxTicks = 200)
    public void steeringWheelTurnsShaft(final GameTestHelper helper) {
        helper.setBlock(SHAFT, AllBlocks.SHAFT.defaultBlockState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Y));
        helper.setBlock(WHEEL, SimBlocks.STEERING_WHEEL.defaultBlockState()
                .setValue(SteeringWheelBlock.ON_FLOOR, true)
                .setValue(SteeringWheelBlock.FACING, absoluteFacing(helper, Direction.NORTH)));
        final float[] firstSpeed = new float[1];

        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    assertKineticsSpeed(helper, SHAFT, 0);
                    steer(helper, 90, false);
                    helper.assertTrue(wheel(helper).held, "the wheel is held after the packet");
                })
                // turning: the shaft runs at the wheel's RPM
                .thenWaitUntil(() -> {
                    final float speed = kinetic(helper, SHAFT).getSpeed();
                    if (Math.abs(Math.abs(speed) - SteeringWheelBlockEntity.RPM) > 1e-3) {
                        throw helper.assertionException(Component.literal("Shaft speed %.2f, expected +-%d".formatted(speed, SteeringWheelBlockEntity.RPM)));
                    }
                    firstSpeed[0] = speed;
                })
                // reaches 90 degrees and stops
                .thenWaitUntil(() -> {
                    assertAngle(helper, 90);
                    assertKineticsSpeed(helper, SHAFT, 0);
                })
                .thenIdle(10)
                .thenExecute(() -> {
                    assertAngle(helper, 90);
                    steer(helper, 0, true);
                    helper.assertFalse(wheel(helper).held, "the wheel is released");
                })
                // winds back with the opposite rotation
                .thenWaitUntil(() -> {
                    final float speed = kinetic(helper, SHAFT).getSpeed();
                    if (Math.abs(speed + firstSpeed[0]) > 1e-3) {
                        throw helper.assertionException(Component.literal("Shaft speed %.2f, expected %.2f".formatted(speed, -firstSpeed[0])));
                    }
                })
                .thenWaitUntil(() -> {
                    assertAngle(helper, 0);
                    assertKineticsSpeed(helper, SHAFT, 0);
                })
                .thenSucceed();
    }

    private static SteeringWheelBlockEntity wheel(final GameTestHelper helper) {
        return helper.getBlockEntity(WHEEL, SteeringWheelBlockEntity.class);
    }

    private static void assertAngle(final GameTestHelper helper, final float expected) {
        final float angle = wheel(helper).getAngle();
        if (Math.abs(angle - expected) > 1.0f) {
            throw helper.assertionException(Component.literal("Wheel angle %.2f, expected %.0f".formatted(angle, expected)));
        }
    }

    /**
     * What the client's {@code SteeringWheelHandler} sends while (and when stopping) holding the wheel, handled for a
     * fake player standing next to it.
     */
    private static void steer(final GameTestHelper helper, final float angle, final boolean stop) {
        final BlockPos worldPos = helper.absolutePos(WHEEL);
        final FakePlayer player = FakePlayer.get(helper.getLevel(), ASSEMBLING_PLAYER);
        player.setPos(worldPos.getX() + 0.5, worldPos.getY(), worldPos.getZ() + 2.0);
        helper.assertTrue(player.isWithinBlockInteractionRange(worldPos, 4), "the wheel is in reach");
        new SteeringWheelPacket(stop, angle, worldPos).handle(new FakePlayerPacketContext(player));
    }
}
