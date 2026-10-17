package dev.simulated_team.simulated.content.blocks.torsion_spring;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zurrtum.create.catnip.math.AngleHelper;
import com.zurrtum.create.catnip.theme.Color;
import com.zurrtum.create.client.AllPartialModels;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import com.zurrtum.create.client.content.kinetics.base.KineticBlockEntityRenderer;
import com.zurrtum.create.content.contraptions.bearing.BearingBlock;
import com.zurrtum.create.content.kinetics.base.KineticBlockEntity;
import dev.simulated_team.simulated.index.SimPartialModels;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class TorsionSpringRenderer extends KineticBlockEntityRenderer<TorsionSpringBlockEntity, TorsionSpringRenderer.TorsionSpringRenderState> {
    public TorsionSpringRenderer(final BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public TorsionSpringRenderState createRenderState() {
        return new TorsionSpringRenderState();
    }

    @Override
    public void extractRenderState(final TorsionSpringBlockEntity be, final TorsionSpringRenderState state, final float partialTicks,
                                   final Vec3 cameraPos, @Nullable final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        super.extractRenderState(be, state, partialTicks, cameraPos, crumblingOverlay);
        if (state.support) {
            return;
        }

        final Direction facing = state.blockState.getValue(TorsionSpringBlock.FACING);

        state.spring = CachedBuffers.partial(SimPartialModels.TORSION_SPRING, state.blockState);
        state.springAngle = Mth.DEG_TO_RAD * be.interpolatedSpring(partialTicks);
        state.springYRot = facing.getAxis().isHorizontal() ? AngleHelper.rad(AngleHelper.horizontalAngle(facing.getOpposite())) : Float.NaN;
        state.springXRot = AngleHelper.rad(-90 - AngleHelper.verticalAngle(facing));

        final KineticBlockEntity output = be.getExtraKinetics();
        state.shaftOut = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state.blockState, facing);
        state.shaftOutAngle = getAngleForBe(output, state.blockPos, facing.getAxis());
        state.shaftOutColor = getColor(output);
    }

    @Override
    protected RenderType getRenderType(final TorsionSpringBlockEntity be, final BlockState state) {
        return RenderTypes.solidMovingBlock();
    }

    @Override
    protected SuperByteBuffer getRotatedModel(final TorsionSpringBlockEntity be, final TorsionSpringRenderState state) {
        return CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state.blockState, state.blockState
                .getValue(BearingBlock.FACING)
                .getOpposite());
    }

    public static class TorsionSpringRenderState extends KineticRenderState {
        public SuperByteBuffer spring;
        public float springAngle;
        public float springYRot;
        public float springXRot;
        public SuperByteBuffer shaftOut;
        public float shaftOutAngle;
        public Color shaftOutColor;

        @Override
        public void render(final PoseStack.Pose matricesEntry, final VertexConsumer vertexConsumer) {
            super.render(matricesEntry, vertexConsumer);

            this.spring.light(this.lightCoords);
            this.spring.rotateCentered(this.springAngle, this.direction);
            this.spring.color(this.color);
            if (!Float.isNaN(this.springYRot)) {
                this.spring.rotateCentered(this.springYRot, Direction.UP);
            }
            this.spring.rotateCentered(this.springXRot, Direction.EAST);
            this.spring.renderInto(matricesEntry, vertexConsumer);

            this.shaftOut.light(this.lightCoords);
            this.shaftOut.rotateCentered(this.shaftOutAngle, this.direction);
            this.shaftOut.color(this.shaftOutColor);
            this.shaftOut.renderInto(matricesEntry, vertexConsumer);
        }
    }
}
