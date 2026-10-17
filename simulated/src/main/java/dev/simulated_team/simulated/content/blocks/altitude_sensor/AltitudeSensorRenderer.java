package dev.simulated_team.simulated.content.blocks.altitude_sensor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import com.zurrtum.create.client.flywheel.lib.model.baked.PartialModel;
import com.zurrtum.create.client.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.util.SimColors;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;


public class AltitudeSensorRenderer extends SmartBlockEntityRenderer<AltitudeSensorBlockEntity, AltitudeSensorRenderer.AltitudeSensorRenderState> {
    public AltitudeSensorRenderer(final BlockEntityRendererProvider.Context context) {
        super(context);
    }

    public static float calculateLinearDial(final float minHeight, final float maxHeight, final float height) {
        final float fraction = (height - minHeight) / (maxHeight - minHeight);
        return Math.min(Math.max(fraction, 0), 1);
    }

    @Override
    public AltitudeSensorRenderState createRenderState() {
        return new AltitudeSensorRenderState();
    }

    @Override
    public void extractRenderState(final AltitudeSensorBlockEntity be, final AltitudeSensorRenderState state, final float partialTicks,
                                   final Vec3 cameraPos, @Nullable final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        super.extractRenderState(be, state, partialTicks, cameraPos, crumblingOverlay);
        state.dial = DialState.create(state.blockState, be.tickCount + partialTicks, be.getValue(), be.getVisualHeight(partialTicks),
                null, null, state.lightCoords);
    }

    @Override
    public void submit(final AltitudeSensorRenderState state, final PoseStack ms, final SubmitNodeCollector queue, final CameraRenderState cameraState) {
        super.submit(state, ms, queue, cameraState);
        queue.submitCustomGeometry(ms, RenderTypes.cutoutMovingBlock(), state.dial);
    }

    public static class AltitudeSensorRenderState extends SmartRenderState {
        public DialState dial;
    }

    /**
     * The case, hand and indicator of a sensor, in the world or on a contraption (then lit by the level through the
     * contraption's world matrix). Shared by {@link AltitudeSensorRenderer} and {@link AltitudeSensorMovementRenderBehaviour}.
     */
    public static class DialState implements SubmitNodeCollector.CustomGeometryRenderer {
        public BlockState blockState;
        public float time;
        public float dialValue;
        public float visualHeight;
        @Nullable
        public Level lightLevel;
        @Nullable
        public Matrix4f worldLight;
        public int light;

        public static DialState create(final BlockState blockState, final float time, final float dialValue, final float visualHeight,
                                       @Nullable final Level lightLevel, @Nullable final Matrix4f worldLight, final int light) {
            final DialState state = new DialState();
            state.blockState = blockState;
            state.time = time;
            state.dialValue = dialValue;
            state.visualHeight = visualHeight;
            state.lightLevel = lightLevel;
            state.worldLight = worldLight;
            state.light = light;
            return state;
        }

        @Override
        public void render(final PoseStack.Pose pose, final VertexConsumer vb) {
            final SuperByteBuffer indicator = CachedBuffers.partial(SimPartialModels.ALTITUDE_SENSOR_INDICATOR, this.blockState);

            PartialModel box = SimPartialModels.ALTITUDE_SENSOR_LINEAR_CASE;
            PartialModel dial = SimPartialModels.ALTITUDE_SENSOR_LINEAR_HAND;
            final boolean isRadial = this.blockState.getValue(AltitudeSensorBlock.DIAL) == AltitudeSensorBlock.FaceType.RADIAL;

            if (isRadial) {
                box = SimPartialModels.ALTITUDE_SENSOR_RADIAL_CASE;
                dial = SimPartialModels.ALTITUDE_SENSOR_RADIAL_HAND;
            }

            final SuperByteBuffer face = CachedBuffers.partial(box, this.blockState);
            final SuperByteBuffer dialBuffer = CachedBuffers.partial(dial, this.blockState);

            final Direction direction = this.blockState.getValue(HORIZONTAL_FACING);

            if (isRadial) {
                dialBuffer.rotateCentered(-(float) (this.visualHeight * Math.PI / 2.0), direction);
            } else {
                dialBuffer.translate(0, (this.dialValue * 8f - 4f) / 16f, 0);
            }

            final AttachFace attachFace = this.blockState.getValue(AltitudeSensorBlock.FACE);
            final float attachFaceAngle = attachFace == AttachFace.WALL ? 90 : attachFace == AttachFace.CEILING ? 180 : 0;

            final float wobbleAngle = (float) (-Math.sin(this.time * 0.8) * Math.exp(-this.time / 3.5)) * 0.7f;

            final float yRot = !direction.getAxis().equals(Direction.Axis.Z) ?
                    (float) Math.toRadians(this.blockState.getValue(HORIZONTAL_FACING).getOpposite().toYRot()) :
                    (float) Math.toRadians(this.blockState.getValue(HORIZONTAL_FACING).toYRot());

            face.rotateCentered((float) (yRot + Math.PI), Direction.UP).rotateCentered(wobbleAngle, Direction.WEST);
            dialBuffer.rotateCentered((float) (yRot + Math.PI), Direction.UP).rotateCentered(wobbleAngle, Direction.WEST);
            indicator.rotateCentered((float) (yRot + Math.PI), Direction.UP).rotateCentered((float) Math.toRadians(attachFaceAngle), Direction.WEST);

            if (this.worldLight != null && this.lightLevel != null) {
                face.useLevelLight(this.lightLevel, new Matrix4f(this.worldLight));
                dialBuffer.useLevelLight(this.lightLevel, new Matrix4f(this.worldLight));
                indicator.useLevelLight(this.lightLevel, new Matrix4f(this.worldLight));
            }
            face.light(this.light);
            dialBuffer.light(this.light);
            indicator.light(this.light);

            final int color = SimColors.redstone(this.dialValue);
            indicator.color(color);

            face.renderInto(pose, vb);
            dialBuffer.renderInto(pose, vb);
            indicator.renderInto(pose, vb);
        }
    }
}
