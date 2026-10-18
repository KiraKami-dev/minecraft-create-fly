package dev.mcf.e2e.gametest;

import dev.mcf.e2e.E2E;
import dev.mcf.e2e.ServerClassSafety;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Required: every Block, Item, BlockEntity, Entity and menu class of the ported mods survives
 * getDeclaredMethods()/getDeclaredFields() on a dedicated server (where client classes are stripped), as Lithium
 * and similar mods do at start-up.
 */
public final class ServerClassSafetyTest {

    @GameTest(maxTicks = 20)
    public void reflectionIsServerSafe(final GameTestHelper helper) {
        final ServerClassSafety.Result result = ServerClassSafety.run();
        E2E.LOG.info("E2E: server-class-safety checked {} classes ({} client-only skipped)", result.checked(), result.skippedClientOnly());
        if (!result.failures().isEmpty()) {
            helper.fail(result.failures().size() + " class(es) reference client-only types: " + String.join("; ", result.failures()));
        }
        helper.succeed();
    }
}
