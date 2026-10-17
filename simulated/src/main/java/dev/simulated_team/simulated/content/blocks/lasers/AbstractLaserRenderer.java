package dev.simulated_team.simulated.content.blocks.lasers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.zurrtum.create.catnip.data.Couple;
import com.zurrtum.create.client.flywheel.lib.transform.TransformStack;
import com.zurrtum.create.client.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.ryanhcode.sable.Sable;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

public abstract class AbstractLaserRenderer<T extends AbstractLaserBlockEntity, S extends AbstractLaserRenderer.LaserRenderState> extends SmartBlockEntityRenderer<T, S> {
    public AbstractLaserRenderer(final BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void extractRenderState(final T blockEntity, final S state, final float tickProgress, final Vec3 cameraPos,
                                   @Nullable final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        super.extractRenderState(blockEntity, state, tickProgress, cameraPos, crumblingOverlay);
        state.laser.visible = false;

        final LaserBehaviour laser = blockEntity.getAllBehaviours().stream().filter(behaviour -> behaviour instanceof LaserBehaviour).map(behaviour -> (LaserBehaviour) behaviour).findFirst().orElse(null);

        if (laser != null && laser.shouldCast()) {
            final Vector4f colors = this.getColors(blockEntity, tickProgress);

            if (colors.w > 0) { // alpha > 0
                state.laser.visible = true;
                state.laser.colors = colors;
                state.laser.facing = blockEntity.getDirection();
                state.laser.scale = this.getLaserScale(laser);
                state.laser.maxLength = laser.getRange();
                state.laser.length = this.getLaserLength(blockEntity.getLevel(), laser);
            }
        }
    }

    @Override
    public void submit(final S state, final PoseStack pose, final SubmitNodeCollector queue, final CameraRenderState cameraState) {
        super.submit(state, pose, queue, cameraState);

        if (state.laser.visible) {
            pose.pushPose();
            transformPose(state.laser, pose);
            queue.submitCustomGeometry(pose, LaserRenderTypes.laser(), state.laser);
            pose.popPose();
        }
    }

    public abstract Vector4f getColors(T blockEntity, float partialTicks);

    public float getLaserLength(final Level level, final LaserBehaviour laser) {
        float laserRange = laser.getRange();

        final HitResult hr = this.getRenderedHitResult(laser);
        final Couple<Vec3> positions = laser.getLaserPositions().get();
        if (hr != null && !hr.getType().equals(HitResult.Type.MISS)) {
            Vec3 hitPos = hr.getLocation();
            if (laser.getVirtualHitPos() != Vec3.ZERO) {
                hitPos = laser.getVirtualHitPos();
            }

            laserRange = (float) Math.sqrt(Sable.HELPER.distanceSquaredWithSubLevels(level, positions.getFirst(), hitPos)) - 0.1f;
        } else if (laser.getVirtualHitPos() != Vec3.ZERO) {
            final Vec3 hitPos = laser.getVirtualHitPos();

            laserRange = (float) Math.sqrt(Sable.HELPER.distanceSquaredWithSubLevels(level, positions.getFirst(), hitPos)) - 0.1f;
        }

        return laserRange;
    }

    public abstract float getLaserScale(final LaserBehaviour laser);

    public HitResult getRenderedHitResult(final LaserBehaviour laser) {
        return laser.getClosestHitResult();
    }

    protected static void transformPose(final LaserState laser, final PoseStack pose) {
        final Direction facing = laser.facing;

        pose.translate(0.5, 0.5, 0.5);

        TransformStack.of(pose)
                .rotate(facing.getRotation())
                .rotateXDegrees(-90)
                .translate(0, 0, 0.5 - 0.0625);

        pose.scale(laser.scale, laser.scale, 1);

        pose.translate(-0.5, -0.5, 0.0);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    public static class LaserRenderState extends SmartRenderState {
        public final LaserState laser = new LaserState();
    }

    /**
     * The beam, drawn as four quads around its axis
     */
    public static class LaserState implements SubmitNodeCollector.CustomGeometryRenderer {
        public boolean visible;
        public Vector4f colors;
        public Direction facing;
        public float scale;
        public float maxLength;
        public float length;

        @Override
        public void render(final PoseStack.Pose pose, final VertexConsumer builder) {
            final float lengthFrac = this.length / this.maxLength;
            final float offset = lengthFrac / 10;
            final float endU = 1f + 1f / this.length; // frag shader uses v > 1 to taper off

            final float red = this.colors.x();
            final float blue = this.colors.y();
            final float green = this.colors.z();
            final float alpha = this.colors.w();
            final float endAlpha = alpha * (1 - lengthFrac);

            final Matrix4f matrix = new Matrix4f(pose.pose());
            final Quaternionf rotationQuat = Axis.ZN.rotationDegrees(90);

            for (int i = 0; i < 4; i++) {

                builder.addVertex(matrix, 0, 0f, 0).setUv(0, endU).setColor(red, green, blue, alpha);
                builder.addVertex(matrix, 1, 0f, 0).setUv(0, endU).setColor(red, green, blue, alpha);
                // offset makes the end of the laser spread out, helping reduce z-fighting
                builder.addVertex(matrix, 1 + offset, -offset, this.length + 0.5f).setUv(endU, endU).setColor(red, green, blue, endAlpha);
                builder.addVertex(matrix, -offset, -offset, this.length + 0.5f).setUv(endU, endU).setColor(red, green, blue, endAlpha);

                matrix.translate(0.5f, 0.5f, 0.5f)
                        .rotate(rotationQuat)
                        .translate(-0.5f, -0.5f, -0.5f);
            }
        }
    }
}
