package dev.mcf.e2e.gametest;

import com.zurrtum.create.AllBlocks;
import com.zurrtum.create.content.kinetics.motor.CreativeMotorBlockEntity;
import dev.eriksonn.aeronautics.index.AeroBlocks;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import static dev.mcf.e2e.gametest.SableTestHelper.*;
import static dev.mcf.e2e.gametest.SimulatedTestHelper.*;

/**
 * Propeller thrust (ROADMAP T2, Aeronautics): a propeller bearing with sails ("blades") driven by a creative motor on
 * an assembled ship lifts it. Built only with {@code -Pe2eAero} (see e2e/build.gradle).
 *
 * <p>Not run yet: Aeronautics does not compile on 1.21.11 so far, so the test is optional until it has passed once.
 * The thrust direction of this build was never observed on the port, so the test does not assume it: if the rotor
 * pushes the ship into the floor at +256 RPM, it reverses the motor and then requires the lift.
 */
public final class AeronauticsPropellerTest {

    private static final int RPM = 256;

    /**
     * Deck 3x1x3 on the floor of the 5x13x5 shaft with a creative motor (0 RPM) in its centre facing up, a propeller
     * bearing on the motor facing up, a 3x3 rotor above it (a plank hub + 8 sails, glued), and a physics assembler on a
     * deck corner. After assembly the motor is started on the ship: the bearing assembles the rotor and its thrust must
     * lift the ship more than a block within 3 s.
     */
    @GameTest(structure = "sable:physicstest.gravity", maxTicks = 300, required = false)
    public void propellerBearingLiftsShip(final GameTestHelper helper) {
        final BlockPos motor = new BlockPos(2, 1, 2);
        final BlockPos bearing = new BlockPos(2, 2, 2);
        final BlockPos hub = new BlockPos(2, 3, 2);
        final BlockPos assemblerPos = new BlockPos(1, 2, 1);

        fill(helper, new BlockPos(1, 1, 1), new BlockPos(3, 1, 3), Blocks.OAK_PLANKS.defaultBlockState());
        placeMotor(helper, motor, Direction.UP, 0);
        helper.setBlock(bearing, AeroBlocks.PROPELLER_BEARING.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP));
        fill(helper, new BlockPos(1, 3, 1), new BlockPos(3, 3, 3), AllBlocks.SAIL.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP));
        helper.setBlock(hub, Blocks.OAK_PLANKS);
        placeAssembler(helper, assemblerPos);
        glue(helper, new BlockPos(1, 1, 1), new BlockPos(3, 2, 3));
        glue(helper, new BlockPos(1, 3, 1), new BlockPos(3, 3, 3));
        final int shipBlocks = 9 + 1 + 9 + 1;

        final ServerSubLevel[] ship = new ServerSubLevel[1];
        final SubLevelProbe[] probe = new SubLevelProbe[1];
        final BlockPos[] plotMotor = new BlockPos[1];
        final boolean[] reversed = new boolean[1];

        helper.startSequence()
                .thenExecuteAfter(1, () -> {
                    ship[0] = assembleWithAssembler(helper, assemblerPos);
                    helper.assertValueEqual(shipBlocks, countBlocks(ship[0]), "blocks in the ship");
                    plotMotor[0] = findOneInPlot(helper, ship[0], state -> state.is(AllBlocks.CREATIVE_MOTOR), "creative motor");
                    probe[0] = new SubLevelProbe(helper, ship[0]).sampleEveryTick();
                    setMotor(helper, ship[0], plotMotor[0], RPM);
                })
                .thenExecuteAfter(60, () -> {
                    if (probe(helper, probe).displacement().y() < 0.5) {
                        // pushed down (or not at all) at +RPM: the other direction must lift
                        reversed[0] = true;
                        setMotor(helper, ship[0], plotMotor[0], -RPM);
                    }
                })
                .thenWaitUntil(() -> {
                    if (probe(helper, probe).displacement().y() < 1.0) {
                        throw helper.assertionException(Component.literal("The propeller has not lifted the ship by a block (motor %s RPM): %s"
                                .formatted(reversed[0] ? -RPM : RPM, probe[0])));
                    }
                })
                .thenExecute(() -> {
                    helper.assertValueEqual(shipBlocks, countBlocks(ship[0]), "blocks in the ship after lift-off");
                    // airflow caps the climb at about 0.1 * RPM m/s
                    if (probe[0].maxSpeed() > 0.1 * RPM + 5) {
                        helper.fail("The ship climbs implausibly fast: " + probe[0]);
                    }
                })
                .thenSucceed();
    }

    private static void setMotor(final GameTestHelper helper, final ServerSubLevel ship, final BlockPos plotMotor, final int rpm) {
        final CreativeMotorBlockEntity motor = plotBlockEntity(ship, plotMotor, CreativeMotorBlockEntity.class);
        if (motor == null) {
            throw helper.assertionException(Component.literal("No creative motor block entity on the ship"));
        }
        motor.generatedSpeed.setValue(rpm);
    }

    private static SubLevelProbe probe(final GameTestHelper helper, final SubLevelProbe[] probe) {
        if (probe[0] == null) {
            throw helper.assertionException(Component.literal("No ship (assembly failed)"));
        }
        return probe[0];
    }
}
