package dev.mcf.e2e;

import dev.mcf.e2e.gametest.E2ETestReporter;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
    private final E2EFlight flight = E2EFlight.enabled() ? new E2EFlight() : null;
    private final Map<UUID, Vector3dc> spawnPositions = new HashMap<>();
    private final Playtest playtest = "playtest".equals(System.getProperty("e2e.scenario")) ? new Playtest() : null;

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

        if (this.playtest != null) {
            if (!server.getPlayerList().getPlayers().isEmpty() && this.ticks >= 40) {
                this.playtest.tick(server);
            }
            return;
        }

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
        if (this.flight != null) {
            this.flight.tick(server.overworld(), since);
        }
        if (since == 1 || since % 100 == 0) {
            if (since > 1) {
                LOG.info("E2E: {} ticks after spawn, server alive", since);
            }
            logSubLevels(server, since);
        }
        if (server.isDedicatedServer() && this.ticks >= DEDICATED_STOP_TICK) {
            LOG.info("E2E: done, stopping server");
            server.halt(false);
        }
    }

    /** Shows that sub-levels stay loaded and simulate (positions change after spawning). */
    private void logSubLevels(final MinecraftServer server, final int since) {
        final ServerSubLevelContainer container = SubLevelContainer.getContainer(server.overworld());
        if (container == null) {
            LOG.error("E2E: no sub-level container in the overworld");
            return;
        }
        int moved = 0;
        for (final SubLevel subLevel : container.getAllSubLevels()) {
            final Vector3dc pos = subLevel.logicalPose().position();
            final Vector3dc start = this.spawnPositions.putIfAbsent(subLevel.getUniqueId(), new Vector3d(pos));
            if (start != null && start.distance(pos) > 0.1) {
                moved++;
            }
        }
        LOG.info("E2E: {} ticks: {} sub-levels loaded, {} moved since spawn", since, container.getLoadedCount(), moved);
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
