package dev.mcf.e2e.gametest;

import com.mojang.authlib.GameProfile;
import com.zurrtum.create.AllBlocks;
import com.zurrtum.create.content.contraptions.glue.SuperGlueEntity;
import com.zurrtum.create.content.kinetics.base.KineticBlockEntity;
import com.zurrtum.create.content.kinetics.motor.CreativeMotorBlock;
import com.zurrtum.create.content.kinetics.motor.CreativeMotorBlockEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.veillite.api.network.handler.ServerPacketContext;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerBlockEntity;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.network.packets.AssemblePacket;
import dev.simulated_team.simulated.util.extra_kinetics.ExtraKinetics;
import io.netty.channel.ChannelFutureListener;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToDoubleFunction;

import static dev.mcf.e2e.gametest.SableTestHelper.*;

/**
 * Helpers for the Simulated content GameTests: a port of upstream Simulated's
 * {@code neoforge/gametest/SimulatedGameTestHelper} (kinetic speed assertions, positions are test-local and
 * {@code helper.getBlockEntity(pos)} became {@code getBlockEntity(pos, Class)}), plus what the ship tests need: super
 * glue, the physics assembler (triggered through the same server handler a player's packet reaches), and lookups of
 * blocks that an assembly moved into a sub-level's plot.
 */
public final class SimulatedTestHelper {

    /**
     * The fake player that "presses" physics assemblers. Fabric's {@link FakePlayer} is a {@link ServerPlayer} that
     * is not in the player list (does not load chunks, ignores stats).
     */
    private static final GameProfile ASSEMBLING_PLAYER = new GameProfile(UUID.fromString("6d63665f-6532-4565-a173-73656d626c79"), "mcf_e2e_assembler");

    private SimulatedTestHelper() {
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Kinetics (upstream SimulatedGameTestHelper)

    public static KineticBlockEntity kinetic(final GameTestHelper helper, final BlockPos localPos) {
        return helper.getBlockEntity(localPos, KineticBlockEntity.class);
    }

    public static <T extends BlockEntity & ExtraKinetics> void assertExtraKinetics(final GameTestHelper helper, final BlockPos pos, final Class<T> type,
                                                                                    final BiPredicate<T, KineticBlockEntity> predicate,
                                                                                    final BiFunction<T, KineticBlockEntity, String> exceptionMessage) {
        final T t = helper.getBlockEntity(pos, type);
        if (!predicate.test(t, t.getExtraKinetics())) {
            helper.fail(Component.literal(exceptionMessage.apply(t, t.getExtraKinetics())), pos);
        }
    }

    public static <T extends KineticBlockEntity> void assertKineticsSpeed(final GameTestHelper helper, final BlockPos pos, final Class<T> type,
                                                                          final ToDoubleFunction<T> speed, final double delta) {
        helper.assertBlockEntityData(pos, type, be -> Math.abs(Math.abs(be.getSpeed()) - Math.abs(speed.applyAsDouble(be))) < delta, () -> {
            final T be = helper.getBlockEntity(pos, type);
            return Component.literal("Expected %.2f speed, got %.2f".formatted(Math.abs(speed.applyAsDouble(be)), Math.abs(be.getSpeed())));
        });
    }

    public static <T extends KineticBlockEntity> void assertKineticsSpeed(final GameTestHelper helper, final BlockPos pos, final Class<T> type, final ToDoubleFunction<T> speed) {
        assertKineticsSpeed(helper, pos, type, speed, 1e-6);
    }

    public static void assertKineticsSpeed(final GameTestHelper helper, final BlockPos pos, final double speed, final double delta) {
        assertKineticsSpeed(helper, pos, KineticBlockEntity.class, be -> speed, delta);
    }

    public static void assertKineticsSpeed(final GameTestHelper helper, final BlockPos pos, final double speed) {
        assertKineticsSpeed(helper, pos, KineticBlockEntity.class, be -> speed, 1e-6);
    }

    public static <T extends KineticBlockEntity & ExtraKinetics> void assertExtraKineticsSpeed(final GameTestHelper helper, final BlockPos pos, final Class<T> type,
                                                                                                final ToDoubleBiFunction<T, KineticBlockEntity> speed,
                                                                                                final ToDoubleBiFunction<T, KineticBlockEntity> extraSpeed,
                                                                                                final double delta) {
        assertExtraKinetics(helper, pos, type, (blockEntity, extraKinetics) -> Math.abs(Math.abs(blockEntity.getSpeed()) - speed.applyAsDouble(blockEntity, extraKinetics)) < delta &&
                        extraKinetics != null &&
                        Math.abs(Math.abs(extraKinetics.getSpeed()) - Math.abs(extraSpeed.applyAsDouble(blockEntity, extraKinetics))) < delta,
                (blockEntity, extraKinetics) -> {
                    if (extraKinetics == null) {
                        return "Expected extra kinetics, got null";
                    }
                    final double speedValue = Math.abs(speed.applyAsDouble(blockEntity, extraKinetics));
                    final double extraSpeedValue = Math.abs(extraSpeed.applyAsDouble(blockEntity, extraKinetics));
                    if (Math.abs(Math.abs(blockEntity.getSpeed()) - speedValue) >= delta) {
                        return "Expected %.2f speed, got %.2f".formatted(speedValue, Math.abs(blockEntity.getSpeed()));
                    }
                    return "Expected %.2f extra kinetics speed, got %.2f".formatted(Math.abs(extraSpeedValue), Math.abs(extraKinetics.getSpeed()));
                });
    }

    public static <T extends KineticBlockEntity & ExtraKinetics> void assertExtraKineticsSpeed(final GameTestHelper helper, final BlockPos pos, final Class<T> type,
                                                                                                final ToDoubleBiFunction<T, KineticBlockEntity> speed,
                                                                                                final ToDoubleBiFunction<T, KineticBlockEntity> extraSpeed) {
        assertExtraKineticsSpeed(helper, pos, type, speed, extraSpeed, 1e-6);
    }

    /**
     * Upstream's {@code assertExtraKineticsSpeed(helper, pos, speed, extraSpeed)}: the block entity's speed and its
     * extra kinetics' speed (absolute values) must match to 1e-6 (upstream ignored its delta parameter here too).
     */
    public static void assertExtraKineticsSpeed(final GameTestHelper helper, final BlockPos pos, final double speed, final double extraSpeed) {
        final KineticBlockEntity be = kinetic(helper, pos);
        if (!(be instanceof final ExtraKinetics extra)) {
            helper.fail(Component.literal("Expected a block entity with extra kinetics, got " + be.getType().builtInRegistryHolder().getRegisteredName()), pos);
            return;
        }

        final KineticBlockEntity extraKinetics = extra.getExtraKinetics();
        if (Math.abs(Math.abs(be.getSpeed()) - Math.abs(speed)) >= 1e-6) {
            helper.fail(Component.literal("Expected %.2f speed, got %.2f".formatted(Math.abs(speed), Math.abs(be.getSpeed()))), pos);
        }
        if (extraKinetics == null) {
            helper.fail(Component.literal("Expected extra kinetics, got null"), pos);
            return;
        }
        if (Math.abs(Math.abs(extraKinetics.getSpeed()) - Math.abs(extraSpeed)) >= 1e-6) {
            helper.fail(Component.literal("Expected %.2f extra kinetics speed, got %.2f".formatted(Math.abs(extraSpeed), Math.abs(extraKinetics.getSpeed()))), pos);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Building ships

    /**
     * Places a creative motor facing {@code facing} and sets its generated speed [RPM].
     */
    public static void placeMotor(final GameTestHelper helper, final BlockPos localPos, final Direction facing, final int rpm) {
        helper.setBlock(localPos, AllBlocks.CREATIVE_MOTOR.defaultBlockState().setValue(CreativeMotorBlock.FACING, absoluteFacing(helper, facing)));
        helper.getBlockEntity(localPos, CreativeMotorBlockEntity.class).generatedSpeed.setValue(rpm);
    }

    /**
     * @return a test-local horizontal direction in world space (vertical ones are unchanged)
     */
    public static Direction absoluteFacing(final GameTestHelper helper, final Direction localFacing) {
        return localFacing.getAxis().isVertical() ? localFacing : helper.getTestRotation().rotate(localFacing);
    }

    /**
     * Glues the local box {@code localMin}..{@code localMax} (inclusive) together with one super glue entity, as a
     * player dragging super glue over it would. Every pair of adjacent blocks inside the box is then attached.
     */
    public static SuperGlueEntity glue(final GameTestHelper helper, final BlockPos localMin, final BlockPos localMax) {
        final AABB box = SuperGlueEntity.span(helper.absolutePos(localMin), helper.absolutePos(localMax));
        final SuperGlueEntity glue = new SuperGlueEntity(helper.getLevel(), box);
        helper.getLevel().addFreshEntity(glue);
        return glue;
    }

    /**
     * Places a physics assembler on top of the block below {@code localPos} (it assembles that block and everything
     * attached to it, and rides along).
     */
    public static void placeAssembler(final GameTestHelper helper, final BlockPos localPos) {
        helper.setBlock(localPos, SimBlocks.PHYSICS_ASSEMBLER.defaultBlockState()
                .setValue(FaceAttachedHorizontalDirectionalBlock.FACE, AttachFace.FLOOR)
                .setValue(FaceAttachedHorizontalDirectionalBlock.FACING, absoluteFacing(helper, Direction.NORTH)));
    }

    /**
     * Uses a physics assembler at world position {@code worldPos} (in the world or in a sub-level's plot) the way a
     * player does: holding "use" on it sends an {@link AssemblePacket}, whose server handler checks the player's reach
     * and calls {@link PhysicsAssemblerBlockEntity#assembleOrDisassemble()}. The packet handler runs here with a fake
     * player standing one block above the assembler (in world space when the assembler is on a sub-level).
     */
    public static void useAssembler(final GameTestHelper helper, final BlockPos worldPos) {
        final FakePlayer player = FakePlayer.get(helper.getLevel(), ASSEMBLING_PLAYER);
        final Vec3 standAt = projectedCenter(helper, worldPos).add(0.0, 1.0, 0.0);
        player.setPos(standAt.x, standAt.y, standAt.z);

        if (!player.isWithinBlockInteractionRange(worldPos, 4)) {
            helper.fail(Component.literal("Assembler at %s is out of the player's reach from %s".formatted(worldPos.toShortString(), standAt)));
        }

        new AssemblePacket(worldPos).handle(new FakePlayerPacketContext(player));
    }

    /**
     * Uses the physics assembler at a test-local world position, see {@link #useAssembler}.
     */
    public static void useAssemblerAt(final GameTestHelper helper, final BlockPos localPos) {
        useAssembler(helper, helper.absolutePos(localPos));
    }

    /**
     * @return the world position of a block's centre, projected out of its sub-level when it is in a plot
     */
    public static Vec3 projectedCenter(final GameTestHelper helper, final BlockPos worldPos) {
        final Vec3 center = Vec3.atCenterOf(worldPos);
        final SubLevel subLevel = Sable.HELPER.getContaining(helper.getLevel(), worldPos);
        return subLevel == null ? center : subLevel.logicalPose().transformPosition(center);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Sub-level lookups

    /**
     * @return the only sub-level intersecting the test; fails the test if there is none or more than one
     */
    public static ServerSubLevel onlySubLevel(final GameTestHelper helper) {
        final List<ServerSubLevel> subLevels = subLevelsInTest(helper);
        if (subLevels.size() != 1) {
            throw helper.assertionException(Component.literal("Expected 1 sub-level in the test, found " + subLevels.size()));
        }
        return subLevels.getFirst();
    }

    /**
     * @return the plot positions of every block in the sub-level matching {@code predicate}
     */
    public static List<BlockPos> findInPlot(final SubLevel subLevel, final Predicate<BlockState> predicate) {
        final List<BlockPos> found = new ArrayList<>();
        final BoundingBox3ic bounds = subLevel.getPlot().getBoundingBox();
        if (bounds == BoundingBox3i.EMPTY || bounds.volume() <= 0) {
            return found;
        }

        for (final BlockPos pos : BlockPos.betweenClosed(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ())) {
            if (predicate.test(subLevel.getLevel().getBlockState(pos))) {
                found.add(pos.immutable());
            }
        }
        return found;
    }

    /**
     * @return the plot position of the one block in the sub-level matching {@code predicate}; fails the test otherwise
     */
    public static BlockPos findOneInPlot(final GameTestHelper helper, final SubLevel subLevel, final Predicate<BlockState> predicate, final String what) {
        final List<BlockPos> found = findInPlot(subLevel, predicate);
        if (found.size() != 1) {
            throw helper.assertionException(Component.literal("Expected 1 %s in the sub-level, found %d (%s)".formatted(what, found.size(), describePlot(subLevel))));
        }
        return found.getFirst();
    }

    /**
     * @return the block entity at a plot position, or null
     */
    public static <T extends BlockEntity> @Nullable T plotBlockEntity(final SubLevel subLevel, final BlockPos plotPos, final Class<T> type) {
        final BlockEntity be = subLevel.getLevel().getBlockEntity(plotPos);
        return type.isInstance(be) ? type.cast(be) : null;
    }

    /**
     * @return the item entities inside the test bounds (an assembly must not drop anything)
     */
    public static List<ItemEntity> itemsInTest(final GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds(), ItemEntity::isAlive);
    }

    /**
     * @return the super glue entities inside the test bounds
     */
    public static List<SuperGlueEntity> gluesInTest(final GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(SuperGlueEntity.class, helper.getBounds(), SuperGlueEntity::isAlive);
    }

    public static String format(final double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    /**
     * A server packet context for running a packet handler with a fake player (nothing is sent back).
     */
    private record FakePlayerPacketContext(ServerPlayer player) implements ServerPacketContext {
        @Override
        public Packet<?> createPacket(final CustomPacketPayload payload) {
            throw new UnsupportedOperationException("The GameTest fake player has no connection");
        }

        @Override
        public void sendPacket(final Packet<?> packet, final @Nullable ChannelFutureListener callback) {
        }

        @Override
        public void disconnect(final Component disconnectReason) {
        }
    }
}
