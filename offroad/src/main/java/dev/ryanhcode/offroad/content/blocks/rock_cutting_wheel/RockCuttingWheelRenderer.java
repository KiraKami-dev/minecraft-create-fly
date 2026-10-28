package dev.ryanhcode.offroad.content.blocks.rock_cutting_wheel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import dev.ryanhcode.offroad.index.OffroadPartialModels;
import dev.simulated_team.simulated.content.blocks.util.AbstractDirectionalAxisBlock;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;

/**
 * The placed rock cutting wheel (it only spins in ponder scenes). PORT(1.21.11): render state + submit; the contraption
 * half moved to {@link RockCuttingWheelMovementRenderBehaviour}.
 */
public class RockCuttingWheelRenderer implements BlockEntityRenderer<RockCuttingWheelBlockEntity, RockCuttingWheelRenderer.RockCuttingWheelRenderState> {

    public RockCuttingWheelRenderer(final BlockEntityRendererProvider.Context context) {
    }

    static void transformBuffer(final Direction facing, final boolean alongFirstCoords, final SuperByteBuffer wheel) {
        if ((facing.getAxis() == Direction.Axis.Z || facing.getAxis() == Direction.Axis.Y) ^ alongFirstCoords) {
            wheel.rotateCentered(facing.getRotation())
                    .rotateZCenteredDegrees(90)
                    .rotateXCenteredDegrees(0)
                    .translate(0.625, 0.5, 0);
        } else {
            wheel.rotateCentered(facing.getRotation())
                    .rotateZCenteredDegrees(0)
                    .rotateXCenteredDegrees(90)
                    .translate(0, 0.5, -0.625);
        }
    }

    @Override
    public RockCuttingWheelRenderState createRenderState() {
        return new RockCuttingWheelRenderState();
    }

    @Override
    public void extractRenderState(final RockCuttingWheelBlockEntity blockEntity, final RockCuttingWheelRenderState state, final float partialTicks,
                                   final Vec3 cameraPos, @Nullable final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderState.extractBase(blockEntity, state, crumblingOverlay);
        final BlockState blockState = blockEntity.getBlockState();
        state.wheel = CachedBuffers.partial(OffroadPartialModels.ROCK_CUTTING_WHEEL_WHEEL, blockState);
        state.facing = blockState.getValue(FACING);
        state.alongFirst = blockState.getValue(AbstractDirectionalAxisBlock.AXIS_ALONG_FIRST_COORDINATE);
        state.spin = blockEntity.isVirtual() ? blockEntity.getAnimatedSpeed(partialTicks) : 0;
    }

    @Override
    public void submit(final RockCuttingWheelRenderState state, final PoseStack ms, final SubmitNodeCollector queue, final CameraRenderState cameraState) {
        queue.submitCustomGeometry(ms, RenderTypes.solidMovingBlock(), (pose, buffer) -> {
            transformBuffer(state.facing, state.alongFirst, state.wheel);
            if (state.spin != 0) {
                state.wheel.rotateYCenteredDegrees(state.spin);
            }
            state.wheel.light(state.lightCoords).renderInto(pose, buffer);
        });
    }

    public static class RockCuttingWheelRenderState extends BlockEntityRenderState {
        public SuperByteBuffer wheel;
        public Direction facing;
        public boolean alongFirst;
        public float spin;
    }
}
