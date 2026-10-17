package dev.mcf.e2e;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.util.BoundedBitVolume3i;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import org.joml.Vector3d;

import java.util.List;
import java.util.Locale;

/**
 * Water occlusion scenario (-Pe2eScenario=water, client only): a pool with an open-fronted hull assembled into
 * it (the water flows into the vacated space, so world water overlaps the hull). Sable has no occlusion region
 * source of its own (Simulated's absorber is one), so this publishes the hull interior as a region for the
 * client to add. Screenshots: control without region, with region, camera straddling the volume's front
 * face (near-plane clipping), and camera inside the volume.
 */
final class WaterScenario {
    /** The client takes a screenshot with this name (then clears it). */
    static volatile String pendingShot;
    /** The client adds this plot-space volume to its water occlusion container (then clears it). */
    static volatile BoundedBitVolume3i pendingRegion;
    static volatile boolean done;

    private int ticks;
    private BoundedBitVolume3i interior;
    private final Vector3d frontCenter = new Vector3d();

    void tick(final MinecraftServer server) {
        if (done) {
            return;
        }
        this.ticks++;
        switch (this.ticks) {
            case 1 -> run(server, List.of(
                    "time set noon", "gamerule advance_time false", "gamerule spawn_mobs false",
                    "forceload add -16 0 15 31", "gamemode spectator @a", "tp @a 0 -57.12 9 0 15",
                    // pool: stone walls, 4 deep water
                    "fill -8 -60 6 8 -57 24 minecraft:stone", "fill -7 -60 7 7 -57 23 minecraft:water",
                    // hull: 5x6x5 brick box resting on the floor, interior x-1..1 y-59..-56 z14..16, front (z13) open
                    "fill -2 -60 13 2 -55 17 minecraft:stone_bricks", "fill -1 -59 13 1 -56 16 minecraft:air",
                    "sable assemble area -2 -60 13 2 -55 17"));
            case 40 -> {
                final SubLevel hull = hull(server);
                if (hull == null) {
                    E2E.LOG.error("E2E: FAIL water scenario: no sub-level after assemble");
                    this.finish();
                    return;
                }
                final BoundingBox3ic b = hull.getPlot().getBoundingBox();
                this.interior = new BoundedBitVolume3i(b.minX() + 1, b.minY() + 1, b.minZ() + 1, b.maxX() - 1, b.maxY() - 1, b.maxZ() - 1);
                for (int x = this.interior.getMinBlockPos().getX(); x <= this.interior.getMaxBlockPos().getX(); x++) {
                    for (int y = this.interior.getMinBlockPos().getY(); y <= this.interior.getMaxBlockPos().getY(); y++) {
                        for (int z = this.interior.getMinBlockPos().getZ(); z <= this.interior.getMaxBlockPos().getZ(); z++) {
                            this.interior.setOccupied(x, y, z, true);
                        }
                    }
                }
                final BlockPos min = this.interior.getMinBlockPos();
                final BlockPos max = this.interior.getMaxBlockPos();
                hull.logicalPose().transformPosition(
                        new Vector3d((min.getX() + max.getX() + 1) / 2.0, (min.getY() + max.getY() + 1) / 2.0, min.getZ()), this.frontCenter);
                E2E.LOG.info("E2E: water hull plot bounds {} {} {} .. {} {} {}, interior front face centre in the world at {}",
                        b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ(), this.frontCenter);
                pendingShot = "water-1-control-no-region.png";
            }
            case 50 -> pendingRegion = this.interior;
            case 70 -> pendingShot = "water-2-region-outside.png";
            case 75 -> this.teleportEye(server, this.frontCenter.z - 0.02, 5);
            case 95 -> pendingShot = "water-3-straddling-front-face.png";
            case 100 -> this.teleportEye(server, this.frontCenter.z + 1.5, 5);
            case 120 -> pendingShot = "water-4-inside.png";
            case 140 -> this.finish();
            default -> { }
        }
    }

    private static SubLevel hull(final MinecraftServer server) {
        final ServerSubLevelContainer container = SubLevelContainer.getContainer(server.overworld());
        return container == null || container.getAllSubLevels().isEmpty() ? null : container.getAllSubLevels().getFirst();
    }

    /** Puts the spectator's eye at the volume's centre height and the given z, looking along +z. */
    private void teleportEye(final MinecraftServer server, final double z, final float pitch) {
        run(server, List.of(String.format(Locale.ROOT, "tp @a %.4f %.4f %.4f 0 %.1f",
                this.frontCenter.x, this.frontCenter.y - 1.62, z, pitch)));
    }

    private void finish() {
        done = true;
    }

    private static void run(final MinecraftServer server, final List<String> commands) {
        for (final String command : commands) {
            E2E.LOG.info("E2E: running /{}", command);
            try {
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
            } catch (final Throwable t) {
                E2E.LOG.error("E2E: command FAILED /{}", command, t);
            }
        }
    }
}
