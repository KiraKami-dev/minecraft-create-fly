package dev.ryanhcode.sable.api.block;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyTypes;

/**
 * An interface for sub-classes of {@link net.minecraft.world.level.block.entity.BlockEntity} to provide angular momentum
 * when mounted on a sub-level.
 */
public interface BlockEntitySubLevelReactionWheel {
    /**
     * Get the angular velocity of this reaction wheel, in radians per second.
     * The total angular momentum given to the sublevel is this velocity scaled by {@link dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyTypes#INERTIA}
     * and by {@link dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyTypes#MASS}.
     *
     * @param angularVelocity Angular velocity to be set, using {@link org.joml.Vector3d#set(double, double, double)} or similar
     */
    void sable$getAngularVelocity(Vector3d angularVelocity);

    /**
     * The default block state getter for block entities
     *
     * <p>Deliberately not an abstract {@code getBlockState()}: an interface method implemented only by a Minecraft
     * method is not remapped by Loom (the Minecraft method becomes {@code method_11010} in the release jar, the
     * interface method keeps its name), so implementers would throw {@link AbstractMethodError} at runtime. Calling the
     * Minecraft method through {@link BlockEntity} is remapped normally.
     *
     * @return The block state for this block entity
     */
    default BlockState sable$getBlockState() {
        return ((BlockEntity) this).getBlockState();
    }
}
