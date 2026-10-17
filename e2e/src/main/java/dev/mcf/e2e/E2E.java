package dev.mcf.e2e;

import dev.mcf.e2e.gametest.E2ETestReporter;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.MixinEnvironment;

/**
 * Applies every mixin up front (so broken injections fail loudly at start-up instead of
 * silently later), then runs the configured commands and, on a dedicated server, shuts down.
 * On the GameTest server (e2e.gametest) it only installs the test reporter; the tests run by themselves.
 * Log lines are prefixed "E2E:" so tools/e2e.sh can grep them.
 */
public final class E2E implements ModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("e2e");
    private static final int DEDICATED_COMMAND_TICK = 100;
    private static final int DEDICATED_STOP_TICK = 700;

    private int ticks;
    private int commandTick = -1;

    @Override
    public void onInitialize() {
        final boolean scene = Boolean.getBoolean("e2e.enabled");
        final boolean gameTests = Boolean.getBoolean("e2e.gametest");
        if (!scene && !gameTests) {
            return;
        }

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                MixinEnvironment.getCurrentEnvironment().audit();
                LOG.info("E2E: mixin audit passed");
            } catch (final Throwable t) {
                LOG.error("E2E: mixin audit FAILED", t);
            }
        });

        if (gameTests) {
            E2ETestReporter.install(System.getProperty("e2e.gametest.report"));
            return;
        }

        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    private void tick(final MinecraftServer server) {
        this.ticks++;

        if (this.commandTick < 0) {
            final boolean ready = server.isDedicatedServer()
                    ? this.ticks >= DEDICATED_COMMAND_TICK
                    : server.getPlayerList().getPlayerCount() > 0 && this.ticks >= 40;
            if (ready) {
                this.commandTick = this.ticks;
                runCommands(server);
            }
            return;
        }

        final int since = this.ticks - this.commandTick;
        if (since % 100 == 0) {
            // sub-levels in chunks that aren't ticking are unloaded into holding chunks (ROADMAP S8a)
            final SubLevelContainer container = SubLevelContainer.getContainer(server.overworld());
            LOG.info("E2E: {} ticks after spawn, server alive, {} sub-levels loaded", since, container == null ? 0 : container.getLoadedCount());
        }
        if (server.isDedicatedServer() && this.ticks >= DEDICATED_STOP_TICK) {
            LOG.info("E2E: done, stopping server");
            server.halt(false);
        }
    }

    private static void runCommands(final MinecraftServer server) {
        for (final String command : System.getProperty("e2e.commands", "").split(";")) {
            if (command.isBlank()) {
                continue;
            }
            LOG.info("E2E: running /{}", command);
            try {
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command.trim());
            } catch (final Throwable t) {
                LOG.error("E2E: command FAILED /{}", command, t);
            }
        }
    }
}
