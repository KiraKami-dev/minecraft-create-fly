package dev.ryanhcode.sable.api.block.propeller;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Vector3d;

/**
 * Spinny spin spin, woosh woosh!
 */
public interface BlockEntityPropeller {

    /**
     * @return the direction of the propeller
     */
    Direction getBlockDirection();

    /**
     * @return airflow in units of [m/s]
     */
    double getAirflow();

    /**
     * @return thrust in [pN]
     */
    double getThrust();

    /**
     * @return if the propeller is active / thrust should be computed
     */
    boolean isActive();

    /**
     * @return the thrust scaled by -1 * airflow scaling * air pressure
     */
    default double getScaledThrust() {
        return -this.getThrust() * this.getAirflowScaling() * this.getCurrentAirPressure();
    }

    default double getCurrentAirPressure() {
        final Level level = this.sable$getLevel();
        return DimensionPhysicsData.getAirPressure(level, Sable.HELPER.projectOutOfSubLevel(level, JOMLConversion.toJOML(this.sable$getBlockPos().getCenter())));
    }

    default double getAirflowScaling() {
        final double airflow = this.getAirflow();

        if (Math.abs(airflow) <= 0.001) {
            return 1.0;
        }

        final Level level = this.sable$getLevel();
        final Vector3d pos = JOMLConversion.toJOML(this.sable$getBlockPos().getCenter());
        final SubLevel subLevel = Sable.HELPER.getContaining(level, this.sable$getBlockPos());

        if (subLevel == null) {
            return 1.0;
        }

        final Vector3d velocity = Sable.HELPER.getVelocity(level, subLevel, pos, new Vector3d());
        final Vector3d thrustDirection = subLevel.logicalPose().transformNormal(JOMLConversion.atLowerCornerOf(this.getBlockDirection().getUnitVec3i()));

        return Math.clamp((airflow + velocity.dot(thrustDirection.x, thrustDirection.y, thrustDirection.z)) / airflow, 0, 1);
    }

    /**
     * The level of this propeller. Implemented by casting to {@link BlockEntity}, which every propeller is.
     *
     * <p>Deliberately not an abstract {@code getLevel()}: an interface method implemented only by a Minecraft method
     * is not remapped by Loom (the Minecraft method becomes {@code method_10997} in the release jar, the interface
     * method keeps its name), so every implementer would throw {@link AbstractMethodError} at runtime. Calling the
     * Minecraft method through {@link BlockEntity} is remapped normally.
     */
    default Level sable$getLevel() {
        return ((BlockEntity) this).getLevel();
    }

    /**
     * The position of this propeller. See {@link #sable$getLevel()} for why this is not an abstract
     * {@code getBlockPos()}.
     */
    default BlockPos sable$getBlockPos() {
        return ((BlockEntity) this).getBlockPos();
    }
}

