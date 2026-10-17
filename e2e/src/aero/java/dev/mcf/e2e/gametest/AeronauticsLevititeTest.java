package dev.mcf.e2e.gametest;

import dev.eriksonn.aeronautics.index.AeroBlocks;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;

import static dev.mcf.e2e.gametest.SableTestHelper.*;

/**
 * Levitite (ROADMAP T2, Aeronautics): a levitite block is Sable floating material {@code aeronautics:levitite}
 * ({@code lift_strength} 10, {@code prevent_self_lift}): it cancels gravity for up to 10 mass units per block, never
 * more than gravity, and adds vertical friction. Built only with {@code -Pe2eAero} (see e2e/build.gradle).
 *
 * <p>Not run yet: Aeronautics does not compile on 1.21.11 so far. Optional until they have passed once; the mass
 * figures assume Sable's default block mass of about 1.
 */
public final class AeronauticsLevititeTest {

    /**
     * Spawns a raft of {@code layers} 3x1x3 plank layers with one levitite block in the centre of the bottom layer,
     * its centre of mass at local {@code center}.
     */
    private static ServerSubLevel spawnRaft(final GameTestHelper helper, final Vector3d center, final int layers) {
        final BlockState planks = Blocks.OAK_PLANKS.defaultBlockState();
        final ServerSubLevel raft = spawnSubLevel(container(helper), absolutePosition(helper, center), accessor -> {
            for (final BlockPos pos : BlockPos.betweenClosed(-1, 0, -1, 1, layers - 1, 1)) {
                accessor.setBlock(pos, pos.getX() == 0 && pos.getY() == 0 && pos.getZ() == 0 ? AeroBlocks.LEVITITE.defaultBlockState() : planks, 3);
            }
        });
        teleport(helper, raft, absolutePosition(helper, center), testOrientation(helper));
        return raft;
    }

    /**
     * 1 levitite + 8 planks (mass about 9, lift capacity 10): gravity is fully cancelled, the raft hovers where it was
     * spawned.
     */
    @GameTest(structure = "sable:physicstest.gravity", maxTicks = 200, required = false)
    public void levititeRaftHovers(final GameTestHelper helper) {
        final ServerSubLevel raft = spawnRaft(helper, new Vector3d(2.5, 6.5, 2.5), 1);
        final SubLevelProbe probe = new SubLevelProbe(helper, raft).sampleEveryTick();

        helper.startSequence()
                .thenIdle(100)
                .thenExecute(() -> {
                    if (Math.abs(probe.displacement().y()) > 0.25 || Math.abs(probe.velocity().y()) > 0.05) {
                        helper.fail("The levitite raft did not hover: " + probe);
                    }
                    final double drift = Math.hypot(probe.displacement().x(), probe.displacement().z());
                    if (drift > 0.1) {
                        helper.fail("The hovering raft drifted %.3f blocks sideways: %s".formatted(drift, probe));
                    }
                })
                .thenSucceed();
    }

    /**
     * Control: 1 levitite + 17 planks (mass about 18 > 10): it sinks, at roughly 11 * (1 - 10 / 18) m/s^2 less the
     * levitite's vertical friction, so clearly slower than free fall.
     */
    @GameTest(structure = "sable:physicstest.gravity", maxTicks = 200, required = false)
    public void overloadedLevititeRaftSinks(final GameTestHelper helper) {
        final ServerSubLevel raft = spawnRaft(helper, new Vector3d(2.5, 9.0, 2.5), 2);
        final SubLevelProbe probe = new SubLevelProbe(helper, raft).sampleEveryTick();

        helper.startSequence()
                .thenIdle(20)
                .thenExecute(() -> {
                    // free fall with Sable's default drag drops 5.34 blocks in 1 s (SablePhysicsTest.freeFallWithDrag)
                    final double drop = -probe.displacement().y();
                    if (drop < 0.05) {
                        helper.fail("The overloaded levitite raft does not sink: " + probe);
                    }
                    if (drop > 0.8 * 5.34) {
                        helper.fail("The overloaded raft falls like a stone (%.3f blocks in 1 s): levitite gives no lift: %s".formatted(drop, probe));
                    }
                })
                .thenSucceed();
    }
}
