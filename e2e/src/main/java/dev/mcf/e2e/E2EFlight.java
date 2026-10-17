package dev.mcf.e2e;

import com.zurrtum.create.AllBlocks;
import com.zurrtum.create.content.contraptions.glue.SuperGlueEntity;
import com.zurrtum.create.content.kinetics.motor.CreativeMotorBlockEntity;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerBlockEntity;
import dev.simulated_team.simulated.index.SimBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Set;

import static dev.mcf.e2e.gametest.SimulatedTestHelper.findInPlot;
import static dev.mcf.e2e.gametest.SimulatedTestHelper.plotBlockEntity;

/**
 * The {@code flight} client scene ({@code -De2e.scene=flight}): the propeller-bearing airship of
 * {@code AeronauticsPropellerTest} built 9 blocks in front of the camera, assembled with its physics assembler and
 * started at 256 RPM a second later (reversed if it has not climbed after 3 s). Server side; {@link E2EClient} takes the
 * screenshots. Aeronautics blocks are looked up by id so this compiles without the aero source set.
 */
final class E2EFlight {
    private static final int RPM = 256;
    private static final BlockPos DECK_MIN = new BlockPos(-1, -60, 8);
    private static final BlockPos MOTOR = new BlockPos(0, -60, 9);
    private static final BlockPos ASSEMBLER = new BlockPos(-1, -59, 8);

    private SubLevel ship;
    private BlockPos plotMotor;
    private double startY;
    private boolean reversed;

    static boolean enabled() {
        return "flight".equals(System.getProperty("e2e.scene"));
    }

    void tick(final ServerLevel level, final int since) {
        try {
            if (since == 1) {
                this.build(level);
            } else if (since == 20) {
                this.assemble(level);
            } else if (since == 40) {
                this.setMotor(RPM);
            } else if (since == 100 && this.ship != null && this.y() - this.startY < 0.5) {
                this.reversed = true;
                this.setMotor(-RPM);
            }
            if (this.ship != null && since % 10 == 0) {
                E2E.LOG.info("E2E: flight t={} ship y={} (start {}, motor {} RPM, {} blocks in plot)", since, String.format("%.2f", this.y()),
                        String.format("%.2f", this.startY), since < 40 ? 0 : this.reversed ? -RPM : RPM, findInPlot(this.ship, s -> !s.isAir()).size());
            }
        } catch (final Throwable t) {
            E2E.LOG.error("E2E: flight scene FAILED at t={}", since, t);
        }
    }

    private double y() {
        return this.ship.logicalPose().position().y();
    }

    private static net.minecraft.world.level.block.Block aero(final String id) {
        return BuiltInRegistries.BLOCK.getValue(Identifier.parse("aeronautics:" + id));
    }

    private void build(final ServerLevel level) {
        for (final BlockPos pos : BlockPos.betweenClosed(DECK_MIN, DECK_MIN.offset(2, 0, 2))) {
            level.setBlockAndUpdate(pos, Blocks.OAK_PLANKS.defaultBlockState());
        }
        level.setBlockAndUpdate(MOTOR, AllBlocks.CREATIVE_MOTOR.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP));
        level.setBlockAndUpdate(MOTOR.above(), aero("propeller_bearing").defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP));
        for (final BlockPos pos : BlockPos.betweenClosed(DECK_MIN.above(2), DECK_MIN.offset(2, 2, 2))) {
            level.setBlockAndUpdate(pos, AllBlocks.SAIL.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP));
        }
        level.setBlockAndUpdate(MOTOR.above(2), Blocks.OAK_PLANKS.defaultBlockState());
        level.setBlockAndUpdate(ASSEMBLER, SimBlocks.PHYSICS_ASSEMBLER.defaultBlockState()
                .setValue(FaceAttachedHorizontalDirectionalBlock.FACE, AttachFace.FLOOR)
                .setValue(FaceAttachedHorizontalDirectionalBlock.FACING, Direction.NORTH));
        // a hot air burner and a levitite block on the deck corners, so their visuals fly along
        level.setBlockAndUpdate(DECK_MIN.offset(2, 1, 2), aero("adjustable_burner").defaultBlockState());
        level.setBlockAndUpdate(DECK_MIN.offset(2, 1, 0), aero("levitite").defaultBlockState());
        level.addFreshEntity(new SuperGlueEntity(level, new AABB(DECK_MIN.getX(), DECK_MIN.getY(), DECK_MIN.getZ(),
                DECK_MIN.getX() + 3, DECK_MIN.getY() + 2, DECK_MIN.getZ() + 3)));
        level.addFreshEntity(new SuperGlueEntity(level, new AABB(DECK_MIN.getX(), DECK_MIN.getY() + 2, DECK_MIN.getZ(),
                DECK_MIN.getX() + 3, DECK_MIN.getY() + 3, DECK_MIN.getZ() + 3)));
        E2E.LOG.info("E2E: flight ship built");
    }

    private void assemble(final ServerLevel level) {
        final SubLevelContainer container = SubLevelContainer.getContainer(level);
        final Set<SubLevel> before = new HashSet<>(container.getAllSubLevels());
        if (!(level.getBlockEntity(ASSEMBLER) instanceof final PhysicsAssemblerBlockEntity assembler)) {
            E2E.LOG.error("E2E: flight scene FAILED: no physics assembler");
            return;
        }
        assembler.assembleOrDisassemble();
        for (final SubLevel subLevel : container.getAllSubLevels()) {
            if (!before.contains(subLevel)) {
                this.ship = subLevel;
            }
        }
        if (this.ship == null) {
            E2E.LOG.error("E2E: flight scene FAILED: assembly made no ship ({})", assembler.getLastAssemblyException());
            return;
        }
        this.startY = this.y();
        this.plotMotor = findInPlot(this.ship, s -> s.is(AllBlocks.CREATIVE_MOTOR)).getFirst();
        E2E.LOG.info("E2E: flight ship assembled");
    }

    private void setMotor(final int rpm) {
        if (this.ship == null) {
            return;
        }
        final CreativeMotorBlockEntity motor = plotBlockEntity(this.ship, this.plotMotor, CreativeMotorBlockEntity.class);
        if (motor != null) {
            motor.generatedSpeed.setValue(rpm);
            E2E.LOG.info("E2E: flight motor set to {} RPM", rpm);
        }
    }
}
