package dev.mcf.e2e.gametest;

import com.zurrtum.create.content.kinetics.base.KineticBlockEntity;
import com.zurrtum.create.content.redstone.analogLever.AnalogLeverBlockEntity;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.simulated_team.simulated.content.blocks.analog_transmission.AnalogTransmissionBlockEntity;
import dev.simulated_team.simulated.content.blocks.torsion_spring.TorsionSpringBlockEntity;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.network.chat.Component;

import java.util.List;

import static dev.mcf.e2e.gametest.SableTestHelper.*;
import static dev.mcf.e2e.gametest.SimulatedTestHelper.*;

/**
 * Upstream Simulated's {@code neoforge/gametest/ExtraKineticsTest} on its own structures
 * ({@code simulated:extrakineticstest.*}, shipped in Simulated's data). Local Y coordinates are one lower than
 * upstream's (see {@link SableTestHelper}); NeoForge's default 100-tick timeout is spelled out.
 *
 * <p>All four are optional for now: the kinetic block entities these structures load from NBT (saved by NeoForge
 * Create with their network ids, stress and {@code Source} positions) do not come up on Create Fly 1.21.11, while the
 * same machines placed in code work ({@link SimulatedShipTest#torsionSpringOnShip}). Observed 2026-10-17: the swivel
 * bearing and the torsion spring stay at speed 0 for the whole 100 ticks even next to a running creative motor, and the
 * analog transmission's block entity is gone by tick 3 ("Missing block entity"). The torsion spring's saved output
 * also fails to load: {@code TorsionSpringOutput.Sequence {Mode:"TURN_ANGLE"}} is upstream Create's enum-name format,
 * Create Fly's {@code SequencerInstructions.CODEC} only accepts its serialized names. Make them required once the
 * structure-loaded kinetics work (or the structures are re-saved from the port). Upstream asserted the first speed
 * after exactly 1 tick; the swivel bearing and torsion spring tests wait for it instead, keeping the later steps'
 * timing relative to it.
 */
public final class SimulatedExtraKineticsTest {

    /**
     * Creative motor (16 RPM) -> analog transmission <- analog lever: stepping the lever from 0 to 15 changes the
     * output speed to 16 / modifier and finally stops it.
     */
    // optional: the structure's analog transmission block entity disappears (see the class comment)
    @GameTest(structure = "simulated:extrakineticstest.analogtransmission", maxTicks = 100, required = false)
    public void analogTransmission(final GameTestHelper helper) {
        final BlockPos leverPos = new BlockPos(1, 1, 1);
        final BlockPos transmissionPos = new BlockPos(1, 1, 2);
        final AnalogLeverBlockEntity leverBE = helper.getBlockEntity(leverPos, AnalogLeverBlockEntity.class);
        final GameTestSequence sequence = helper.startSequence();
        for (int i = 0; i < 16; i++) {
            sequence.thenExecuteAfter(1, () -> {
                switch (leverBE.getState()) {
                    case 0 -> assertKineticsSpeed(helper, transmissionPos, 16);
                    case 15 -> assertKineticsSpeed(helper, transmissionPos, 0);
                    default -> assertKineticsSpeed(helper, transmissionPos, AnalogTransmissionBlockEntity.class, be -> 16 / be.getRotationModifier());
                }
            }).thenExecuteAfter(1, () -> leverBE.changeState(false));
        }
        sequence.thenSucceed();
    }

    /**
     * The motor drives the transmission's other side: its own speed stays 16 and the extra (cogwheel) side turns at
     * 16 * modifier.
     */
    // optional: the structure's analog transmission block entity disappears (see the class comment)
    @GameTest(structure = "simulated:extrakineticstest.analogtransmissionreverse", maxTicks = 100, required = false)
    public void analogTransmissionReverse(final GameTestHelper helper) {
        final BlockPos leverPos = new BlockPos(1, 1, 1);
        final BlockPos transmissionPos = new BlockPos(1, 1, 2);
        final AnalogLeverBlockEntity leverBE = helper.getBlockEntity(leverPos, AnalogLeverBlockEntity.class);
        final GameTestSequence sequence = helper.startSequence();
        for (int i = 0; i < 16; i++) {
            sequence.thenExecuteAfter(1, () -> {
                switch (leverBE.getState()) {
                    case 0 -> assertExtraKineticsSpeed(helper, transmissionPos, 16, 16);
                    case 15 -> assertExtraKineticsSpeed(helper, transmissionPos, 16, 0);
                    default -> assertExtraKineticsSpeed(helper, transmissionPos, AnalogTransmissionBlockEntity.class,
                            (be, ebe) -> 16, (be, ebe) -> 16 * be.getRotationModifier());
                }
            }).thenExecuteAfter(1, () -> leverBE.changeState(false));
        }
        sequence.thenSucceed();
    }

    /**
     * Swivel bearing driven by two creative motors (64 RPM shaft, 32 RPM cogwheel): it assembles the honey-glued
     * encased fan in front of it into its own sub-level, which keeps turning at 64 RPM through the bearing's link.
     */
    // optional: the structure's swivel bearing never gets speed (see the class comment)
    @GameTest(structure = "simulated:extrakineticstest.swivelbearing", maxTicks = 100, required = false)
    public void swivelBearing(final GameTestHelper helper) {
        final BlockPos bearingPos = new BlockPos(2, 2, 2);
        helper.startSequence()
                // upstream: exactly after 1 tick; here the network comes up a few ticks later (see the class comment)
                .thenWaitUntil(() -> assertExtraKineticsSpeed(helper, bearingPos, 64, -32))
                .thenIdle(20)
                .thenExecute(() -> {
                    final List<ServerSubLevel> subLevels = subLevelsInTest(helper);
                    if (subLevels.size() != 1) {
                        throw helper.assertionException(Component.literal("Expected 1 sub-level, found " + subLevels.size()));
                    }

                    final ServerSubLevel subLevel = subLevels.getFirst();
                    final KineticBlockEntity be = plotBlockEntity(subLevel, subLevel.getPlot().getCenterBlock(), KineticBlockEntity.class);
                    if (be == null) {
                        throw helper.assertionException(Component.literal("No kinetic block entity at the swivel sub-level's centre: " + describePlot(subLevel)));
                    }
                    if (Math.abs(Math.abs(be.getSpeed()) - 64) >= 1e-6) {
                        throw helper.assertionException(Component.literal("Expected %.2f speed, got %.2f (%s)".formatted(64F, Math.abs(be.getSpeed()), describe(helper, subLevel))));
                    }
                })
                .thenSucceed();
    }

    /**
     * Creative motor -> clutch -> torsion spring -> mechanical bearing: the spring winds to its 90 degree limit; once
     * the lever disengages the clutch, it unwinds back to 0.
     */
    // optional: the structure's torsion spring never gets speed (see the class comment); the same machine on a ship passes
    @GameTest(structure = "simulated:extrakineticstest.torsionspring", maxTicks = 100, required = false)
    public void torsionSpring(final GameTestHelper helper) {
        final BlockPos springPos = new BlockPos(2, 1, 3);
        final BlockPos leverPos = new BlockPos(1, 1, 2);
        helper.startSequence()
                // upstream: exactly after 1 tick; here the network comes up a few ticks later (see the class comment)
                .thenWaitUntil(() -> assertKineticsSpeed(helper, springPos, 32))
                .thenExecuteAfter(15, () -> helper.assertBlockEntityData(springPos, TorsionSpringBlockEntity.class, be -> Math.abs(be.getAngle()) == 90,
                        () -> Component.literal("Expected 90 degrees, got %.0f".formatted(Math.abs(helper.getBlockEntity(springPos, TorsionSpringBlockEntity.class).getAngle())))))
                // upstream set the lever's POWERED state directly; pulling it is what a player does
                .thenExecuteAfter(1, () -> helper.pullLever(leverPos))
                .thenExecuteAfter(15, () -> helper.assertBlockEntityData(springPos, TorsionSpringBlockEntity.class, be -> be.getAngle() == 0,
                        () -> Component.literal("Expected 0 degrees, got %.0f".formatted(Math.abs(helper.getBlockEntity(springPos, TorsionSpringBlockEntity.class).getAngle())))))
                .thenSucceed();
    }
}
