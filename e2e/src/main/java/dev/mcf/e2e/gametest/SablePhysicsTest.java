package dev.mcf.e2e.gametest;

import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTestAssertPosException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import static dev.mcf.e2e.gametest.SableTestHelper.*;

/**
 * Sub-level physics on a dedicated (GameTest) server. The first three tests are upstream Sable's
 * {@code neoforge/gametest/PhysicsTest}; local Y coordinates are one lower than upstream's (see {@link SableTestHelper}).
 */
public final class SablePhysicsTest {

    /**
     * Upstream: a glass block thrown at a brick wall must not tunnel through it.
     */
    @GameTest(structure = "sable:physicstest.continuouscollision", maxTicks = 100)
    public void continuousCollision(final GameTestHelper helper) {
        final ServerSubLevelContainer plotContainer = container(helper);
        final SubLevelPhysicsSystem physicsSystem = physics(helper);

        final ServerSubLevel subLevel = spawnSingleBlockSubLevel(plotContainer, absolutePosition(helper, new Vector3d(2.5, 3, 1.5)), Blocks.GLASS.defaultBlockState());
        final RigidBodyHandle handle = physicsSystem.getPhysicsHandle(subLevel);
        final Vector3d impulse = absoluteDirection(helper, new Vector3d(0, 10, 20));
        // not upstream: a sub-level that got unloaded would pass the wall check with its last pose
        final SubLevelProbe probe = new SubLevelProbe(helper, subLevel).sampleEveryTick();

        helper.startSequence()
                .thenExecuteAfter(10, () -> handle.applyLinearImpulse(impulse))
                .thenExecuteFor(40, () -> {
                    final Vector3d globalPos = subLevel.logicalPose().position();
                    final Vector3d localPos = localPosition(helper, globalPos);
                    if (localPos.z >= 9 || !isInBounds(helper, globalPos)) {
                        helper.fail(Component.literal("Sublevel passed through wall"), BlockPos.containing(localPos.x, localPos.y, localPos.z));
                    }
                })
                .thenExecute(() -> {
                    if (probe.maxSpeed() < 1.0) {
                        helper.fail(Component.literal("Sublevel never moved: " + probe));
                    }
                })
                .thenSucceed();
    }

    /**
     * Upstream (optional): after one second of free fall the velocity is g * 1 s and the drop is g / 2, to 1e-2.
     */
    // FIXME (upstream) allow manual tests to run automatically when rapier is set to 64-bit mode
    @GameTest(structure = "sable:physicstest.gravity", maxTicks = 100, required = false)
    public void gravity(final GameTestHelper helper) {
        final ServerSubLevelContainer plotContainer = container(helper);
        final SubLevelPhysicsSystem physicsSystem = physics(helper);

        final Vector3dc spawnPos = absolutePosition(helper, new Vector3d(2.5, 11, 2.5));
        final ServerSubLevel subLevel = spawnSingleBlockSubLevel(plotContainer, spawnPos, Blocks.DIAMOND_BLOCK.defaultBlockState());

        helper.runAfterDelay(20, () -> {
            if (subLevel.isRemoved()) {
                helper.fail("Sublevel was removed");
                return;
            }

            final Vector3dc gravity = DimensionPhysicsData.getGravity(helper.getLevel(), spawnPos);
            final RigidBodyHandle handle = physicsSystem.getPhysicsHandle(subLevel);
            final Vector3dc linearVelocity = handle.getLinearVelocity(new Vector3d());

            if (!gravity.equals(linearVelocity, 1e-2)) {
                final Vector3d localPos = localPosition(helper, spawnPos);
                helper.fail(Component.literal("Sublevel velocity didn't follow gravity: Delta: " + gravity.distance(linearVelocity)
                        + " (expected " + SubLevelProbe.format(gravity) + ", got " + SubLevelProbe.format(linearVelocity) + ")"), BlockPos.containing(localPos.x, localPos.y, localPos.z));
                return;
            }

            // 1/2 * a * t * t
            // t = 1
            final Vector3d expectedDelta = gravity.mul(0.5, new Vector3d());
            final Vector3d delta = subLevel.logicalPose().position().sub(spawnPos, new Vector3d());

            // FIXME (upstream) for some reason the sublevels don't have a consistent distance travelled. It seems like they aren't spawned on the first tick?
            if (!expectedDelta.equals(delta, 1e-2)) {
                final Vector3d localPos = localPosition(helper, spawnPos);
                helper.fail(Component.literal("Sublevel position didn't follow gravity. Delta: " + expectedDelta.distance(delta)
                        + " (expected " + SubLevelProbe.format(expectedDelta) + ", got " + SubLevelProbe.format(delta) + ")"), BlockPos.containing(localPos.x, localPos.y, localPos.z));
            }

            helper.succeed();
        });
    }

    /**
     * Upstream (optional, must pass 10 of 10 attempts): a diamond block pushed along a one-block channel must slide
     * through it instead of snagging on the seams between the channel's blocks.
     */
    @GameTest(structure = "sable:physicstest.snag", maxTicks = 100, maxAttempts = 10, requiredSuccesses = 10, required = false)
    public void snag(final GameTestHelper helper) {
        final ServerSubLevelContainer plotContainer = container(helper);
        final SubLevelPhysicsSystem physicsSystem = physics(helper);

        final Vector3dc spawnPos = absolutePosition(helper, new Vector3d(13, 2.5, 3.5));
        final ServerSubLevel subLevel = spawnSingleBlockSubLevel(plotContainer, spawnPos, Blocks.DIAMOND_BLOCK.defaultBlockState());
        final RigidBodyHandle handle = physicsSystem.getPhysicsHandle(subLevel);
        final Vector3d impulse = absoluteDirection(helper, new Vector3d(-60, 0, 0));

        helper.startSequence()
                .thenExecuteAfter(10, () -> handle.applyLinearImpulse(impulse))
                .thenExecuteFor(40, () -> {
                    final Vector3d globalPos = subLevel.logicalPose().position();
                    final Vector3d localPos = localPosition(helper, globalPos);
                    if (localPos.x <= 9 && isInBounds(helper, globalPos)) {
                        helper.succeed();
                    }
                }).thenFail(() -> {
                    final Vector3dc position = subLevel.logicalPose().position();
                    final BlockPos globalPos = BlockPos.containing(position.x(), position.y(), position.z());
                    return new GameTestAssertPosException(Component.literal("Sub-level got stuck at local " + SubLevelProbe.format(localPosition(helper, position))),
                            globalPos, helper.relativePos(globalPos), (int) helper.getTick());
                });
    }

    /**
     * A 3x2x3 box spawned mid-air must stay loaded, fall straight down at about free-fall speed, land on the floor
     * and come to rest there, still loaded and intact. This is the dedicated-server physics check (ROADMAP S8a).
     */
    @GameTest(structure = "sable:physicstest.gravity", maxTicks = 200)
    public void fallsAndComesToRest(final GameTestHelper helper) {
        final Vec3i size = new Vec3i(3, 2, 3);
        final int blocks = size.getX() * size.getY() * size.getZ();
        // the shaft is 5x5 inside with its floor at local y 0 (top face y = 1); the box spans y 7..9
        final ServerSubLevel subLevel = spawnBox(helper, new Vector3d(2.5, 8, 2.5), size, Blocks.OAK_PLANKS.defaultBlockState());
        final SubLevelProbe probe = new SubLevelProbe(helper, subLevel).sampleEveryTick();
        final Vector3dc gravity = localDirection(helper, DimensionPhysicsData.getGravity(helper.getLevel(), subLevel.logicalPose().position()));

        helper.assertValueEqual(countBlocks(subLevel), blocks, "blocks in the spawned sub-level");
        helper.assertTrue(Math.abs(probe.bottom() - 7.0) < 0.05, "box spawned with its bottom at y 7: " + probe);

        final int fallTicks = 5;
        helper.startSequence()
                .thenExecuteAfter(fallTicks, () -> {
                    probe.sample();
                    // between 50% and 120% of the free-fall speed after fallTicks physics steps, straight down
                    final double freeFallSpeed = gravity.y() * fallTicks / 20.0;
                    final Vector3dc velocity = probe.velocity();
                    if (!(velocity.y() <= 0.5 * freeFallSpeed && velocity.y() >= 1.2 * freeFallSpeed)) {
                        helper.fail("Not falling at about free-fall speed %.2f m/s: %s".formatted(freeFallSpeed, probe));
                    }
                    if (Math.abs(velocity.x()) > 0.1 || Math.abs(velocity.z()) > 0.1 || probe.displacement().y() > -0.05) {
                        helper.fail("Not falling straight down: " + probe);
                    }
                })
                .thenWaitUntil(() -> {
                    probe.sample();
                    probe.assertResting(10);
                })
                .thenExecute(() -> {
                    if (Math.abs(probe.bottom() - 1.0) > 0.1) {
                        helper.fail("Came to rest with its bottom at y %.3f instead of on the floor (y 1): %s".formatted(probe.bottom(), probe));
                    }
                    final Vector3d moved = probe.displacement();
                    if (Math.abs(moved.x) > 0.25 || Math.abs(moved.z) > 0.25) {
                        helper.fail("Drifted sideways while falling: " + probe);
                    }
                    helper.assertValueEqual(countBlocks(subLevel), blocks, "blocks in the sub-level after landing");
                })
                .thenSucceed();
    }
}
