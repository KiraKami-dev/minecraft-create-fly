package dev.mcf.e2e.gametest;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.CommonLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector2i;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Sub-level helpers for GameTests: a port of upstream Sable's {@code neoforge/gametest/SableTestHelper}, plus the
 * building blocks the Simulated/Aeronautics tests need (assembly, pose/velocity in test coordinates, block counts).
 *
 * <p><b>Coordinates.</b> "Local" means test-relative, in the frame of {@link GameTestHelper#absolutePos}. On 1.21.11
 * local (0, 0, 0) is the first block of the test structure (the test instance block's start corner). On 1.21.1 it
 * was the structure block one below it, so upstream's local Y values are one higher than the ones used here.
 * Positions are continuous: local (0.5, 0.5, 0.5) is the centre of the structure's first block. Rotated tests rotate
 * about the origin block like {@link GameTestHelper#absoluteVec} does.
 *
 * <p><b>Poses.</b> A sub-level's {@link SubLevel#logicalPose() pose} position is the world position of its centre of
 * mass (the pose's rotation point), so a single-block sub-level's position is the centre of that block.
 */
public final class SableTestHelper {

    private SableTestHelper() {
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Containers and physics

    /**
     * @return the sub-level container of the test level
     */
    public static ServerSubLevelContainer container(final GameTestHelper helper) {
        final ServerSubLevelContainer container = SubLevelContainer.getContainer(helper.getLevel());
        if (container == null) {
            throw new IllegalStateException("Plot container not found in level");
        }
        return container;
    }

    /**
     * @return the physics system of the test level
     */
    public static SubLevelPhysicsSystem physics(final GameTestHelper helper) {
        final SubLevelPhysicsSystem physicsSystem = container(helper).physicsSystem();
        if (physicsSystem == null) {
            throw new IllegalStateException("Plot container does not have physics");
        }
        return physicsSystem;
    }

    /**
     * @return a physics handle (impulses, velocities) for a sub-level
     */
    public static RigidBodyHandle handle(final GameTestHelper helper, final ServerSubLevel subLevel) {
        return physics(helper).getPhysicsHandle(subLevel);
    }

    /**
     * @return if the sub-level is still loaded in its container (not removed, and not moved into a holding chunk)
     */
    public static boolean isLoaded(final SubLevelContainer container, final SubLevel subLevel) {
        return !subLevel.isRemoved() && container.getSubLevel(subLevel.getUniqueId()) == subLevel;
    }

    /**
     * @return all loaded sub-levels whose bounds intersect the test structure
     */
    public static List<ServerSubLevel> subLevelsInTest(final GameTestHelper helper) {
        final List<ServerSubLevel> subLevels = new ArrayList<>();
        for (final SubLevel subLevel : container(helper).queryIntersecting(new BoundingBox3d(helper.getBounds()))) {
            subLevels.add((ServerSubLevel) subLevel);
        }
        return subLevels;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Spawning, assembly and removal

    /**
     * Spawns an empty sub-level at a world position and lets {@code setter} place its blocks, with (0, 0, 0) at the
     * plot centre. The centre of mass ends up near {@code pos}; see {@link #spawnBox} to place it exactly.
     */
    public static ServerSubLevel spawnSubLevel(final SubLevelContainer plotContainer, final Vector3dc pos, final Consumer<CommonLevelAccessor> setter) {
        final Pose3d pose = new Pose3d();
        pose.position().set(pos);

        final SubLevel subLevel = plotContainer.allocateNewSubLevel(pose);
        final LevelPlot plot = subLevel.getPlot();

        final ChunkPos center = plot.getCenterChunk();
        plot.newEmptyChunk(center);

        setter.accept(plot.getEmbeddedLevelAccessor());
        subLevel.updateLastPose();
        return (ServerSubLevel) subLevel;
    }

    /**
     * Spawns a single-block sub-level whose block centre is at world position {@code pos}.
     */
    public static ServerSubLevel spawnSingleBlockSubLevel(final SubLevelContainer plotContainer, final Vector3dc pos, final BlockState state) {
        return spawnSubLevel(plotContainer, pos, accessor -> accessor.setBlock(BlockPos.ZERO, state, 3));
    }

    /**
     * Spawns a box of {@code size} blocks of {@code state}, oriented with the test, with its centre of mass at local
     * position {@code localCenter}.
     */
    public static ServerSubLevel spawnBox(final GameTestHelper helper, final Vector3dc localCenter, final Vec3i size, final BlockState state) {
        final Vector3d center = absolutePosition(helper, localCenter);
        final ServerSubLevel subLevel = spawnSubLevel(container(helper), center, accessor -> {
            for (final BlockPos pos : BlockPos.betweenClosed(0, 0, 0, size.getX() - 1, size.getY() - 1, size.getZ() - 1)) {
                accessor.setBlock(pos, state, 3);
            }
        });
        // every placed block moves the centre of mass, and the body with it; put it where it was asked for
        teleport(helper, subLevel, center, testOrientation(helper));
        return subLevel;
    }

    /**
     * Assembles the world blocks at the given local positions into a new sub-level, anchored at the first one. The
     * blocks keep their world position (this is what a physics assembler does). Air positions are skipped.
     */
    public static ServerSubLevel assemble(final GameTestHelper helper, final List<BlockPos> localBlocks) {
        if (localBlocks.isEmpty()) {
            throw new IllegalArgumentException("Nothing to assemble");
        }

        final List<BlockPos> blocks = new ArrayList<>(localBlocks.size());
        BoundingBox3i bounds = null;
        for (final BlockPos local : localBlocks) {
            final BlockPos pos = helper.absolutePos(local);
            if (helper.getLevel().getBlockState(pos).isAir()) {
                continue;
            }

            blocks.add(pos);
            if (bounds == null) {
                bounds = new BoundingBox3i(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
            } else {
                bounds.expandTo(pos.getX(), pos.getY(), pos.getZ());
            }
        }

        if (bounds == null) {
            throw helper.assertionException(Component.literal("Nothing to assemble: all " + localBlocks.size() + " positions are air"));
        }

        return SubLevelAssemblyHelper.assembleBlocks(helper.getLevel(), blocks.getFirst(), blocks, bounds);
    }

    /**
     * Assembles every non-air world block in the local box {@code localMin}..{@code localMax} (inclusive).
     */
    public static ServerSubLevel assembleRegion(final GameTestHelper helper, final BlockPos localMin, final BlockPos localMax) {
        final List<BlockPos> blocks = new ArrayList<>();
        for (final BlockPos pos : BlockPos.betweenClosed(localMin, localMax)) {
            blocks.add(pos.immutable());
        }
        return assemble(helper, blocks);
    }

    /**
     * Moves a sub-level's centre of mass to a world position, keeping the physics body and the logical pose in sync.
     */
    public static void teleport(final GameTestHelper helper, final ServerSubLevel subLevel, final Vector3dc position, final Quaterniondc orientation) {
        physics(helper).getPipeline().teleport(subLevel, position, orientation);
        subLevel.updateLastPose();
    }

    /**
     * Removes a sub-level and its blocks from the level (upstream helper).
     */
    public static void removeSubLevel(final SubLevelContainer container, final ServerSubLevel subLevel) {
        final LevelPlot plot = subLevel.getPlot();
        final Vector2i origin = container.getOrigin();
        container.removeSubLevel(plot.plotPos.x - origin.x, plot.plotPos.z - origin.y, SubLevelRemovalReason.REMOVED);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Inspection

    /**
     * @return the number of non-air blocks in the sub-level's plot
     */
    public static int countBlocks(final SubLevel subLevel) {
        final BoundingBox3ic bounds = subLevel.getPlot().getBoundingBox();
        if (bounds == BoundingBox3i.EMPTY || bounds.volume() <= 0) {
            return 0;
        }

        int count = 0;
        for (final BlockPos pos : BlockPos.betweenClosed(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ())) {
            if (!subLevel.getLevel().getBlockState(pos).isAir()) {
                count++;
            }
        }
        return count;
    }

    /**
     * @return the number of non-air blocks in the local box {@code localMin}..{@code localMax} (inclusive) of the world
     */
    public static int countWorldBlocks(final GameTestHelper helper, final BlockPos localMin, final BlockPos localMax) {
        int count = 0;
        for (final BlockPos pos : BlockPos.betweenClosed(localMin, localMax)) {
            if (!helper.getBlockState(pos).isAir()) {
                count++;
            }
        }
        return count;
    }

    /**
     * @return the world bounds of the sub-level's blocks at its current pose. Unlike {@link SubLevel#boundingBox()},
     * which is refreshed before the physics step, this is up to date with the pose.
     */
    public static BoundingBox3d worldBounds(final SubLevel subLevel) {
        final BoundingBox3ic plotBounds = subLevel.getPlot().getBoundingBox();
        final BoundingBox3d bounds = new BoundingBox3d(plotBounds.minX(), plotBounds.minY(), plotBounds.minZ(),
                plotBounds.maxX() + 1.0, plotBounds.maxY() + 1.0, plotBounds.maxZ() + 1.0);
        return bounds.transform(subLevel.logicalPose());
    }

    /**
     * @return the sub-level's centre of mass in local coordinates
     */
    public static Vector3d localPosition(final GameTestHelper helper, final SubLevel subLevel) {
        return localPosition(helper, subLevel.logicalPose().position());
    }

    /**
     * @return the linear velocity of the sub-level's physics body in local coordinates [m/s]
     */
    public static Vector3d localVelocity(final GameTestHelper helper, final ServerSubLevel subLevel) {
        return localDirection(helper, handle(helper, subLevel).getLinearVelocity(new Vector3d()));
    }

    /**
     * @return the angular velocity of the sub-level's physics body in local coordinates [rad/s]
     */
    public static Vector3d localAngularVelocity(final GameTestHelper helper, final ServerSubLevel subLevel) {
        return localDirection(helper, handle(helper, subLevel).getAngularVelocity(new Vector3d()));
    }

    /**
     * @return a one-line description of a sub-level's state for failure messages
     */
    public static String describe(final GameTestHelper helper, final ServerSubLevel subLevel) {
        final SubLevelContainer container = container(helper);
        if (!isLoaded(container, subLevel)) {
            return "%s (removed=%s, not loaded)".formatted(subLevel, subLevel.isRemoved());
        }

        final Vector3d pos = localPosition(helper, subLevel);
        final Vector3d vel = localVelocity(helper, subLevel);
        final double bottom = worldBounds(subLevel).minY() - origin(helper).getY();
        return String.format(Locale.ROOT, "local pos (%.3f, %.3f, %.3f), vel (%.3f, %.3f, %.3f) m/s, bottom y %.3f, %d blocks",
                pos.x, pos.y, pos.z, vel.x, vel.y, vel.z, bottom, countBlocks(subLevel));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Blocks and redstone

    /**
     * Fills the local box {@code localMin}..{@code localMax} (inclusive) with {@code state}.
     */
    public static void fill(final GameTestHelper helper, final BlockPos localMin, final BlockPos localMax, final BlockState state) {
        for (final BlockPos pos : BlockPos.betweenClosed(localMin, localMax)) {
            helper.setBlock(pos, state);
        }
    }

    /**
     * Powers the blocks around {@code localPos} by placing a redstone block there; {@link #unpower} removes it.
     * Use {@link GameTestHelper#pulseRedstone} for a timed pulse, {@link GameTestHelper#pullLever} for levers.
     */
    public static void power(final GameTestHelper helper, final BlockPos localPos) {
        helper.setBlock(localPos, Blocks.REDSTONE_BLOCK);
    }

    /**
     * Removes the redstone block placed by {@link #power}.
     */
    public static void unpower(final GameTestHelper helper, final BlockPos localPos) {
        helper.setBlock(localPos, Blocks.AIR);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Test coordinates (upstream helpers, see the class comment for the frame)

    /**
     * @return the world position of the test origin block (local 0, 0, 0)
     */
    public static BlockPos origin(final GameTestHelper helper) {
        return helper.absolutePos(BlockPos.ZERO);
    }

    /**
     * @return the world orientation of the test structure
     */
    public static Quaterniond testOrientation(final GameTestHelper helper) {
        return helper.getTestRotation().rotation().transformation().getNormalizedRotation(new Quaterniond());
    }

    public static Vector3d absoluteDirection(final GameTestHelper helper, final Vector3dc localDirection) {
        return new Vector3d(localDirection).rotateY(-getAngle(helper.getTestRotation()));
    }

    public static Vector3d localDirection(final GameTestHelper helper, final Vector3dc globalDirection) {
        return new Vector3d(globalDirection).rotateY(getAngle(helper.getTestRotation()));
    }

    public static Vector3d absolutePosition(final GameTestHelper helper, final Vector3dc localPosition) {
        final BlockPos origin = origin(helper);
        final Vector3d pos = localPosition.sub(0.5, 0.5, 0.5, new Vector3d()).rotateY(-getAngle(helper.getTestRotation()));
        return pos.add(origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5);
    }

    public static Vector3d absolutePosition(final GameTestHelper helper, final double x, final double y, final double z) {
        return absolutePosition(helper, new Vector3d(x, y, z));
    }

    /**
     * Inverse of {@link #absolutePosition}. (Upstream rotated about the origin block's corner here instead of its
     * centre; that only differs for rotated tests.)
     */
    public static Vector3d localPosition(final GameTestHelper helper, final Vector3dc globalPosition) {
        final BlockPos origin = origin(helper);
        final Vector3d pos = globalPosition.sub(origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5, new Vector3d());
        return pos.rotateY(getAngle(helper.getTestRotation())).add(0.5, 0.5, 0.5);
    }

    public static double getAngle(final Rotation rotation) {
        return switch (rotation) {
            case NONE -> 0;
            case CLOCKWISE_90 -> Math.PI / 2.0;
            case CLOCKWISE_180 -> Math.PI;
            case COUNTERCLOCKWISE_90 -> -Math.PI / 2.0;
        };
    }

    /**
     * Checks if the specified global position is within the bounds of the test.
     *
     * @param helper         The game test helper instance
     * @param globalPosition The position in global space to check
     * @return Whether that position is inside the bounds of the test
     */
    public static boolean isInBounds(final GameTestHelper helper, final Vector3dc globalPosition) {
        final AABB box = helper.getBounds();
        return box.contains(globalPosition.x(), globalPosition.y(), globalPosition.z());
    }
}
