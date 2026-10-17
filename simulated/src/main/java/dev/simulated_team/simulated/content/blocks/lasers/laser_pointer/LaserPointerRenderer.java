package dev.simulated_team.simulated.content.blocks.lasers.laser_pointer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import dev.simulated_team.simulated.content.blocks.lasers.AbstractLaserRenderer;
import dev.simulated_team.simulated.content.blocks.lasers.LaserBehaviour;
import dev.simulated_team.simulated.index.SimRenderTypes;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.util.SimColors;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector4f;

import java.awt.*;

public class LaserPointerRenderer extends AbstractLaserRenderer<LaserPointerBlockEntity, LaserPointerRenderer.LaserPointerRenderState> {

    public LaserPointerRenderer(final BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public LaserPointerRenderState createRenderState() {
        return new LaserPointerRenderState();
    }

    @Override
    public void extractRenderState(final LaserPointerBlockEntity blockEntity, final LaserPointerRenderState state, final float tickProgress,
                                   final Vec3 cameraPos, @Nullable final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        super.extractRenderState(blockEntity, state, tickProgress, cameraPos, crumblingOverlay);

        final Vector4f colors = this.getColors(blockEntity, tickProgress);
        final boolean isDarkerThanDark = colors.x == 0 && colors.y == 0 && colors.z == 0;
        if (blockEntity.shouldCast() && !isDarkerThanDark) {
            state.lens = CachedBuffers.partial(SimPartialModels.LASER_POINTER_LENS_ON, state.blockState);
        } else {
            state.lens = CachedBuffers.partial(SimPartialModels.LASER_POINTER_LENS_OFF, state.blockState);
        }
        state.facing = state.blockState.getValue(LaserPointerBlock.FACING);
        state.lensLight = blockEntity.shouldCast() ? LightTexture.FULL_BRIGHT : state.lightCoords;
        state.lensColor = colors;

        // only draw non-black lasers
        if (isDarkerThanDark) {
            state.laser.visible = false;
        }
    }

    @Override
    public void submit(final LaserPointerRenderState state, final PoseStack pose, final SubmitNodeCollector queue, final CameraRenderState cameraState) {
        queue.submitCustomGeometry(pose, SimRenderTypes.lens(), state);
        super.submit(state, pose, queue, cameraState);
    }

    @Override
    public float getLaserScale(final LaserBehaviour laser) {
        return 0.48f;
    }

    @Override
    public Vector4f getColors(final LaserPointerBlockEntity blockEntity, final float partialTicks) {
        Color c = new Color(blockEntity.laserColor);
        if (blockEntity.isRainbow()) {
            final Vector3d baseLCh = SimColors.LabToLCh(SimColors.toOklab(c));
            final float t;
            if (blockEntity.isVirtual()) {
                t = (float) ((Util.getMillis() % 5000) * 2 * Math.PI / 5000);
            } else {
                final long timeOff = blockEntity.getLevel().getGameTime();
                t = (float) (((timeOff) % 100 + partialTicks) * 2 * Math.PI / 100f);
            }
            c = SimColors.LChOklab(0.8f, 0.3f, (float) (t + baseLCh.z()));
        }
        // 25% opacity at strongest
        return new Vector4f(c.getRed() / 255f, c.getBlue() / 255f, c.getGreen() / 255f, blockEntity.getPower() / 60f);
    }

    public static class LaserPointerRenderState extends LaserRenderState implements SubmitNodeCollector.CustomGeometryRenderer {
        public SuperByteBuffer lens;
        public Direction facing;
        public int lensLight;
        public Vector4f lensColor;

        @Override
        public void render(final PoseStack.Pose pose, final VertexConsumer vertexConsumer) {
            this.lens.translate(0.5, 0.5, 0.5);
            this.lens.rotateToFace(this.facing);
            this.lens.translate(-0.5, -0.5, -0.5);
            this.lens.light(this.lensLight);
            this.lens.disableDiffuse();
            this.lens.color((int) (this.lensColor.x * 255), (int) (this.lensColor.z * 255), (int) (this.lensColor.y * 255), 255);
            this.lens.renderInto(pose, vertexConsumer);
        }
    }
}
