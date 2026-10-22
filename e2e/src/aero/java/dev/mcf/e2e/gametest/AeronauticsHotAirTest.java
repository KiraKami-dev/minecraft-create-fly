package dev.mcf.e2e.gametest;

import dev.eriksonn.aeronautics.content.blocks.hot_air.hot_air_burner.HotAirBurnerBlock;
import dev.eriksonn.aeronautics.content.blocks.hot_air.hot_air_burner.HotAirBurnerBlockEntity;
import dev.eriksonn.aeronautics.content.blocks.hot_air.steam_vent.SteamVentBlockEntity;
import dev.eriksonn.aeronautics.index.AeroBlocks;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import static dev.mcf.e2e.gametest.SableTestHelper.*;
import static dev.mcf.e2e.gametest.SimulatedTestHelper.*;

/**
 * Hot-air balloons (ROADMAP T2, Aeronautics): a powered hot air burner under an envelope fills it with hot air and the
 * lift (1.5 * g * filled volume * pressure) raises the ship. Built only with {@code -Pe2eAero} (see e2e/build.gradle).
 *
 * <p>Not run yet: Aeronautics does not compile on 1.21.11 so far, so both tests are optional until they have passed
 * once. Gas fills towards min(burner output, capacity) with a time constant of about 180 ticks, balloons are detected
 * on the burner's 20-tick lazy tick.
 */
public final class AeronauticsHotAirTest {

    private static final BlockPos BURNER = new BlockPos(2, 2, 2);
    private static final BlockPos REDSTONE = new BlockPos(1, 2, 2);
    private static final BlockPos ASSEMBLER = new BlockPos(3, 2, 2);

    /**
     * 5x5 plank deck on the floor of the 5x13x5 shaft, burner in the middle, a 5x5 envelope box above (walls y 2..7,
     * lid y 8, open bottom onto the deck): about 51 blocks of balloon volume (lift capacity about 76) for about 58
     * mass units (envelopes weigh 0.25). Everything is glued and assembled with a physics assembler.
     */
    private static void buildBalloon(final GameTestHelper helper, final boolean powered) {
        final BlockState envelope = AeroBlocks.WHITE_ENVELOPE_BLOCK.defaultBlockState();
        fill(helper, new BlockPos(0, 1, 0), new BlockPos(4, 1, 4), Blocks.OAK_PLANKS.defaultBlockState());
        for (final BlockPos pos : BlockPos.betweenClosed(0, 2, 0, 4, 7, 4)) {
            if (pos.getX() == 0 || pos.getX() == 4 || pos.getZ() == 0 || pos.getZ() == 4) {
                helper.setBlock(pos, envelope);
            }
        }
        fill(helper, new BlockPos(0, 8, 0), new BlockPos(4, 8, 4), envelope);
        helper.setBlock(BURNER, AeroBlocks.HOT_AIR_BURNER);
        if (powered) {
            // the burner reads the best neighbour signal; the power source has to ride on the same ship
            helper.setBlock(REDSTONE, Blocks.REDSTONE_BLOCK);
        }
        placeAssembler(helper, ASSEMBLER);
        glue(helper, new BlockPos(0, 1, 0), new BlockPos(4, 8, 4));
    }

    /**
     * A powered burner fills the envelope and the balloon leaves the floor within 30 s, rising at a bounded rate.
     */
    @GameTest(structure = "sable:physicstest.gravity", maxTicks = 800, required = false)
    public void poweredBurnerLiftsBalloon(final GameTestHelper helper) {
        buildBalloon(helper, true);
        final SubLevelProbe[] probe = new SubLevelProbe[1];

        helper.startSequence()
                .thenExecuteAfter(1, () -> probe[0] = new SubLevelProbe(helper, assembleWithAssembler(helper, ASSEMBLER)).sampleEveryTick())
                .thenWaitUntil(() -> {
                    if (probe(helper, probe).bottom() < 1.5) {
                        throw helper.assertionException(Component.literal("The balloon has not left the floor: " + probe[0]));
                    }
                })
                .thenExecute(() -> {
                    // envelope drag limits the climb; anything near free-fall speeds means the lift is not gas-limited
                    if (probe[0].maxSpeed() > 5.0) {
                        helper.fail("The balloon climbs implausibly fast: " + probe[0]);
                    }
                })
                .thenSucceed();
    }

    /**
     * Control: the same balloon with an unpowered burner stays on the floor.
     */
    @GameTest(structure = "sable:physicstest.gravity", maxTicks = 400, required = false)
    public void unpoweredBurnerStaysDown(final GameTestHelper helper) {
        buildBalloon(helper, false);
        final ServerSubLevel[] ship = new ServerSubLevel[1];
        final SubLevelProbe[] probe = new SubLevelProbe[1];

        helper.startSequence()
                .thenExecuteAfter(1, () -> {
                    ship[0] = assembleWithAssembler(helper, ASSEMBLER);
                    probe[0] = new SubLevelProbe(helper, ship[0]).sampleEveryTick();
                })
                .thenIdle(300)
                .thenExecute(() -> {
                    if (Math.abs(probe(helper, probe).bottom() - 1.0) > 0.1) {
                        helper.fail("The unpowered balloon left the floor: " + probe[0]);
                    }
                })
                .thenSucceed();
    }

    /**
     * Required: a powered fire burner and a powered steam vent with no envelope above them run their balloon logic
     * ({@code tickBalloonLogic} -> {@code tryCreateBalloon}) on their first tick and every lazy tick without throwing.
     * This is the path of the release-jar {@code AbstractMethodError} on {@code getLevel()}; in dev it guards the
     * balloon logic itself, {@link RemapSafetyTest} guards the remapping.
     */
    @GameTest(structure = "sable:physicstest.gravity", maxTicks = 200)
    public void poweredBurnerAndVentTickWithoutBalloon(final GameTestHelper helper) {
        final BlockPos vent = new BlockPos(2, 2, 4);
        helper.setBlock(BURNER, AeroBlocks.HOT_AIR_BURNER.defaultBlockState()
                .setValue(HotAirBurnerBlock.POWERED, true)
                .setValue(HotAirBurnerBlock.VARIANT, HotAirBurnerBlock.Variant.FIRE));
        helper.setBlock(REDSTONE, Blocks.REDSTONE_BLOCK);
        helper.setBlock(vent, AeroBlocks.STEAM_VENT);
        helper.setBlock(vent.west(), Blocks.REDSTONE_BLOCK);

        helper.startSequence()
                .thenIdle(100)
                .thenExecute(() -> {
                    final HotAirBurnerBlockEntity burner = helper.getBlockEntity(BURNER, HotAirBurnerBlockEntity.class);
                    if (!burner.canOutputGas()) {
                        helper.fail("The powered burner does not output gas, so its balloon logic never ran");
                    }
                    helper.getBlockEntity(vent, SteamVentBlockEntity.class);
                })
                .thenSucceed();
    }

    private static SubLevelProbe probe(final GameTestHelper helper, final SubLevelProbe[] probe) {
        if (probe[0] == null) {
            throw helper.assertionException(Component.literal("No ship (assembly failed)"));
        }
        return probe[0];
    }
}
