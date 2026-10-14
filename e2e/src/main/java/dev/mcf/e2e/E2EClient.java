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

    private int inWorldTicks;

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

        for (final int shot : SHOT_TICKS) {
            if (this.inWorldTicks == shot) {
                final String name = "e2e-" + shot + ".png";
                Screenshot.grab(minecraft.gameDirectory, name, minecraft.getMainRenderTarget(), 1,
                        message -> E2E.LOG.info("E2E: screenshot {}", name));
            }
        }
        if (this.inWorldTicks == STOP_TICK) {
            E2E.LOG.info("E2E: done, closing client");
            minecraft.stop();
        }
    }
}
