package dev.ryanhcode.sable.api.physics.force;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import org.joml.Vector3dc;

/**
 * PORT(1.21.11): rejects point impulses that cannot be real.
 * A sub-level fits inside its plot, so a lever arm (impulse point minus center of mass) longer than a plot is a
 * world-space position passed where a plot-space one was expected. Applied anyway, that torque spins the sub-level
 * and flings it thousands of blocks away (arrows did this before the AbstractArrowMixin fix).
 */
public final class ImpulseGuard {

    private static final double MAX_LEVER_ARM = 2.0 * (16 << SubLevelContainer.DEFAULT_LOG_PLOT_SIZE);
    private static boolean loggedTrace = false;

    private ImpulseGuard() {
    }

    /**
     * @param leverArm the impulse point relative to the center of mass [m]
     * @param force    the impulse [N]
     * @return if the impulse is safe to apply; logs a warning (with a stack trace the first time) if not
     */
    public static boolean check(final Vector3dc leverArm, final Vector3dc force) {
        if (leverArm.isFinite() && force.isFinite() && leverArm.lengthSquared() <= MAX_LEVER_ARM * MAX_LEVER_ARM) {
            return true;
        }

        final String message = "Dropped an impulse of " + force + " at lever arm " + leverArm + " (not inside the sub-level's plot)";
        if (loggedTrace) {
            Sable.LOGGER.warn(message);
        } else {
            loggedTrace = true;
            Sable.LOGGER.warn(message, new IllegalArgumentException("impulse point outside plot"));
        }
        return false;
    }
}
