package dev.simulated_team.simulated.content.blocks.physics_assembler;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zurrtum.create.content.redstone.analogLever.AnalogLeverBlock;
import com.zurrtum.create.client.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.simulated_team.simulated.index.SimPartialModels;
import com.zurrtum.create.catnip.math.AngleHelper;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class PhysicsAssemblerRenderer extends SmartBlockEntityRenderer<PhysicsAssemblerBlockEntity, PhysicsAssemblerRenderer.PhysicsAssemblerRenderState> {

    public PhysicsAssemblerRenderer(final BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public PhysicsAssemblerRenderState createRenderState() {
        return new PhysicsAssemblerRenderState();
    }

    @Override
    public void extractRenderState(final PhysicsAssemblerBlockEntity be, final PhysicsAssemblerRenderState state, final float partialTicks, final Vec3 cameraPos, @Nullable final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        super.extractRenderState(be, state, partialTicks, cameraPos, crumblingOverlay);

        // Render handle
        state.handle = CachedBuffers.partial(SimPartialModels.ASSEMBLER_LEVER, state.blockState);
        state.angle = getRenderAngle(be, partialTicks);
    }

    @Override
    public void submit(final PhysicsAssemblerRenderState state, final PoseStack ms, final SubmitNodeCollector queue, final CameraRenderState cameraState) {
        super.submit(state, ms, queue, cameraState);
        queue.submitCustomGeometry(ms, RenderTypes.solidMovingBlock(), state);
    }

    public static float getRenderAngle(final PhysicsAssemblerBlockEntity be, final float partialTicks) {
        if (!be.isVirtual()) {
            be.initializeLeverPosition();
        }

        return (float) Math.toRadians(be.getClientAngle(partialTicks));
    }

    private static SuperByteBuffer transform(final SuperByteBuffer buffer, final BlockState leverState) {
        final AttachFace face = leverState.getValue(AnalogLeverBlock.FACE);
        final float rX = face == AttachFace.FLOOR ? 0 : face == AttachFace.WALL ? 90 : 180;
        final float rY = AngleHelper.horizontalAngle(leverState.getValue(AnalogLeverBlock.FACING));
        buffer.rotateCentered((float) (rY / 180 * Math.PI), Direction.UP);
        buffer.rotateCentered((float) (rX / 180 * Math.PI), Direction.EAST);
        return buffer;
    }

    public static class PhysicsAssemblerRenderState extends SmartRenderState implements SubmitNodeCollector.CustomGeometryRenderer {
        public SuperByteBuffer handle;
        public float angle;

        @Override
        public void render(final PoseStack.Pose pose, final VertexConsumer vb) {
            transform(this.handle, this.blockState).translate(1 / 2f, 7 / 16f, 1 / 2f)
                    .rotate(this.angle, Direction.EAST)
                    .translate(-1 / 2f, -7 / 16f, -1 / 2f);
            this.handle.light(this.lightCoords)
                    .renderInto(pose, vb);
        }
    }
}
