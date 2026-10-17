package dev.simulated_team.simulated.multiloader;

import com.zurrtum.create.foundation.block.RedStoneConnectBlock;
import com.zurrtum.create.foundation.block.WeakPowerControlBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * An interface for {@link net.minecraft.world.level.block.Block Blocks} to implement to implement redstone behaviour on both loaders.
 * <p>
 * On Fabric this plugs into Create Fly's {@link RedStoneConnectBlock} (redstone dust connection, dispatched by its
 * {@code RedstoneWireBlockMixin}) and {@link WeakPowerControlBlock} (whether the block conducts weak power, dispatched by
 * its {@code SignalGetterMixin}). These replace NeoForge's {@code IBlockExtension} hooks the upstream mixin overrode.
 */
public interface CommonRedstoneBlock extends RedStoneConnectBlock, WeakPowerControlBlock {
    /**
     * @param state This blocks state
     * @param level The level. Vanilla's redstone dust connection check has no level, so on Fabric this is an empty level.
     * @param pos The position of this block. On Fabric this is {@link BlockPos#ZERO}, for the same reason.
     * @param direction The direction redstone dust is trying to connect towards (opposite for the face this block points towards the redstone), null if attempting to connect up or down on a diagonal
     * @return True to cause redstone dust to point towards this block
     */
    default boolean commonConnectRedstone(final BlockState state, final BlockGetter level, final BlockPos pos, @Nullable final Direction direction) {
        return false;
    }

    default boolean commonCheckWeakPower(final BlockState state, final SignalGetter level, final BlockPos pos, final Direction side) {
        return false;
    }

    @Override
    default boolean canConnectRedstone(final BlockState state, @Nullable final Direction side) {
        return this.commonConnectRedstone(state, EmptyBlockGetter.INSTANCE, BlockPos.ZERO, side);
    }

    @Override
    default boolean shouldCheckWeakPower(final BlockState state, final SignalGetter level, final BlockPos pos, final Direction side) {
        return this.commonCheckWeakPower(state, level, pos, side);
    }
}
