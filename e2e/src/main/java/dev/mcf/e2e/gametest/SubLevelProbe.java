package dev.mcf.e2e.gametest;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.Locale;

/**
 * Follows one sub-level through a test: samples its pose and velocity (in local test coordinates) and keeps
 * extremes, so a test can assert on motion over time: falling, coming to rest, rising (balloons), accelerating along
 * an axis (propellers), turning (steering).
 *
 * <p>Call {@link #sample()} from the test's per-tick checks (sampling twice in one tick is a no-op), or
 * {@link #sampleEveryTick()} to record in the background. Every sample also checks that the sub-level is still loaded:
 * a sub-level that enters a chunk that is not ticking is moved into a holding chunk (ROADMAP S8a), and a test would
 * otherwise keep reading its last pose.
 */
public final class SubLevelProbe {
    /**
     * Below these speeds a sub-level counts as at rest [m/s, rad/s].
     */
    public static final double REST_SPEED = 0.05;
    public static final double REST_ANGULAR_SPEED = 0.05;

    private final GameTestHelper helper;
    private final ServerSubLevelContainer container;
    private final ServerSubLevel subLevel;
    private final Vector3d start;

    private final Vector3d position = new Vector3d();
    private final Vector3d velocity = new Vector3d();
    private final Vector3d angularVelocity = new Vector3d();
    private final Vector3d min = new Vector3d(Double.POSITIVE_INFINITY);
    private final Vector3d max = new Vector3d(Double.NEGATIVE_INFINITY);
    private double maxSpeed;
    private int samples;
    private int restingTicks;
    private long lastSampleTick = Long.MIN_VALUE;

    public SubLevelProbe(final GameTestHelper helper, final ServerSubLevel subLevel) {
        this.helper = helper;
        this.container = SableTestHelper.container(helper);
        this.subLevel = subLevel;
        this.start = SableTestHelper.localPosition(helper, subLevel);
        this.sample();
    }

    /**
     * Samples the sub-level now and on every following tick of the test.
     */
    public SubLevelProbe sampleEveryTick() {
        this.helper.onEachTick(this::sample);
        return this;
    }

    /**
     * Records the current pose and velocity. Fails the test if the sub-level is no longer loaded.
     */
    public SubLevelProbe sample() {
        final long tick = this.helper.getTick();
        if (tick == this.lastSampleTick) {
            return this;
        }
        this.lastSampleTick = tick;

        this.assertLoaded();
        this.position.set(SableTestHelper.localPosition(this.helper, this.subLevel));
        this.velocity.set(SableTestHelper.localVelocity(this.helper, this.subLevel));
        this.angularVelocity.set(SableTestHelper.localAngularVelocity(this.helper, this.subLevel));
        this.min.min(this.position);
        this.max.max(this.position);
        this.maxSpeed = Math.max(this.maxSpeed, this.velocity.length());
        this.restingTicks = this.isMoving() ? 0 : this.restingTicks + 1;
        this.samples++;
        return this;
    }

    public ServerSubLevel subLevel() {
        return this.subLevel;
    }

    /**
     * @return the local position of the centre of mass when the probe was created
     */
    public Vector3dc start() {
        return this.start;
    }

    /**
     * @return the local position of the centre of mass at the last sample
     */
    public Vector3dc position() {
        return this.position;
    }

    /**
     * @return the movement of the centre of mass since the probe was created, in local coordinates
     */
    public Vector3d displacement() {
        return this.position.sub(this.start, new Vector3d());
    }

    /**
     * @return the local linear velocity at the last sample [m/s]
     */
    public Vector3dc velocity() {
        return this.velocity;
    }

    /**
     * @return the local angular velocity at the last sample [rad/s]
     */
    public Vector3dc angularVelocity() {
        return this.angularVelocity;
    }

    /**
     * @return the lowest/highest local centre of mass coordinates seen
     */
    public Vector3dc min() {
        return this.min;
    }

    public Vector3dc max() {
        return this.max;
    }

    public double maxSpeed() {
        return this.maxSpeed;
    }

    public int samples() {
        return this.samples;
    }

    /**
     * @return for how many consecutive samples the sub-level has been at rest
     */
    public int restingTicks() {
        return this.restingTicks;
    }

    public boolean isMoving() {
        return this.velocity.length() >= REST_SPEED || this.angularVelocity.length() >= REST_ANGULAR_SPEED;
    }

    /**
     * @return the local Y of the lowest point of the sub-level's blocks at its current pose
     */
    public double bottom() {
        return SableTestHelper.worldBounds(this.subLevel).minY() - SableTestHelper.origin(this.helper).getY();
    }

    public void assertLoaded() {
        if (!SableTestHelper.isLoaded(this.container, this.subLevel)) {
            throw this.helper.assertionException(Component.literal("Sub-level %s is no longer loaded (removed=%s); it was last at %s"
                    .formatted(this.subLevel, this.subLevel.isRemoved(), format(this.position))));
        }
    }

    /**
     * Asserts that the sub-level has been at rest for at least {@code ticks} samples.
     */
    public void assertResting(final int ticks) {
        if (this.restingTicks < ticks) {
            throw this.helper.assertionException(Component.literal("Sub-level not at rest for %d ticks yet: %s".formatted(ticks, this)));
        }
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "pos %s, vel %s m/s, ang vel %s rad/s, bottom y %.3f, moved %s, resting %d ticks",
                format(this.position), format(this.velocity), format(this.angularVelocity), this.bottom(), format(this.displacement()), this.restingTicks);
    }

    public static String format(final Vector3dc vector) {
        return String.format(Locale.ROOT, "(%.3f, %.3f, %.3f)", vector.x(), vector.y(), vector.z());
    }
}
