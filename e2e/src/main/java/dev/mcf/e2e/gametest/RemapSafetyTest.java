package dev.mcf.e2e.gametest;

import dev.mcf.e2e.E2E;
import dev.mcf.e2e.RemapSafety;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Required: no class of the ported mods (or Create class they mix into) leaves an abstract mod-interface method to be
 * implemented by a Minecraft method, which is renamed in the intermediary-named release jars and then throws
 * {@link AbstractMethodError} (the hot air burner {@code getLevel()} crash). See {@link RemapSafety}.
 */
public final class RemapSafetyTest {

    @GameTest(maxTicks = 20)
    public void modInterfacesSurviveRemapping(final GameTestHelper helper) {
        final RemapSafety.Result result = RemapSafety.run();
        E2E.LOG.info("E2E: remap-safety checked {} classes ({} skipped)", result.checked(), result.skipped());
        if (!result.failures().isEmpty()) {
            helper.fail(result.failures().size() + " mod-interface method(s) rely on Minecraft names: " + String.join("; ", result.failures()));
        }
        helper.succeed();
    }
}
