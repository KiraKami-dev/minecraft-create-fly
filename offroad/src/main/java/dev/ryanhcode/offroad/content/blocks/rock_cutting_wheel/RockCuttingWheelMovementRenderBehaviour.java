package dev.ryanhcode.offroad.content.blocks.rock_cutting_wheel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zurrtum.create.catnip.animation.LerpedFloat;
import com.zurrtum.create.client.api.behaviour.movement.MovementRenderBehaviour;
import com.zurrtum.create.client.api.behaviour.movement.MovementRenderState;
import com.zurrtum.create.client.catnip.animation.AnimationTickHolder;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import com.zurrtum.create.client.content.contraptions.render.ActorVisual;
import com.zurrtum.create.client.flywheel.api.visualization.VisualizationContext;
import com.zurrtum.create.client.flywheel.api.visualization.VisualizationManager;
import com.zurrtum.create.client.foundation.virtualWorld.VirtualRenderWorld;
import com.zurrtum.create.content.contraptions.behaviour.MovementContext;
import dev.ryanhcode.offroad.index.OffroadPartialModels;
import dev.simulated_team.simulated.content.blocks.util.AbstractDirectionalAxisBlock;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;

/**
 * Client half of {@link RockCuttingWheelActor}: the spinning wheel on a borehead contraption (upstream:
 * {@code renderInContraption} / {@code createVisual} of the actor, and {@code RockCuttingWheelRenderer#renderInContraption}).
 */
public class RockCuttingWheelMovementRenderBehaviour implements MovementRenderBehaviour {

    @Override
    public MovementRenderState getRenderState(final Vec3 camera, final Font textRenderer, final MovementContext context,
                                              final VirtualRenderWorld renderWorld, final Matrix4f worldMatrix4f) {
        if (VisualizationManager.supportsVisualization(context.world)) {
            return null;
        }

        if (context.temporaryData == null) {
            context.temporaryData = LerpedFloat.angular();
        }

        final BlockState state = context.state;
        final RockCuttingWheelMovementRenderState renderState = new RockCuttingWheelMovementRenderState(context.localPos);
        renderState.wheel = CachedBuffers.partial(OffroadPartialModels.ROCK_CUTTING_WHEEL_WHEEL, state);
        renderState.state = state;
        renderState.spin = ((LerpedFloat) context.temporaryData).getValue(AnimationTickHolder.getPartialTicks(context.world));
        renderState.light = LevelRenderer.getLightColor(renderWorld, context.localPos);
        renderState.world = context.world;
        renderState.worldMatrix4f = worldMatrix4f;
        return renderState;
    }

    @Override
    public @Nullable ActorVisual createVisual(final VisualizationContext visualizationContext, final VirtualRenderWorld simulationWorld, final MovementContext context) {
        if (context.temporaryData == null) {
            context.temporaryData = LerpedFloat.angular();
        }

        return new RockCuttingWheelActorVisual(visualizationContext, simulationWorld, context);
    }

    public static class RockCuttingWheelMovementRenderState extends MovementRenderState implements SubmitNodeCollector.CustomGeometryRenderer {
        public SuperByteBuffer wheel;
        public BlockState state;
        public float spin;
        public int light;
        public Level world;
        public Matrix4f worldMatrix4f;

        public RockCuttingWheelMovementRenderState(final BlockPos pos) {
            super(pos);
        }

        @Override
        public void render(final PoseStack matrices, final SubmitNodeCollector queue) {
            queue.submitCustomGeometry(matrices, RenderTypes.solidMovingBlock(), this);
        }

        @Override
        public void render(final PoseStack.Pose matricesEntry, final VertexConsumer vertexConsumer) {
            RockCuttingWheelRenderer.transformBuffer(this.state.getValue(FACING), this.state.getValue(AbstractDirectionalAxisBlock.AXIS_ALONG_FIRST_COORDINATE), this.wheel);
            this.wheel.rotateYCenteredDegrees(this.spin);
            this.wheel.light(this.light)
                    .useLevelLight(this.world, this.worldMatrix4f)
                    .renderInto(matricesEntry, vertexConsumer);
        }
    }
}
