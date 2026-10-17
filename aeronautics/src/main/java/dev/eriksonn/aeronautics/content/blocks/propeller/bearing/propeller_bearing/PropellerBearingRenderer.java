package dev.eriksonn.aeronautics.content.blocks.propeller.bearing.propeller_bearing;

import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.content.contraptions.bearing.BearingRenderer;
import dev.eriksonn.aeronautics.index.AeroPartialModels;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Create Fly's bearing renderer (shaft + rotating top, nothing when Flywheel visualizes the bearing) with the
 * propeller bearing plate as the top
 */
public class PropellerBearingRenderer extends BearingRenderer<PropellerBearingBlockEntity> {

    public PropellerBearingRenderer(final BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void extractRenderState(final PropellerBearingBlockEntity be, final BearingRenderState state, final float partialTicks, final Vec3 cameraPos,
                                   @Nullable final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        super.extractRenderState(be, state, partialTicks, cameraPos, crumblingOverlay);
        if (state.support) {
            return;
        }

        state.top = CachedBuffers.partial(AeroPartialModels.BEARING_PLATE, state.blockState);
    }
}
