package dev.mcf.e2e;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import org.joml.Vector3d;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Scripted player-facing physics scenario (-Pe2eScenario=playtest, client only): assemble blocks into
 * a sub-level, let it fall and land, stack another on it, stand a player on a platform and push,
 * rotate and move the platform. Logs "E2E: PASS|FAIL ..." lines; sets {@link #done} at the end.
 */
final class Playtest {
    private static final double GROUND_TOP = -60.0;
    private static final double EPS = 0.2;
    private static final int STEP_TIMEOUT = 300;
    private static final int SETTLE_TICKS = 15;

    /** The client takes a screenshot with this name (then clears it). */
    static volatile String pendingShot;
    static volatile boolean done;

    private int step;
    private int stepTicks;
    private int settled;
    private final Vector3d lastPos = new Vector3d();
    private final Vector3d startPos = new Vector3d();
    private final Vector3d playerStart = new Vector3d();
    private final Set<UUID> known = new HashSet<>();
    private ServerSubLevel cubeA;
    private ServerSubLevel cubeB;
    private ServerSubLevel platform;
    private double cubeAMinY;
    private double cubeAMaxY;
    private double rotatePivotAngle;
    private int fails;

    void tick(final MinecraftServer server) {
        if (done) {
            return;
        }
        final ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        this.stepTicks++;
        if (this.stepTicks > STEP_TIMEOUT) {
            this.check("step " + this.step + " finished in time", false, "timed out");
            this.finish();
            return;
        }

        switch (this.step) {
            case 0 -> { // setup
                if (this.stepTicks == 1) {
                    run(server, "time set noon", "gamerule advance_time false", "gamerule spawn_mobs false",
                            "gamemode survival @a", "forceload add -16 0 15 31", "tp @a 0 -60 0 0 15");
                }
                if (this.stepTicks == 30) {
                    this.next();
                }
            }
            case 1 -> { // assemble a stone cube floating in the air
                this.markKnown(server);
                run(server, "fill 0 -50 10 2 -48 12 minecraft:stone", "sable assemble area 0 -50 10 2 -48 12");
                final ServerSubLevel created = this.newSubLevel(server);
                this.check("assemble creates one new sub-level", created != null, "");
                this.check("assemble removes the world blocks",
                        server.overworld().getBlockState(new BlockPos(1, -49, 11)).isAir(), "");
                if (created == null) {
                    this.finish();
                    return;
                }
                this.cubeA = created;
                this.check("assembled sub-level has mass", created.getMassTracker().getMass() > 0,
                        "mass=" + created.getMassTracker().getMass());
                this.startPos.set(created.logicalPose().position());
                this.lastPos.set(this.startPos);
                this.settled = 0;
                this.next();
            }
            case 2 -> { // falls and comes to rest on the ground
                if (this.settle(this.cubeA)) {
                    final BoundingBox3dc bb = this.cubeA.boundingBox();
                    final double fell = this.startPos.y - this.cubeA.logicalPose().position().y;
                    this.check("assembled sub-level falls", fell > 3, fmt("fell=%.2f", fell));
                    this.check("assembled sub-level rests on the ground (bb min y == ground top)",
                            Math.abs(bb.minY() - GROUND_TOP) < EPS, fmt("minY=%.3f expected=%.1f", bb.minY(), GROUND_TOP));
                    this.cubeAMinY = bb.minY();
                    this.cubeAMaxY = bb.maxY();
                    this.next();
                }
            }
            case 3 -> { // second cube assembled where the first one started, lands on the first
                this.markKnown(server);
                run(server, "fill 0 -50 10 2 -48 12 minecraft:stone", "sable assemble area 0 -50 10 2 -48 12");
                this.cubeB = this.newSubLevel(server);
                if (this.cubeB == null) {
                    this.check("second cube assembled", false, "");
                    this.finish();
                    return;
                }
                this.lastPos.set(this.cubeB.logicalPose().position());
                this.settled = 0;
                this.next();
            }
            case 4 -> {
                if (this.settle(this.cubeB)) {
                    final BoundingBox3dc bb = this.cubeB.boundingBox();
                    this.check("falling sub-level rests on another sub-level (min y == lower max y)",
                            Math.abs(bb.minY() - this.cubeAMaxY) < EPS, fmt("minY=%.3f expected=%.3f", bb.minY(), this.cubeAMaxY));
                    this.check("lower sub-level was not pushed into the ground",
                            Math.abs(this.cubeA.boundingBox().minY() - this.cubeAMinY) < EPS,
                            fmt("minY=%.3f was %.3f", this.cubeA.boundingBox().minY(), this.cubeAMinY));
                    this.next();
                }
            }
            case 5 -> { // platform dropped from above
                this.markKnown(server);
                run(server, "execute positioned 8 -52 20 run sable spawn platform 3 minecraft:oak_planks");
                this.platform = this.newSubLevel(server);
                if (this.platform == null) {
                    this.check("platform spawned", false, "");
                    this.finish();
                    return;
                }
                this.lastPos.set(this.platform.logicalPose().position());
                this.settled = 0;
                this.next();
            }
            case 6 -> {
                if (this.settle(this.platform)) {
                    final BoundingBox3dc bb = this.platform.boundingBox();
                    this.check("platform rests on the ground", Math.abs(bb.minY() - GROUND_TOP) < EPS, fmt("minY=%.3f", bb.minY()));
                    this.next();
                }
            }
            case 7 -> { // put the player on top
                final BoundingBox3dc bb = this.platform.boundingBox();
                final double x = (bb.minX() + bb.maxX()) / 2 + 2;
                final double z = (bb.minZ() + bb.maxZ()) / 2;
                run(server, fmt("tp @a %.3f %.3f %.3f", x, bb.maxY() + 0.1, z));
                this.next();
            }
            case 8 -> { // stands on it
                if (this.stepTicks == 40) {
                    final double top = this.platform.boundingBox().maxY();
                    this.check("player stands on a resting sub-level (y == platform top)",
                            Math.abs(player.getY() - top) < 0.1 && player.onGround(),
                            fmt("playerY=%.3f top=%.3f onGround=%b", player.getY(), top, player.onGround()));
                    pendingShot = "playtest-stand.png";
                    this.next();
                }
            }
            case 9 -> { // pushed by a linear impulse
                if (this.stepTicks == 1) {
                    this.startPos.set(this.platform.logicalPose().position());
                    this.playerStart.set(player.getX(), player.getY(), player.getZ());
                    final double impulse = this.platform.getMassTracker().getMass() * 4;
                    run(server, fmt("sable physics impulse @l linear %.1f 0 0", impulse));
                }
                if (this.stepTicks == 25) {
                    final Vector3d dPlatform = this.platform.logicalPose().position().sub(this.startPos, new Vector3d());
                    final double dx = player.getX() - this.playerStart.x;
                    final double dz = player.getZ() - this.playerStart.z;
                    this.check("platform moves under a linear impulse", dPlatform.x > 0.5, fmt("dPlatform.x=%.2f", dPlatform.x));
                    this.check("player is carried along by a pushed sub-level",
                            Math.abs(dx - dPlatform.x) < 0.5 && Math.abs(dz - dPlatform.z) < 0.5 && player.getY() > GROUND_TOP + 0.5,
                            fmt("dPlayer=(%.2f,%.2f) dPlatform=(%.2f,%.2f) playerY=%.2f", dx, dz, dPlatform.x, dPlatform.z, player.getY()));
                    this.lastPos.set(this.platform.logicalPose().position());
                    this.settled = 0;
                    this.next();
                }
            }
            case 10 -> { // wait for the push to die out, then re-seat the player on the left side
                if (this.settle(this.platform)) {
                    final BoundingBox3dc bb = this.platform.boundingBox();
                    run(server, fmt("tp @a %.3f %.3f %.3f", (bb.minX() + bb.maxX()) / 2 + 2, bb.maxY() + 0.1, (bb.minZ() + bb.maxZ()) / 2));
                    this.next();
                }
            }
            case 11 -> { // rotated 90 degrees about Y with the player on it
                if (this.stepTicks == 30) {
                    this.rotatePivotAngle = this.angleAround(player, this.platform);
                    this.playerStart.set(player.getX(), player.getY(), player.getZ());
                    run(server, "sable physics rotation @l add axis 0 1 0 90 global");
                }
                if (this.stepTicks == 50) {
                    final double turned = Math.toDegrees(this.angleAround(player, this.platform) - this.rotatePivotAngle);
                    final double norm = ((turned % 360) + 540) % 360 - 180;
                    final Vector3d p = this.platform.logicalPose().position();
                    final double radius = Math.hypot(player.getX() - p.x, player.getZ() - p.z);
                    this.check("player turns with a rotated sub-level (about 90 degrees)",
                            Math.abs(Math.abs(norm) - 90) < 20 && player.getY() > GROUND_TOP + 0.5,
                            fmt("turned=%.1f radius=%.2f playerY=%.2f", norm, radius, player.getY()));
                    pendingShot = "playtest-rotated.png";
                    this.next();
                }
            }
            case 12 -> { // moved 5 blocks by a translation
                if (this.stepTicks == 1) {
                    this.startPos.set(this.platform.logicalPose().position());
                    this.playerStart.set(player.getX(), player.getY(), player.getZ());
                    run(server, "sable physics translation @l add 0 0 5 global");
                }
                if (this.stepTicks == 25) {
                    final Vector3d dPlatform = this.platform.logicalPose().position().sub(this.startPos, new Vector3d());
                    final double dz = player.getZ() - this.playerStart.z;
                    this.check("platform moves under a translation", dPlatform.z > 3, fmt("dPlatform.z=%.2f", dPlatform.z));
                    this.check("player is carried along by a translated sub-level",
                            Math.abs(dz - dPlatform.z) < 0.5 && player.getY() > GROUND_TOP + 0.5,
                            fmt("dPlayerZ=%.2f dPlatformZ=%.2f playerY=%.2f", dz, dPlatform.z, player.getY()));
                    this.next();
                }
            }
            default -> {
                E2E.LOG.info("E2E: INFO ride: Sable has no seat or mount mechanism of its own (seats come from Simulated/Create, Phase 2), nothing to test");
                this.finish();
            }
        }
    }

    private double angleAround(final ServerPlayer player, final SubLevel subLevel) {
        final Vector3d p = subLevel.logicalPose().position();
        return Math.atan2(player.getZ() - p.z, player.getX() - p.x);
    }

    private void finish() {
        E2E.LOG.info("E2E: playtest {} ({} failed checks)", this.fails == 0 ? "PASSED" : "FAILED", this.fails);
        done = true;
    }

    private void next() {
        this.step++;
        this.stepTicks = 0;
    }

    private void check(final String name, final boolean ok, final String detail) {
        if (!ok) {
            this.fails++;
        }
        E2E.LOG.info("E2E: {} {}{}", ok ? "PASS" : "FAIL", name, detail.isEmpty() ? "" : " (" + detail + ")");
    }

    /** True once the sub-level has moved less than a millimetre for {@link #SETTLE_TICKS} ticks. */
    private boolean settle(final SubLevel subLevel) {
        final Vector3d pos = subLevel.logicalPose().position();
        this.settled = pos.distance(this.lastPos) < 0.001 ? this.settled + 1 : 0;
        this.lastPos.set(pos);
        return this.settled >= SETTLE_TICKS;
    }

    private void markKnown(final MinecraftServer server) {
        this.known.clear();
        for (final SubLevel subLevel : SubLevelContainer.getContainer(server.overworld()).getAllSubLevels()) {
            this.known.add(subLevel.getUniqueId());
        }
    }

    /** The one sub-level that appeared since {@link #markKnown}, or null if there isn't exactly one. */
    private ServerSubLevel newSubLevel(final MinecraftServer server) {
        final ServerSubLevelContainer container = SubLevelContainer.getContainer(server.overworld());
        ServerSubLevel found = null;
        for (final SubLevel subLevel : container.getAllSubLevels()) {
            if (!this.known.contains(subLevel.getUniqueId())) {
                if (found != null) {
                    return null;
                }
                found = (ServerSubLevel) subLevel;
            }
        }
        return found;
    }

    private static String fmt(final String format, final Object... args) {
        return String.format(Locale.ROOT, format, args);
    }

    private static void run(final MinecraftServer server, final String... commands) {
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
