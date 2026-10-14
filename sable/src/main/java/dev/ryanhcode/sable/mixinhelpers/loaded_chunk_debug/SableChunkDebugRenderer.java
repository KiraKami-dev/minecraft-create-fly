package dev.ryanhcode.sable.mixinhelpers.loaded_chunk_debug;

import dev.ryanhcode.sable.mixinterface.loaded_chunk_debug.DebugChunkProviderAttachments;
import dev.ryanhcode.sable.mixinterface.loaded_chunk_debug.DebugLevelChunkExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;

/**
 * Replaces the chunk border debug view with an outline of every loaded chunk, flashing red when a block update arrives.
 * <p>
 * On 1.21.11 debug renderers emit {@link Gizmos} in world space instead of drawing lines themselves.
 */
@ApiStatus.Internal
public class SableChunkDebugRenderer {

    public static void emitGizmos(final double camX, final double camY, final double camZ) {
        final long time = System.currentTimeMillis();

        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        final Entity entity = minecraft.gameRenderer.getMainCamera().entity();
        if (level == null || entity == null) {
            return;
        }

        final int minBuildHeight = level.getMinY();
        final int maxBuildHeight = level.getMaxY() + 1;

        final DebugChunkProviderAttachments attachments = (DebugChunkProviderAttachments) level.getChunkSource();
        for (final LevelChunk chunk : attachments.sable$loadedChunks()) {
            final ChunkPos pos = chunk.getPos();
            final float diff = Mth.clamp(time - ((DebugLevelChunkExtension) chunk).sable$getLastUpdate(), 0, 1000) / 1000.0F;
            final int color = ARGB.colorFromFloat(1.0F, 1.0F - diff, diff, 0.0F);

            final double x = pos.getMinBlockX();
            final double z = pos.getMinBlockZ();
            double y = minBuildHeight;
            if (camY > minBuildHeight) {
                y += (10 * ((1 - diff) / 100));
            } else {
                y -= (10 * ((1 - diff) / 100));
            }
            double y1 = maxBuildHeight;
            if (camY < maxBuildHeight) {
                y1 -= (10 * ((1 - diff) / 100));
            } else {
                y1 += (10 * ((1 - diff) / 100));
            }

            square(x, y, z, color);
            square(x, y1, z, color);
        }

        final ChunkPos ckPos = entity.chunkPosition();
        final double x = ckPos.x * 16;
        final double z = ckPos.z * 16;
        final int yellow = ARGB.colorFromFloat(1.0F, 1.0F, 1.0F, 0.0F);

        for (int xO = 0; xO < 2; xO++) {
            for (int zO = 0; zO < 2; zO++) {
                Gizmos.line(new Vec3(x + xO * 16, minBuildHeight, z + zO * 16), new Vec3(x + xO * 16, maxBuildHeight, z + zO * 16), yellow, 1.0F);
            }
        }

        final int blue = ARGB.colorFromFloat(1.0F, 0.0F, 0.0F, 1.0F);
        final int y = ((minBuildHeight / 16)) * 16;

        for (int yO = y; yO <= maxBuildHeight + 1; yO += 16) {
            square(x, yO, z, blue);
        }
    }

    private static void square(final double x, final double y, final double z, final int color) {
        final Vec3 a = new Vec3(x, y, z);
        final Vec3 b = new Vec3(x + 16, y, z);
        final Vec3 c = new Vec3(x + 16, y, z + 16);
        final Vec3 d = new Vec3(x, y, z + 16);
        Gizmos.line(a, b, color, 1.0F);
        Gizmos.line(b, c, color, 1.0F);
        Gizmos.line(c, d, color, 1.0F);
        Gizmos.line(d, a, color, 1.0F);
    }
}
