package dev.eriksonn.aeronautics.mixin.levitite;

import dev.eriksonn.aeronautics.index.AeroTags;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public class BlockMixin {

    /**
     * Faces between levitite and other blocks are always drawn (unless the block skips rendering against its
     * neighbour). Upstream wrapped {@code Shapes.joinIsNotEmpty} in the occlusion check; on 1.21.11 the occlusion cache
     * is keyed by shape pairs only and full block neighbours return early, so the decision is made up front instead.
     */
    @Inject(method = "shouldRenderFace", at = @At("HEAD"), cancellable = true)
    private static void aeronautics$renderLevititeFaces(final BlockState blockState, final BlockState neighborState, final Direction direction, final CallbackInfoReturnable<Boolean> cir) {
        final boolean l1 = blockState.is(AeroTags.BlockTags.LEVITITE);
        final boolean l2 = neighborState.is(AeroTags.BlockTags.LEVITITE);

        if ((l1 ^ l2) && !blockState.skipRendering(neighborState, direction)) {
            cir.setReturnValue(true);
        }
    }
}
