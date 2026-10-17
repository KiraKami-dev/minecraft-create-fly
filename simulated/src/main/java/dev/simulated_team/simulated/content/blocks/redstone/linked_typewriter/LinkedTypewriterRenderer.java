package dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zurrtum.create.catnip.animation.LerpedFloat;
import com.zurrtum.create.catnip.math.AngleHelper;
import com.zurrtum.create.client.catnip.animation.AnimationTickHolder;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import com.zurrtum.create.client.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.simulated_team.simulated.index.SimPartialModels;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Vector;


public class LinkedTypewriterRenderer extends SmartBlockEntityRenderer<LinkedTypewriterBlockEntity, LinkedTypewriterRenderer.TypewriterRenderState> {

    private static final int KEY_COUNT = 14;
    private static final float S = 0.0625F;
    private static final float B = S * -0.75F;

    static Vector<LerpedFloat> keys = new Vector<>(KEY_COUNT);

    public LinkedTypewriterRenderer(final BlockEntityRendererProvider.Context context) {
        super(context);
    }

    static {
        for (int i = 0; i < KEY_COUNT; ++i) {
            keys.add(LerpedFloat.linear().startWithValue(0.0));
        }
    }

    public static void tick() {
        if (Minecraft.getInstance()
                .isPaused())
            return;

        if (LinkedTypewriterInteractionHandler.getMode() == LinkedTypewriterInteractionHandler.Mode.IDLE) {
            return;
        }

        for (int i = 0; i < keys.size(); i++) {
            final LerpedFloat lerpedFloat = keys.get(i);
            lerpedFloat.chase(LinkedTypewriterInteractionHandler.getPressedKeys().contains(i) ? 1 : 0, .4f, LerpedFloat.Chaser.EXP);
            lerpedFloat.tickChaser();
        }
    }

    public static void resetKeys() {
        for (final LerpedFloat key : keys) {
            key.startWithValue(0.0);
        }
    }

    @Override
    public TypewriterRenderState createRenderState() {
        return new TypewriterRenderState();
    }

    @Override
    public void extractRenderState(final LinkedTypewriterBlockEntity be, final TypewriterRenderState state, final float tickProgress,
                                   final Vec3 cameraPos, @Nullable final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        super.extractRenderState(be, state, tickProgress, cameraPos, crumblingOverlay);

        state.key = CachedBuffers.partial(SimPartialModels.LINKED_TYPEWRITER_KEY, state.blockState);
        state.spacebar = CachedBuffers.partial(SimPartialModels.LINKED_TYPEWRITER_KEY_SPACEBAR, state.blockState);
        state.angle = AngleHelper.horizontalAngle(state.blockState.getValue(BlockStateProperties.HORIZONTAL_FACING));

        state.keyLight = state.lightCoords;
        if (LinkedTypewriterInteractionHandler.getMode() == LinkedTypewriterInteractionHandler.Mode.BIND) {
            final int i = (int) Mth.lerp((Mth.sin(AnimationTickHolder.getRenderTime() / 4.0F) + 1.0F) / 2.0F, 5.0F, 15.0F);
            state.keyLight = i << 20;
        }

        final boolean using = Minecraft.getInstance().player != null && be.checkUser(Minecraft.getInstance().player.getUUID());
        for (int index = 0; index < KEY_COUNT; index++) {
            state.depression[index] = using ? B * keys.get(index).getValue(tickProgress) : 0;
        }
    }

    @Override
    public void submit(final TypewriterRenderState state, final PoseStack ms, final SubmitNodeCollector queue, final CameraRenderState cameraState) {
        super.submit(state, ms, queue, cameraState);
        queue.submitCustomGeometry(ms, RenderTypes.cutoutMovingBlock(), state);
    }

    public static class TypewriterRenderState extends SmartRenderState implements SubmitNodeCollector.CustomGeometryRenderer {
        public SuperByteBuffer key;
        public SuperByteBuffer spacebar;
        public float angle;
        public int keyLight;
        public final float[] depression = new float[KEY_COUNT];

        @Override
        public void render(final PoseStack.Pose pose, final VertexConsumer vertexConsumer) {
            int index = 0;

            // Top Row
            for (int i = 0; i < 6; i++) {
                this.renderKey(this.key, pose, vertexConsumer, -7 * S + 2 * S * (i + 1), S, 2 * S, index++);
            }

            // Bottom Row
            for (int i = 0; i < 7; i++) {
                this.renderKey(this.key, pose, vertexConsumer, -8 * S + 2 * S * (i + 1), 0, 4 * S, index++);
            }

            // Space Bar
            this.renderKey(this.spacebar, pose, vertexConsumer, 0, -S, 6 * S, index);
        }

        private void renderKey(final SuperByteBuffer buffer, final PoseStack.Pose pose, final VertexConsumer vertexConsumer,
                               final float x, final float y, final float z, final int index) {
            // Account for block rotation, then move to the key
            buffer.translate(0.5, 4 * S, 0.5)
                    .rotateYDegrees(this.angle)
                    .translate(x, y + this.depression[index], z);
            buffer.light(this.keyLight);
            buffer.renderInto(pose, vertexConsumer);
        }
    }
}
