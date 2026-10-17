package dev.simulated_team.simulated.content.blocks.redstone.redstone_inductor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zurrtum.create.catnip.math.AngleHelper;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import com.zurrtum.create.client.flywheel.api.visualization.VisualizationManager;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.util.SimColors;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Renders the colored indicator when Flywheel is off (see {@link RedstoneInductorVisual}).
 * Was a {@code ColoredOverlayBlockEntityRenderer}, which in Create Fly has no hook for the facing rotation.
 */
public class RedstoneInductorRenderer implements BlockEntityRenderer<RedstoneInductorBlockEntity, RedstoneInductorRenderer.InductorRenderState> {

    public RedstoneInductorRenderer(final BlockEntityRendererProvider.Context context) {
    }

    @Override
    public InductorRenderState createRenderState() {
        return new InductorRenderState();
    }

    @Override
    public void extractRenderState(final RedstoneInductorBlockEntity be, final InductorRenderState state, final float tickProgress,
                                   final Vec3 cameraPos, @Nullable final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
        state.model = null;
        if (VisualizationManager.supportsVisualization(be.getLevel())) {
            return;
        }

        state.model = CachedBuffers.partial(SimPartialModels.REDSTONE_INDUCTOR_INDICATOR, state.blockState);
        state.color = this.getColor(be, tickProgress);
        state.facing = state.blockState.getValue(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public void submit(final InductorRenderState state, final PoseStack ms, final SubmitNodeCollector queue, final CameraRenderState cameraState) {
        if (state.model != null) {
            queue.submitCustomGeometry(ms, RenderTypes.cutoutMovingBlock(), state);
        }
    }

    protected int getColor(final RedstoneInductorBlockEntity te, final float partialTicks) {
        final float state = te.getLerpedState(partialTicks);
        return SimColors.redstone(state / 15F);
    }

    public static class InductorRenderState extends BlockEntityRenderState implements SubmitNodeCollector.CustomGeometryRenderer {
        public SuperByteBuffer model;
        public int color;
        public Direction facing;

        @Override
        public void render(final PoseStack.Pose pose, final VertexConsumer vertexConsumer) {
            this.model.color(this.color).light(this.lightCoords);
            this.model.translate(0.5, 0, 0.5);
            this.model.rotateYDegrees(AngleHelper.horizontalAngle(this.facing));
            this.model.renderInto(pose, vertexConsumer);
        }
    }
}
