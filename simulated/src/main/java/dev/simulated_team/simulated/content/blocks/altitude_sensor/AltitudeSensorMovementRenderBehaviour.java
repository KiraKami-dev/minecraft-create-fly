package dev.simulated_team.simulated.content.blocks.altitude_sensor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zurrtum.create.client.api.behaviour.movement.MovementRenderBehaviour;
import com.zurrtum.create.client.api.behaviour.movement.MovementRenderState;
import com.zurrtum.create.client.catnip.animation.AnimationTickHolder;
import com.zurrtum.create.client.foundation.virtualWorld.VirtualRenderWorld;
import com.zurrtum.create.content.contraptions.behaviour.MovementContext;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3d;

/**
 * Renders the dial of an altitude sensor on a contraption (was {@code AltitudeSensorMovementBehaviour#renderInContraption})
 */
public class AltitudeSensorMovementRenderBehaviour implements MovementRenderBehaviour {

    @Override
    public MovementRenderState getRenderState(final Vec3 camera, final Font textRenderer, final MovementContext context,
                                              final VirtualRenderWorld renderWorld, final Matrix4f worldMatrix4f) {
        final float lowSignal = context.blockEntityData.getFloatOr("low_signal", 0);
        final float highSignal = context.blockEntityData.getFloatOr("high_signal", 0);
        final float partialTicks = AnimationTickHolder.getPartialTicks();

        final float visualHeight;
        if (context.temporaryData instanceof final Tuple<?, ?> heights) {
            visualHeight = ((float) heights.getA()) * (1 - partialTicks) + (float) heights.getB() * partialTicks;
        } else {
            final Vector3d pos = context.position != null ? JOMLConversion.toJOML(context.position) : new Vector3d();
            visualHeight = (float) Sable.HELPER.projectOutOfSubLevel(context.world, pos).y;
        }

        final Level level = context.contraption.entity.level();
        final float y = (float) Mth.map(context.position.y, level.getMinY(), level.getMaxY() + 1, 0.0f, 1.0f);
        final float value = Mth.clampedMap(y, 0.0f, 1.0f, lowSignal, highSignal);

        final AltitudeSensorRenderer.DialState dial = AltitudeSensorRenderer.DialState.create(context.state, 1000 + partialTicks, value, visualHeight,
                context.world, worldMatrix4f, LevelRenderer.getLightColor(renderWorld, context.localPos));
        return new AltitudeSensorMovementRenderState(context.localPos, dial);
    }

    public static class AltitudeSensorMovementRenderState extends MovementRenderState {
        private final AltitudeSensorRenderer.DialState dial;

        public AltitudeSensorMovementRenderState(final BlockPos pos, final AltitudeSensorRenderer.DialState dial) {
            super(pos);
            this.dial = dial;
        }

        @Override
        public void render(final PoseStack matrices, final SubmitNodeCollector queue) {
            queue.submitCustomGeometry(matrices, RenderTypes.cutoutMovingBlock(), this.dial);
        }
    }
}
