package dev.mcf.e2e.gametest;

import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTestAssertPosException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SimpleExplosionDamageCalculator;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

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
     * Upstream's gravity check with Sable's universal drag (0.09 by default) accounted for, which is why upstream's
     * version fails: Rapier damps every substep, {@code v = (v + g dt) / (1 + drag dt)}, then moves by the new velocity.
     */
    @GameTest(structure = "sable:physicstest.gravity", maxTicks = 100)
    public void freeFallWithDrag(final GameTestHelper helper) {
        final Vector3dc spawnPos = absolutePosition(helper, new Vector3d(2.5, 11, 2.5));
        final ServerSubLevel subLevel = spawnSingleBlockSubLevel(container(helper), spawnPos, Blocks.DIAMOND_BLOCK.defaultBlockState());
        final SubLevelProbe probe = new SubLevelProbe(helper, subLevel).sampleEveryTick();
        final int ticks = 20;

        helper.runAfterDelay(ticks, () -> {
            probe.sample();
            final Vector3dc gravity = DimensionPhysicsData.getGravity(helper.getLevel(), spawnPos);
            final double drag = DimensionPhysicsData.getUniversalDrag(helper.getLevel());
            final int substeps = physics(helper).getConfig().substepsPerTick;
            final double dt = 1.0 / 20.0 / substeps;

            final Vector3d expectedVelocity = new Vector3d();
            final Vector3d coarseDelta = new Vector3d();
            for (int i = 0; i < ticks * substeps; i++) {
                expectedVelocity.fma(dt, gravity);
                // one position step per substep: the coarsest integration (Rapier steps positions more finely)
                coarseDelta.fma(dt, expectedVelocity);
                expectedVelocity.div(1.0 + drag * dt);
            }
            // the exact drop with linear drag k: g / k * (t - (1 - e^-kt) / k)
            final double t = ticks / 20.0;
            final double exactDrop = gravity.length() * (drag > 0.0 ? (t - (1.0 - Math.exp(-drag * t)) / drag) / drag : t * t / 2.0);
            final double coarseDrop = coarseDelta.length();

            final Vector3d down = gravity.normalize(new Vector3d());
            final Vector3d velocity = handle(helper, subLevel).getLinearVelocity(new Vector3d());
            final Vector3d delta = subLevel.logicalPose().position().sub(spawnPos, new Vector3d());
            final double drop = delta.dot(down);
            final double sideways = delta.fma(-drop, down, new Vector3d()).length();
            if (!expectedVelocity.equals(velocity, 0.01) || drop < exactDrop - 0.01 || drop > coarseDrop + 0.01 || sideways > 0.01) {
                helper.fail("After %d ticks expected velocity %s and a drop of %.3f..%.3f m straight down (g %s, drag %s, %d substeps), got %s and %s".formatted(
                        ticks, SubLevelProbe.format(expectedVelocity), exactDrop, coarseDrop, SubLevelProbe.format(gravity), drag, substeps,
                        SubLevelProbe.format(velocity), SubLevelProbe.format(delta)));
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
                    final Vector3d localPos = localPosition(helper, position);
                    // (vanilla's helper.relativePos mirrors positions of unrotated tests)
                    return new GameTestAssertPosException(Component.literal("Sub-level got stuck at local " + SubLevelProbe.format(localPos)),
                            BlockPos.containing(position.x(), position.y(), position.z()), BlockPos.containing(localPos.x, localPos.y, localPos.z), (int) helper.getTick());
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

        helper.assertValueEqual(blocks, countBlocks(subLevel), "blocks in the spawned sub-level");
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
                    helper.assertValueEqual(blocks, countBlocks(subLevel), "blocks in the sub-level after landing");
                })
                .thenSucceed();
    }

    /**
     * Projectiles hitting a sub-level push it a little and nothing more. Arrows used to fling it ~20k blocks away
     * (AbstractArrowMixin transformed a plot-space hit point a second time); every vanilla projectile is checked, plus an
     * eye of ender flying through and a mace Wind Burst on top.
     */
    @GameTest(structure = "sable:physicstest.gravity", maxTicks = 200)
    public void projectilesDoNotFlingSubLevels(final GameTestHelper helper) {
        final ServerSubLevel subLevel = spawnBox(helper, new Vector3d(2.5, 2, 2.5), new Vec3i(3, 2, 3), Blocks.OAK_PLANKS.defaultBlockState());
        final SubLevelProbe probe = new SubLevelProbe(helper, subLevel).sampleEveryTick();
        final List<EntityType<?>> projectiles = List.of(EntityType.ARROW, EntityType.SPECTRAL_ARROW, EntityType.TRIDENT, EntityType.SNOWBALL,
                EntityType.EGG, EntityType.WIND_CHARGE, EntityType.SMALL_FIREBALL, EntityType.ENDER_PEARL);

        helper.startSequence()
                .thenExecuteAfter(10, () -> {
                    for (int i = 0; i < projectiles.size(); i++) {
                        final Entity projectile = projectiles.get(i).create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
                        final Vector3d pos = absolutePosition(helper, new Vector3d(1.6 + (i % 3) * 0.9, 8, 1.6 + (i / 3) * 0.9));
                        projectile.setPos(pos.x, pos.y, pos.z);
                        projectile.setDeltaMovement(JOMLConversion.toMojang(absoluteDirection(helper, new Vector3d(0, -2.5, 0))));
                        helper.getLevel().addFreshEntity(projectile);
                    }
                    // an eye of ender flying straight through the box (it never collides, so it must not push)
                    final Vector3d eyeStart = absolutePosition(helper, new Vector3d(2.5, 2, 0.5));
                    final EyeOfEnder eye = new EyeOfEnder(helper.getLevel(), eyeStart.x, eyeStart.y, eyeStart.z);
                    eye.signalTo(JOMLConversion.toMojang(absolutePosition(helper, new Vector3d(2.5, 2, 40))));
                    helper.getLevel().addFreshEntity(eye);
                    // a mace's Wind Burst III landing on top of the box: vanilla's ExplodeEffect parameters
                    final Optional<HolderSet<Block>> immune = helper.getLevel().registryAccess().lookupOrThrow(Registries.BLOCK)
                            .get(BlockTags.BLOCKS_WIND_CHARGE_EXPLOSIONS).map(Function.identity());
                    helper.getLevel().explode(null, null, new SimpleExplosionDamageCalculator(false, false, Optional.of(2.2F), immune),
                            JOMLConversion.toMojang(absolutePosition(helper, new Vector3d(2.5, 3.5, 2.5))), 3.5F, false, Level.ExplosionInteraction.TRIGGER);
                })
                .thenExecuteAfter(60, () -> {
                    probe.assertLoaded();
                    if (probe.displacement().length() > 2.0 || probe.maxSpeed() > 10.0) {
                        helper.fail("Projectiles flung the sub-level: " + probe);
                    }
                })
                .thenSucceed();
    }

    /**
     * Any point impulse more than a plot away from the centre of mass is a coordinate-space mix-up and is dropped
     * ({@code ImpulseGuard}), so a future bug like the arrow one cannot fling a sub-level either.
     */
    @GameTest(structure = "sable:physicstest.gravity", maxTicks = 100)
    public void impulseOutsidePlotIsDropped(final GameTestHelper helper) {
        final ServerSubLevel subLevel = spawnBox(helper, new Vector3d(2.5, 2, 2.5), new Vec3i(3, 2, 3), Blocks.OAK_PLANKS.defaultBlockState());
        final SubLevelProbe probe = new SubLevelProbe(helper, subLevel).sampleEveryTick();

        helper.startSequence()
                .thenExecuteAfter(10, () -> {
                    // a world-space point where a plot-space one belongs
                    final Vector3d worldPoint = new Vector3d(subLevel.logicalPose().position());
                    handle(helper, subLevel).applyImpulseAtPoint(worldPoint, absoluteDirection(helper, new Vector3d(0, 2.5, 0)));
                    subLevel.getOrCreateQueuedForceGroup(ForceGroups.LIFT.get()).applyAndRecordPointForce(worldPoint, absoluteDirection(helper, new Vector3d(0, 2.5, 0)));
                })
                .thenExecuteAfter(40, () -> {
                    probe.assertLoaded();
                    if (probe.displacement().length() > 0.5 || probe.maxSpeed() > 1.0) {
                        helper.fail("An impulse outside the plot moved the sub-level: " + probe);
                    }
                })
                .thenSucceed();
    }
}
