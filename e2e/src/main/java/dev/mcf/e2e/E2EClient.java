package dev.mcf.e2e;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

/**
 * Once in the world, takes screenshots at fixed intervals (the server side spawns the
 * sub-levels in front of the camera), then closes the game.
 */
public final class E2EClient implements ClientModInitializer {
    private static final int[] SHOT_TICKS = {80, 140, 220, 320, 440};
    private static final int STOP_TICK = 480;
    // flight scene (E2EFlight): the server builds the ship about 40 ticks in, assembles at +20, starts the motor at +40
    private static final int[] FLIGHT_SHOT_TICKS = {75, 100, 125, 145, 165, 190, 220, 260};
    private static final int FLIGHT_STOP_TICK = 290;
    private final boolean flight = "flight".equals(System.getProperty("e2e.scene"));

    private int inWorldTicks;
    private int doneTicks;

    @Override
    public void onInitializeClient() {
        // e2e.play: same scene, but the game stays open for manual testing
        if (!Boolean.getBoolean("e2e.enabled") || Boolean.getBoolean("e2e.play")) {
            return;
        }
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(final Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        this.inWorldTicks++;

        // playtest scenario: the server script requests screenshots and says when it is finished
        if ("playtest".equals(System.getProperty("e2e.scenario"))) {
            // a pause screen (opened when the window loses focus) would freeze the integrated server
            if (minecraft.screen != null) {
                E2E.LOG.info("E2E: closing screen {}", minecraft.screen.getClass().getSimpleName());
                minecraft.setScreen(null);
            }
            final String shot = Playtest.pendingShot;
            if (shot != null) {
                Playtest.pendingShot = null;
                Screenshot.grab(minecraft.gameDirectory, shot, minecraft.getMainRenderTarget(), 1,
                        message -> E2E.LOG.info("E2E: screenshot {}", shot));
            }
            if (Playtest.done && ++this.doneTicks == 20) {
                E2E.LOG.info("E2E: done, closing client");
                minecraft.stop();
            }
            return;
        }

        for (final int shot : this.flight ? FLIGHT_SHOT_TICKS : SHOT_TICKS) {
            if (this.inWorldTicks == shot) {
                final String name = (this.flight ? "flight-" : "e2e-") + shot + ".png";
                Screenshot.grab(minecraft.gameDirectory, name, minecraft.getMainRenderTarget(), 1,
                        message -> E2E.LOG.info("E2E: screenshot {}", name));
            }
        }
        if (this.inWorldTicks == (this.flight ? FLIGHT_STOP_TICK : STOP_TICK)) {
            E2E.LOG.info("E2E: done, closing client");
            minecraft.stop();
        }
    }
}
