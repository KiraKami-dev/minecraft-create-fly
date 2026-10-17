package dev.eriksonn.aeronautics.content.blocks.propeller.small.smart_propeller;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zurrtum.create.catnip.math.AngleHelper;
import com.zurrtum.create.client.AllPartialModels;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import com.zurrtum.create.client.flywheel.lib.model.baked.PartialModel;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.SimplePropellerRenderer;
import dev.eriksonn.aeronautics.index.AeroPartialModels;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import static dev.eriksonn.aeronautics.content.blocks.propeller.small.smart_propeller.SmartPropellerBlock.REVERSED;

/**
 * The smart propeller has no Flywheel visual, so unlike the other propellers this renderer always draws (as upstream).
 */
public class SmartPropellerRenderer extends SimplePropellerRenderer<SmartPropellerBlockEntity, SmartPropellerRenderer.SmartPropellerRenderState> {

    public SmartPropellerRenderer(final BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public SmartPropellerRenderState createRenderState() {
        return new SmartPropellerRenderState();
    }

    @Override
    public void extractRenderState(final SmartPropellerBlockEntity be, final SmartPropellerRenderState state, final float partialTicks, final Vec3 cameraPos,
                                   @Nullable final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        state.support = false;
        this.updateBaseRenderState(be, state, be.getLevel(), crumblingOverlay);
        state.model = this.getRotatedModel(be, state);
        state.angle = getAngleForBe(be, state.blockPos, state.axis);

        final BlockState blockState = state.blockState;
        state.horizontal = blockState.getValue(BlockStateProperties.HORIZONTAL_AXIS);
        state.propeller = CachedBuffers.partialFacing(this.getCurrentModel(be), blockState, Direction.UP);
        state.hinge = CachedBuffers.partialFacing(AeroPartialModels.SMART_PROPELLER_HINGE, blockState, Direction.UP);
        state.hingeAngle = be.getLerpedHingeAngle(partialTicks);
        state.propellerAngle = this.getAngle(partialTicks, Direction.UP, be);
        state.propellerColor = getColor(be);
    }

    @Override
    public void submit(final SmartPropellerRenderState state, final PoseStack matrices, final SubmitNodeCollector queue, final CameraRenderState cameraState) {
        // rotating shaft (upstream renderRotatingBuffer)
        queue.submitCustomGeometry(matrices, state.layer, state);
        queue.submitCustomGeometry(matrices, RenderTypes.solidMovingBlock(), state::renderSmartPropeller);
    }

    @Override
    public PartialModel getCurrentModel(final SmartPropellerBlockEntity be) {
        return be.getBlockState().getValue(REVERSED) ? AeroPartialModels.SMART_PROPELLER_REVERSED : AeroPartialModels.SMART_PROPELLER;
    }

    @Override
    protected SuperByteBuffer getRotatedModel(final SmartPropellerBlockEntity be, final SmartPropellerRenderState state) {
        return CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state.blockState, Direction.DOWN);
    }

    public static class SmartPropellerRenderState extends PropellerRenderState {
        public Direction.Axis horizontal;
        public SuperByteBuffer hinge;
        public float hingeAngle;

        public void renderSmartPropeller(final PoseStack.Pose pose, final VertexConsumer vb) {
            final SuperByteBuffer propeller = this.propeller.light(this.lightCoords);
            final SuperByteBuffer hinge = this.hinge.light(this.lightCoords);

            final Direction d = Direction.get(Direction.AxisDirection.NEGATIVE, this.horizontal);

            hinge.rotateCentered(AngleHelper.rad(this.hingeAngle), d.getClockWise());
            propeller.rotateCentered(AngleHelper.rad(this.hingeAngle), d.getClockWise());

            final float factChecked = AngleHelper.rad(AngleHelper.horizontalAngle(d));
            propeller.rotateCentered(factChecked, Direction.UP);
            hinge.rotateCentered(factChecked, Direction.UP);

            // upstream kineticRotationTransform(propeller, be, Axis.Y, angle, light)
            propeller.light(this.lightCoords);
            propeller.rotateCentered(this.propellerAngle, Direction.UP);
            propeller.color(this.propellerColor);

            propeller.translate(0, 10 / 16f, 0);
            propeller.rotateCentered(AngleHelper.rad(90), Direction.EAST);

            hinge.translate(0, -1 / 16f, 0);
            hinge.rotateCentered(AngleHelper.rad(90), Direction.EAST);

            propeller.renderInto(pose, vb);
            hinge.renderInto(pose, vb);
        }
    }
}
