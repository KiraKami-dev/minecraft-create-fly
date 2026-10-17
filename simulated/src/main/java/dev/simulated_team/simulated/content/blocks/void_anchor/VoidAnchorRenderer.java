package dev.simulated_team.simulated.content.blocks.void_anchor;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.simulated_team.simulated.content.end_sea.EndSeaShadowRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Draws nothing itself: it only reports visible void anchors to the end sea shadow renderer, which now happens when
 * the render state is extracted (was during render).
 */
public class VoidAnchorRenderer implements BlockEntityRenderer<VoidAnchorBlockEntity, BlockEntityRenderState> {
    public VoidAnchorRenderer(final BlockEntityRendererProvider.Context context) {

    }

    @Override
    public BlockEntityRenderState createRenderState() {
        return new BlockEntityRenderState();
    }

    @Override
    public boolean shouldRender(final VoidAnchorBlockEntity blockEntity, final Vec3 vec3) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 512;
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public void extractRenderState(final VoidAnchorBlockEntity blockEntity, final BlockEntityRenderState state, final float partialTicks,
                                   final Vec3 cameraPos, @Nullable final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderState.extractBase(blockEntity, state, crumblingOverlay);
        if (EndSeaShadowRenderer.renderingShadowMap())
            return;

        EndSeaShadowRenderer.addVoidAnchor(blockEntity);
    }

    @Override
    public void submit(final BlockEntityRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState cameraRenderState) {
    }
}
