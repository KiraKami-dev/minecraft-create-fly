package dev.simulated_team.simulated.content.blocks.redstone.modulating_receiver;


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zurrtum.create.catnip.math.AngleHelper;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import com.zurrtum.create.client.flywheel.api.visualization.VisualizationManager;
import com.zurrtum.create.client.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.simulated_team.simulated.index.SimPartialModels;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import static dev.simulated_team.simulated.content.blocks.redstone.modulating_receiver.ModulatingLinkVisual.MAX_DISTANCE;
import static dev.simulated_team.simulated.content.blocks.redstone.modulating_receiver.ModulatingLinkVisual.SMOOTHING;


public class ModulatingLinkedReceiverRenderer extends SmartBlockEntityRenderer<ModulatingLinkedReceiverBlockEntity, ModulatingLinkedReceiverRenderer.ModulatingLinkRenderState> {

    public ModulatingLinkedReceiverRenderer(final BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ModulatingLinkRenderState createRenderState() {
        return new ModulatingLinkRenderState();
    }

    @Override
    public void extractRenderState(final ModulatingLinkedReceiverBlockEntity be, final ModulatingLinkRenderState state, final float tickProgress,
                                   final Vec3 cameraPos, @Nullable final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        super.extractRenderState(be, state, tickProgress, cameraPos, crumblingOverlay);

        state.plate = null;
        if (VisualizationManager.supportsVisualization(be.getLevel())) {
            return;
        }

        state.facing = state.blockState.getValue(BlockStateProperties.FACING);
        state.minPos = 5.5f * ((be.minRange - 1) * (SMOOTHING + MAX_DISTANCE - 1)) / ((MAX_DISTANCE - 1) * (SMOOTHING + be.minRange - 1));
        state.maxPos = 5.5f * ((be.maxRange - 1) * (SMOOTHING + MAX_DISTANCE - 1)) / ((MAX_DISTANCE - 1) * (SMOOTHING + be.maxRange - 1));
        state.plate = CachedBuffers.partial(SimPartialModels.MODULATING_RECEIVER_PLATE, state.blockState);
    }

    @Override
    public void submit(final ModulatingLinkRenderState state, final PoseStack ms, final SubmitNodeCollector queue, final CameraRenderState cameraState) {
        super.submit(state, ms, queue, cameraState);

        if (state.plate != null) {
            queue.submitCustomGeometry(ms, RenderTypes.solidMovingBlock(), state);
        }
    }

    public static class ModulatingLinkRenderState extends SmartRenderState implements SubmitNodeCollector.CustomGeometryRenderer {
        public SuperByteBuffer plate;
        public Direction facing;
        public float minPos;
        public float maxPos;

        @Override
        public void render(final PoseStack.Pose pose, final VertexConsumer vertexConsumer) {
            final Vec3 pixelNormal = new Vec3(this.facing.step()).scale(1 / 16.0);

            // bottom plate, then top plate
            for (int i = 0; i < 2; i++) {
                final boolean bottom = i == 0;
                if (bottom) {
                    this.plate.translate(pixelNormal.scale(this.minPos));
                } else {
                    this.plate.translate(pixelNormal.scale(0.5 + this.maxPos));
                }

                if (this.facing.getAxis().isHorizontal()) {
                    this.plate.rotateCentered(AngleHelper.rad(AngleHelper.horizontalAngle(this.facing.getOpposite())), Direction.UP);
                }

                this.plate.rotateCentered(AngleHelper.rad(-90 - AngleHelper.verticalAngle(this.facing)), Direction.EAST);

                this.plate.light(this.lightCoords);
                this.plate.renderInto(pose, vertexConsumer);
            }
        }
    }
}
