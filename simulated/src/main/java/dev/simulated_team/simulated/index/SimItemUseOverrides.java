package dev.simulated_team.simulated.index;

import com.zurrtum.create.AllItems;
import com.zurrtum.create.catnip.math.VecHelper;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Port of Create's {@code ItemUseOverrides} (Create Fly has no equivalent): right-clicking one of these blocks always
 * uses the block, even while holding an item or sneaking. Upstream hooked NeoForge's {@code RightClickBlock}; Fabric's
 * {@link UseBlockCallback} fires at the same point on both sides.
 */
public class SimItemUseOverrides {
    private static final Set<Block> OVERRIDES = ConcurrentHashMap.newKeySet();

    public static void addBlock(final Block block) {
        OVERRIDES.add(block);
    }

    public static void register() {
        UseBlockCallback.EVENT.register(SimItemUseOverrides::onBlockActivated);
    }

    private static InteractionResult onBlockActivated(final Player player, final Level level, final InteractionHand hand, final BlockHitResult hitResult) {
        // the callback fires before vanilla's spectator check
        if (player.isSpectator() || player.getItemInHand(hand).is(AllItems.WRENCH)) {
            return InteractionResult.PASS;
        }

        final BlockPos pos = hitResult.getBlockPos();
        final BlockState state = level.getBlockState(pos);
        if (!OVERRIDES.contains(state.getBlock())) {
            return InteractionResult.PASS;
        }

        final BlockHitResult blockTrace = new BlockHitResult(VecHelper.getCenterOf(pos), hitResult.getDirection(), pos, true);
        final InteractionResult result = state.useWithoutItem(level, player, blockTrace);
        return result.consumesAction() ? result : InteractionResult.PASS;
    }
}
