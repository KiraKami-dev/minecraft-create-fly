package dev.simulated_team.simulated.content.blocks.absorber;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zurrtum.create.catnip.math.AngleHelper;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import com.zurrtum.create.client.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.simulated_team.simulated.index.SimPartialModels;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public class AbsorberRenderer extends SmartBlockEntityRenderer<AbsorberBlockEntity, AbsorberRenderer.AbsorberRenderState> {

    public AbsorberRenderer(final BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public AbsorberRenderState createRenderState() {
        return new AbsorberRenderState();
    }

    @Override
    public void extractRenderState(final AbsorberBlockEntity be, final AbsorberRenderState state, final float partialTicks,
                                   final Vec3 cameraPos, @Nullable final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        super.extractRenderState(be, state, partialTicks, cameraPos, crumblingOverlay);

        final BlockState blockState = state.blockState;

        state.yRot = (float)Math.toRadians(AngleHelper.horizontalAngle(blockState.getValue(AbsorberBlock.HORIZONTAL_FACING))+180);

        float pos = be.animationTimer.getValue(partialTicks);
        final float target = be.animationTimer.getChaseTarget();

        if(target > 0.5) {

            final float fallTime = 0.3f;
            if (pos < fallTime)
                pos = 1f - pos * pos / (fallTime * fallTime);
            else {

                pos = (pos - fallTime) / (1f - fallTime);
                float bounce = (float) (Math.exp(-pos * 4.0) * Math.sin(pos * Math.PI * 3.0));
                final float smoothing = 0.05f;
                bounce = (float) Math.sqrt(bounce * bounce + smoothing * smoothing) - smoothing;
                pos = bounce / 2f;
            }
        }else
        {
            pos = 1-pos;
            final float startVelocity = 2f;
            pos = pos * Mth.lerp(pos,startVelocity,1);
        }

        state.pos = pos;
        state.wet = blockState.getValue(AbsorberBlock.WET);
    }

    @Override
    public void submit(final AbsorberRenderState state, final PoseStack matrices, final SubmitNodeCollector queue, final CameraRenderState cameraState) {
        super.submit(state, matrices, queue, cameraState);
        queue.submitCustomGeometry(matrices, RenderTypes.cutoutMovingBlock(), state);
    }

    public static class AbsorberRenderState extends SmartRenderState implements SubmitNodeCollector.CustomGeometryRenderer {
        public float yRot;
        public float pos;
        public boolean wet;

        @Override
        public void render(final PoseStack.Pose ms, final VertexConsumer vb) {
            final BlockState blockState = this.blockState;
            final int light = this.lightCoords;
            final float pos = this.pos;
            final float yRot = this.yRot;

            final float movementDistance = 8;

            float totalMovement = (1+(1-pos)*movementDistance)/16f;

            final SuperByteBuffer sponge = CachedBuffers.partial(this.wet ? SimPartialModels.ABSORBER_SPONGE_WET :SimPartialModels.ABSORBER_SPONGE_DRY,blockState);

            sponge.translate(0,0.25,0);
            sponge.scale(1,1-pos*movementDistance/9,1);
            sponge.light(light).renderInto(ms,vb);
            final Matrix4f rotationMatrix = new Matrix4f();
            apply(CachedBuffers.partial(SimPartialModels.ABSORBER_HAT,blockState),ms,light,vb,yRot,totalMovement,rotationMatrix);
            totalMovement/=2;
            apply(CachedBuffers.partial(SimPartialModels.ABSORBER_PIVOT,blockState),ms,light,vb,yRot,totalMovement,rotationMatrix);

            float height = totalMovement+0.5f/16; //height from base to pivot
            final float length = 13.8f/32f; //distance from pivot to endpoint of arm
            float width = (float)Math.sqrt(length*length-height*height);
            width /= length;
            height /= length;

            rotationMatrix.m22(width);
            rotationMatrix.m21(height);
            rotationMatrix.m11(width);
            rotationMatrix.m12(-height);

            apply(CachedBuffers.partial(SimPartialModels.ABSORBER_ARM,blockState),ms,light,vb,yRot,totalMovement,rotationMatrix);
            rotationMatrix.m21(-height);
            rotationMatrix.m12(height);
            rotationMatrix.m00(0.98f);
            apply(CachedBuffers.partial(SimPartialModels.ABSORBER_ARM,blockState),ms,light,vb,yRot,totalMovement,rotationMatrix);
        }
    }

    static void apply(final SuperByteBuffer buffer, final PoseStack.Pose ms, final int light, final VertexConsumer vb, final float yRot, final float offset, final Matrix4f rotationMatrix)
    {

        buffer.translate(0.5,0.25+offset,0.5);
        final Matrix4f r = new Matrix4f().rotate(yRot,0,1,0);
        buffer.mulPose(r.mul(rotationMatrix));
        buffer.translate(-0.5,0,-0.5);
        buffer.light(light).renderInto(ms,vb);
    }
}
