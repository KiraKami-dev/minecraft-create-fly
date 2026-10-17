package dev.mcf.e2e.gametest;

import com.zurrtum.create.AllBlocks;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerBlockEntity;
import dev.simulated_team.simulated.content.blocks.torsion_spring.TorsionSpringBlockEntity;
import dev.simulated_team.simulated.index.SimBlocks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.List;

import static dev.mcf.e2e.gametest.SableTestHelper.*;
import static dev.mcf.e2e.gametest.SimulatedTestHelper.*;

/**
 * Simulated ships (ROADMAP T2): a glued hull is assembled by a physics assembler, triggered the way a player does it
 * (the assembler's {@code AssemblePacket} handler with a player in reach), then falls, rests, carries working Create
 * kinetics and disassembles back into the world. All tests build on the empty 5x4x5 floor structure
 * {@code sable:assemblytest.allblocks} (floor at local y 0, top face y 1).
 */
public final class SimulatedShipTest {

    private static final BlockPos DECK_MIN = new BlockPos(1, 2, 1);
    private static final BlockPos DECK_MAX = new BlockPos(3, 2, 3);
    private static final BlockPos ASSEMBLER = new BlockPos(2, 3, 2);
    /**
     * A block resting on the deck but outside the glue: it is not attached, so it must stay in the world.
     */
    private static final BlockPos LOOSE_BLOCK = new BlockPos(1, 3, 1);
    private static final int HULL_BLOCKS = 9 + 1;

    /**
     * Builds a floating 3x1x3 glued plank deck one block above the floor, a physics assembler on its centre and a loose
     * stone block on one corner.
     */
    private static void buildHull(final GameTestHelper helper) {
        fill(helper, DECK_MIN, DECK_MAX, Blocks.OAK_PLANKS.defaultBlockState());
        glue(helper, DECK_MIN, DECK_MAX);
        placeAssembler(helper, ASSEMBLER);
        helper.setBlock(LOOSE_BLOCK, Blocks.STONE);
    }

    /**
     * Assembles the hull with its assembler and checks the result: one sub-level holding the deck and the assembler,
     * nothing left behind in the world except the loose block, nothing dropped, the glue moved along.
     */
    private static ServerSubLevel assembleHull(final GameTestHelper helper) {
        final int gluesBefore = gluesInTest(helper).size();
        final ServerSubLevel ship = assembleWithAssembler(helper, ASSEMBLER);
        if (countBlocks(ship) != HULL_BLOCKS) {
            helper.fail("The ship holds %d blocks instead of the %d glued deck blocks + assembler: %s".formatted(countBlocks(ship), HULL_BLOCKS, describePlot(ship)));
        }
        helper.assertValueEqual(9, findInPlot(ship, state -> state.is(Blocks.OAK_PLANKS)).size(), "planks in the ship");
        final BlockPos plotAssembler = findOneInPlot(helper, ship, state -> state.is(SimBlocks.PHYSICS_ASSEMBLER), "physics assembler");
        final PhysicsAssemblerBlockEntity assembler = plotBlockEntity(ship, plotAssembler, PhysicsAssemblerBlockEntity.class);
        helper.assertTrue(assembler != null, "the assembler's block entity moved into the ship");
        helper.assertTrue(assembler.getLastAssemblyException() == null, "assembly exception: " + assembler.getLastAssemblyException());

        helper.assertValueEqual(1, countWorldBlocks(helper, DECK_MIN, ASSEMBLER.offset(1, 0, 1)), "world blocks left in the hull region (only the loose block)");
        helper.assertBlockPresent(Blocks.STONE, LOOSE_BLOCK);
        helper.assertValueEqual(0, itemsInTest(helper).size(), "item entities dropped by the assembly");
        helper.assertValueEqual(gluesBefore - 1, gluesInTest(helper).size(), "glue entities left in the world (the deck's glue moves into the plot)");
        return ship;
    }

    /**
     * A glued deck with a physics assembler assembles into one ship when the assembler is used; the ship then falls one
     * block onto the floor and comes to rest there, still whole.
     */
    @GameTest(structure = "sable:assemblytest.allblocks", maxTicks = 200)
    public void assemblerBuildsShip(final GameTestHelper helper) {
        buildHull(helper);
        final SubLevelProbe[] probe = new SubLevelProbe[1];

        helper.startSequence()
                // let the assembler's block entity initialize, as it has when a player gets to it
                .thenExecuteAfter(1, () -> {
                    final ServerSubLevel ship = assembleHull(helper);
                    probe[0] = new SubLevelProbe(helper, ship).sampleEveryTick();
                    helper.assertTrue(Math.abs(probe[0].bottom() - 2.0) < 0.05, "assembled in place, bottom at y 2: " + probe[0]);
                })
                .thenExecuteAfter(5, () -> {
                    if (probe(helper, probe).sample().displacement().y() > -0.05) {
                        helper.fail("The assembled ship is not falling: " + probe[0]);
                    }
                })
                .thenWaitUntil(() -> {
                    probe(helper, probe).sample();
                    probe[0].assertResting(10);
                })
                .thenExecute(() -> {
                    if (Math.abs(probe(helper, probe).bottom() - 1.0) > 0.1) {
                        helper.fail("Came to rest with its bottom at y %.3f instead of on the floor (y 1): %s".formatted(probe[0].bottom(), probe[0]));
                    }
                    final double drift = Math.hypot(probe[0].displacement().x(), probe[0].displacement().z());
                    if (drift > 0.1) {
                        helper.fail("The ship drifted %.3f blocks sideways while falling: %s".formatted(drift, probe[0]));
                    }
                    helper.assertValueEqual(HULL_BLOCKS, countBlocks(probe[0].subLevel()), "blocks in the ship after landing");
                    helper.assertBlockPresent(Blocks.STONE, LOOSE_BLOCK);
                })
                .thenSucceed();
    }

    /**
     * Using the assembler of a ship that rests on the ground disassembles it: the ship aligns to the block grid and
     * its blocks are placed back into the world one block lower than they were built (where it landed).
     */
    @GameTest(structure = "sable:assemblytest.allblocks", maxTicks = 300)
    public void assemblerDisassemblesLandedShip(final GameTestHelper helper) {
        buildHull(helper);
        final SubLevelProbe[] probe = new SubLevelProbe[1];

        helper.startSequence()
                // sampled by the wait below only: a probe sampling every tick fails the test once the ship is removed
                .thenExecuteAfter(1, () -> probe[0] = new SubLevelProbe(helper, assembleHull(helper)))
                .thenWaitUntil(() -> {
                    probe(helper, probe).sample();
                    probe[0].assertResting(10);
                })
                .thenExecute(() -> {
                    final ServerSubLevel ship = probe(helper, probe).subLevel();
                    final BlockPos plotAssembler = findOneInPlot(helper, ship, state -> state.is(SimBlocks.PHYSICS_ASSEMBLER), "physics assembler");
                    useAssembler(helper, plotAssembler);
                    final PhysicsAssemblerBlockEntity assembler = plotBlockEntity(ship, plotAssembler, PhysicsAssemblerBlockEntity.class);
                    if (assembler != null && assembler.getLastAssemblyException() != null) {
                        helper.fail("Disassembly refused: %s (%s)".formatted(assembler.getLastAssemblyException().getMessage(), probe[0]));
                    }
                })
                // alignment takes a few ticks (the probe fails a test whose sub-level is removed, so stop sampling)
                .thenWaitUntil(() -> {
                    if (!probe[0].subLevel().isRemoved()) {
                        throw helper.assertionException(Component.literal("Ship not disassembled yet: " + describe(helper, probe[0].subLevel())));
                    }
                })
                .thenExecute(() -> {
                    final BlockPos landedMin = DECK_MIN.below();
                    final BlockPos landedMax = DECK_MAX.below();
                    for (final BlockPos pos : BlockPos.betweenClosed(landedMin, landedMax)) {
                        helper.assertBlockPresent(Blocks.OAK_PLANKS, pos);
                    }
                    helper.assertBlockPresent(SimBlocks.PHYSICS_ASSEMBLER, ASSEMBLER.below());
                    helper.assertBlockPresent(Blocks.STONE, LOOSE_BLOCK);
                    helper.assertValueEqual(HULL_BLOCKS + 1, countWorldBlocks(helper, landedMin, ASSEMBLER.offset(1, 0, 1)), "world blocks after disassembly");
                    helper.assertValueEqual(0, itemsInTest(helper).size(), "item entities dropped by the disassembly");
                    helper.assertTrue(!gluesInTest(helper).isEmpty(), "the deck's glue came back into the world");
                })
                .thenSucceed();
    }

    /**
     * Upstream's {@code torsionSpring} machine (creative motor -> clutch -> torsion spring -> shaft, lever on the
     * clutch) built on a glued deck and assembled into a ship: Create kinetics keep working in the ship's plot. The
     * spring winds to 90 degrees; pulling the lever (on the ship) disengages the clutch and it unwinds to 0.
     */
    @GameTest(structure = "sable:assemblytest.allblocks", maxTicks = 300)
    public void torsionSpringOnShip(final GameTestHelper helper) {
        // deck on the floor (local y 1), machine on top of it along +z (south) at x 2
        final BlockPos deckMin = new BlockPos(1, 1, 0);
        final BlockPos deckMax = new BlockPos(3, 1, 4);
        final BlockPos motor = new BlockPos(2, 2, 0);
        final BlockPos clutch = new BlockPos(2, 2, 1);
        final BlockPos spring = new BlockPos(2, 2, 2);
        final BlockPos shaft = new BlockPos(2, 2, 3);
        final BlockPos lever = new BlockPos(1, 2, 1);
        final BlockPos assemblerPos = new BlockPos(3, 2, 2);
        final Direction south = absoluteFacing(helper, Direction.SOUTH);
        final int machineBlocks = 15 + 4 + 1 + 1;

        fill(helper, deckMin, deckMax, Blocks.OAK_PLANKS.defaultBlockState());
        placeMotor(helper, motor, Direction.SOUTH, 32);
        helper.setBlock(clutch, AllBlocks.CLUTCH.defaultBlockState().setValue(BlockStateProperties.AXIS, south.getAxis()));
        helper.setBlock(spring, SimBlocks.TORSION_SPRING.defaultBlockState().setValue(BlockStateProperties.FACING, south));
        helper.setBlock(shaft, AllBlocks.SHAFT.defaultBlockState().setValue(BlockStateProperties.AXIS, south.getAxis()));
        helper.setBlock(lever, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR).setValue(LeverBlock.FACING, absoluteFacing(helper, Direction.NORTH)));
        placeAssembler(helper, assemblerPos);
        glue(helper, deckMin, new BlockPos(deckMax.getX(), 2, deckMax.getZ()));

        final ServerSubLevel[] ship = new ServerSubLevel[1];
        final BlockPos[] plotSpring = new BlockPos[1];

        helper.startSequence()
                .thenExecuteAfter(1, () -> {
                    ship[0] = assembleWithAssembler(helper, assemblerPos);
                    if (countBlocks(ship[0]) != machineBlocks) {
                        helper.fail("The ship holds %d blocks instead of %d: %s".formatted(countBlocks(ship[0]), machineBlocks, describePlot(ship[0])));
                    }
                    helper.assertValueEqual(0, countWorldBlocks(helper, deckMin, new BlockPos(3, 3, 4)), "world blocks left behind by the assembly");
                    plotSpring[0] = findOneInPlot(helper, ship[0], state -> state.is(SimBlocks.TORSION_SPRING), "torsion spring");
                })
                // the motor drives the spring through the clutch on the ship, and it winds to its 90 degree limit
                .thenWaitUntil(() -> {
                    final TorsionSpringBlockEntity be = springOn(helper, ship[0], plotSpring[0]);
                    if (Math.abs(Math.abs(be.getSpeed()) - 32) >= 1e-6 || Math.abs(be.getAngle()) != 90) {
                        throw helper.assertionException(Component.literal("Spring on the ship: speed %.2f (expected 32), angle %.1f (expected 90); %s"
                                .formatted(be.getSpeed(), be.getAngle(), describe(helper, ship[0]))));
                    }
                })
                .thenExecute(() -> {
                    final BlockPos plotLever = findOneInPlot(helper, ship[0], state -> state.is(Blocks.LEVER), "lever");
                    final BlockState leverState = ship[0].getLevel().getBlockState(plotLever);
                    ((LeverBlock) leverState.getBlock()).pull(leverState, ship[0].getLevel(), plotLever, null);
                })
                .thenWaitUntil(() -> {
                    final TorsionSpringBlockEntity be = springOn(helper, ship[0], plotSpring[0]);
                    if (be.getAngle() != 0) {
                        throw helper.assertionException(Component.literal("Spring on the ship did not unwind after the clutch disengaged: speed %.2f, angle %.1f"
                                .formatted(be.getSpeed(), be.getAngle())));
                    }
                })
                .thenExecute(() -> helper.assertValueEqual(machineBlocks, countBlocks(ship[0]), "blocks in the ship at the end"))
                .thenSucceed();
    }

    /**
     * @return the probe set by an earlier step. A failed step does not stop the steps queued after it in the same
     * tick, and anything but an assertion exception there crashes the GameTest server, hence no plain dereference.
     */
    private static SubLevelProbe probe(final GameTestHelper helper, final SubLevelProbe[] probe) {
        if (probe[0] == null) {
            throw helper.assertionException(Component.literal("No ship (assembly failed)"));
        }
        return probe[0];
    }

    private static TorsionSpringBlockEntity springOn(final GameTestHelper helper, final ServerSubLevel ship, final BlockPos plotPos) {
        if (ship == null || plotPos == null) {
            throw helper.assertionException(Component.literal("No ship (assembly failed)"));
        }
        final TorsionSpringBlockEntity be = plotBlockEntity(ship, plotPos, TorsionSpringBlockEntity.class);
        if (be == null) {
            throw helper.assertionException(Component.literal("No torsion spring block entity on the ship at " + plotPos.toShortString()));
        }
        return be;
    }
}
