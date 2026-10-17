package dev.simulated_team.simulated.content.blocks.gimbal_sensor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import com.zurrtum.create.client.flywheel.api.visualization.VisualizationManager;
import com.zurrtum.create.client.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.util.SimColors;
import dev.simulated_team.simulated.util.SimDirectionUtil;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

/**
 * Renders the gimbal when Flywheel is off (see {@link GimbalSensorVisual}), and the filter / link items like any smart
 * block entity (was a {@code SafeBlockEntityRenderer} calling {@code FilteringRenderer#renderOnBlockEntity}).
 */
public class GimbalSensorRenderer extends SmartBlockEntityRenderer<GimbalSensorBlockEntity, GimbalSensorRenderer.GimbalSensorRenderState> {
    public GimbalSensorRenderer(final BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public GimbalSensorRenderState createRenderState() {
        return new GimbalSensorRenderState();
    }

    @Override
    public void extractRenderState(final GimbalSensorBlockEntity be, final GimbalSensorRenderState state, final float partialTicks,
                                   final Vec3 cameraPos, @Nullable final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        super.extractRenderState(be, state, partialTicks, cameraPos, crumblingOverlay);

        state.visible = !VisualizationManager.supportsVisualization(be.getLevel());
        if (!state.visible) {
            return;
        }

        state.indicator = CachedBuffers.partial(SimPartialModels.GIMBAL_SENSOR_INDICATOR, state.blockState);
        for (int i = 0; i < SimDirectionUtil.Y_AXIS_PLANE.length; i++) {
            final float signalStrength = Math.max(be.getPower(SimDirectionUtil.Y_AXIS_PLANE[i]), 0) / 15.0F;
            // Analog indicators (mixes between colors smoothly)
            state.indicatorColors[i] = SimColors.redstone(signalStrength);
        }

        final Quaternionf q = be.getBaseQuaternion();
        be.applyPrimaryQuaternion(q, partialTicks);
        state.gimbalRotation = new Quaternionf(q);
        be.applySecondaryQuaternion(q, partialTicks);
        state.compassRotation = new Quaternionf(q);
        be.applyCompassQuaternion(q, partialTicks);
        state.needleRotation = new Quaternionf(q);

        state.gimbal = CachedBuffers.partial(SimPartialModels.GIMBAL_SENSOR_GIMBAL, state.blockState);
        state.compass = CachedBuffers.partial(SimPartialModels.GIMBAL_SENSOR_COMPASS, state.blockState);
        state.needle = CachedBuffers.partial(SimPartialModels.GIMBAL_SENSOR_NEEDLE, state.blockState);
    }

    @Override
    public void submit(final GimbalSensorRenderState state, final PoseStack ms, final SubmitNodeCollector queue, final CameraRenderState cameraState) {
        super.submit(state, ms, queue, cameraState);

        if (state.visible) {
            queue.submitCustomGeometry(ms, RenderTypes.cutoutMovingBlock(), state);
        }
    }

    public static class GimbalSensorRenderState extends SmartRenderState implements SubmitNodeCollector.CustomGeometryRenderer {
        public boolean visible;
        public SuperByteBuffer indicator;
        public final int[] indicatorColors = new int[SimDirectionUtil.Y_AXIS_PLANE.length];
        public SuperByteBuffer gimbal;
        public SuperByteBuffer compass;
        public SuperByteBuffer needle;
        public Quaternionf gimbalRotation;
        public Quaternionf compassRotation;
        public Quaternionf needleRotation;

        @Override
        public void render(final PoseStack.Pose pose, final VertexConsumer vb) {
            // Render Redstone Indicators
            for (int i = 0; i < SimDirectionUtil.Y_AXIS_PLANE.length; i++) {
                final Direction direction = SimDirectionUtil.Y_AXIS_PLANE[i];
                this.indicator.translate(0.5, 0, 0.5);
                this.indicator.rotateToFace(direction);
                this.indicator.translate(0, 0, -0.5);
                this.indicator.light(this.lightCoords)
                        .color(this.indicatorColors[i])
                        .renderInto(pose, vb);
            }

            this.apply(this.gimbal, this.gimbalRotation, pose, vb);
            this.apply(this.compass, this.compassRotation, pose, vb);
            this.apply(this.needle, this.needleRotation, pose, vb);
        }

        private void apply(final SuperByteBuffer buf, final Quaternionf q, final PoseStack.Pose pose, final VertexConsumer vb) {
            buf.rotateCentered(q);
            buf.translate(0.5, 0.5, 0.5);
            buf.light(this.lightCoords).renderInto(pose, vb);
        }
    }
}
