package dev.mcf.e2e.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.mcf.e2e.E2E;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Two fixes to the headless GameTest server for this suite:
 * <ul>
 *     <li>Lays the test grid out next to the world origin instead of at a random x/z within +-15M blocks
 *     ({@code startTests}). Sable's Rapier build simulates in f32, which only resolves whole metres that far out:
 *     sub-levels snap sideways and fall through the floor (upstream's gravity test FIXME is about this).</li>
 *     <li>Waits for the re-runs of multi-attempt tests ({@code maxAttempts > 1}) before stopping. Vanilla only
 *     tracks the first attempt of every test, so it halts while the re-runs are still queued and those tests never
 *     report a result.</li>
 * </ul>
 */
@Mixin(GameTestServer.class)
public abstract class GameTestServerMixin {
    /**
     * Stop waiting for re-runs after this many ticks, in case one never finishes (e.g. its structure failed to place).
     */
    @Unique
    private static final int MCF_E2E$MAX_RERUN_WAIT_TICKS = 20 * 60 * 5;

    @Unique
    private @Nullable GameTestRunner mcf_e2e$runner;

    @Unique
    private int mcf_e2e$firstAttemptsDoneTick = -1;

    @ModifyVariable(method = "startTests", at = @At("STORE"), ordinal = 0)
    private BlockPos mcf_e2e$startNearOrigin(final BlockPos randomStart) {
        return new BlockPos(0, randomStart.getY(), 0);
    }

    @ModifyVariable(method = "startTests", at = @At("STORE"))
    private GameTestRunner mcf_e2e$captureRunner(final GameTestRunner runner) {
        this.mcf_e2e$runner = runner;
        return runner;
    }

    @ModifyExpressionValue(method = "tickServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/gametest/framework/MultipleTestTracker;isDone()Z"))
    private boolean mcf_e2e$waitForReruns(final boolean firstAttemptsDone) {
        if (!firstAttemptsDone || this.mcf_e2e$runner == null) {
            return firstAttemptsDone;
        }

        // the runner's list includes the re-runs scheduled so far
        if (this.mcf_e2e$runner.getTestInfos().stream().allMatch(GameTestInfo::isDone)) {
            return true;
        }

        final int tick = ((MinecraftServer) (Object) this).getTickCount();
        if (this.mcf_e2e$firstAttemptsDoneTick < 0) {
            this.mcf_e2e$firstAttemptsDoneTick = tick;
        }
        if (tick - this.mcf_e2e$firstAttemptsDoneTick > MCF_E2E$MAX_RERUN_WAIT_TICKS) {
            E2E.LOG.error("E2E: gave up waiting for re-runs of multi-attempt tests");
            return true;
        }
        return false;
    }
}
