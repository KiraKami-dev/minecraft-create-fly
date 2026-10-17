package dev.simulated_team.simulated.content.blocks.swivel_bearing.link_block;

import com.zurrtum.create.client.content.kinetics.base.KineticBlockEntityRenderer;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.SwivelBearingBlock;
import dev.simulated_team.simulated.index.SimPartialModels;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class SwivelBearingPlateBlockRenderer extends KineticBlockEntityRenderer<SwivelBearingPlateBlockEntity, KineticBlockEntityRenderer.KineticRenderState> {

    public SwivelBearingPlateBlockRenderer(final BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected SuperByteBuffer getRotatedModel(final SwivelBearingPlateBlockEntity be, final KineticRenderState state) {
        return CachedBuffers.partialFacing(SimPartialModels.SHAFT_SIXTEENTH, state.blockState, state.blockState.getValue(SwivelBearingBlock.FACING));
    }
}
