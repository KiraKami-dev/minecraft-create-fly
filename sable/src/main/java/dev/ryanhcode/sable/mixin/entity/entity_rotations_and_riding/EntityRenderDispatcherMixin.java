package dev.ryanhcode.sable.mixin.entity.entity_rotations_and_riding;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.util.Mth;
import org.joml.Vector3d;
import dev.ryanhcode.sable.mixinhelpers.camera.camera_rotation.EntitySubLevelRotationHelper;
import dev.ryanhcode.sable.mixinterface.entity.EntityRenderStateExtension;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Rotates entity rendering to match the sub-level's rotation.
 * <p>
 * Since 1.21.11 rendering is split into extracting a render state and submitting it, so the rotation is computed
 * while extracting (where the entity is available) and applied while submitting.
 */
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    /**
     * Moves entities that are inside of (or tracked by) a sub-level to where the sub-level is rendered
     */
    @ModifyReturnValue(method = "extractEntity", at = @At("RETURN"))
    private <E extends Entity> EntityRenderState sable$extractOnSubLevel(final EntityRenderState state, final E entity, final float partialTicks) {
        final ClientSubLevel subLevel = (ClientSubLevel) Sable.HELPER.getContaining(entity);

        if (subLevel == null) {
            // Tracking sub-levels
            final SubLevel trackingSubLevel = Sable.HELPER.getTrackingSubLevel(entity);

            if (trackingSubLevel instanceof final ClientSubLevel clientSubLevel && !entity.isPassenger()) {
                final Vector3d oldTrackingPosLocal = trackingSubLevel.lastPose().transformPositionInverse(new Vector3d(entity.xOld, entity.yOld, entity.zOld));
                final Vector3d newTrackingPosLocal = trackingSubLevel.logicalPose().transformPositionInverse(JOMLConversion.toJOML(entity.position()));

                final Vector3d interpolatedTrackingPosLocal = new Vector3d(
                        Mth.lerp(partialTicks, oldTrackingPosLocal.x, newTrackingPosLocal.x),
                        Mth.lerp(partialTicks, oldTrackingPosLocal.y, newTrackingPosLocal.y),
                        Mth.lerp(partialTicks, oldTrackingPosLocal.z, newTrackingPosLocal.z)
                );

                clientSubLevel.renderPose(partialTicks).transformPosition(interpolatedTrackingPosLocal);

                state.x = interpolatedTrackingPosLocal.x;
                state.y = interpolatedTrackingPosLocal.y;
                state.z = interpolatedTrackingPosLocal.z;
            }

            return state;
        }

        final Pose3dc renderPose = subLevel.renderPose(partialTicks);
        final Vector3d transformedPosition = renderPose.transformPosition(new Vector3d(state.x, state.y, state.z));

        ((EntityRenderStateExtension) state).sable$setSubLevelOrientation(new Quaternionf(renderPose.orientation()));

        state.x = transformedPosition.x;
        state.y = transformedPosition.y;
        state.z = transformedPosition.z;
        return state;
    }

    @ModifyReturnValue(method = "extractEntity", at = @At("RETURN"))
    private <E extends Entity> EntityRenderState sable$extractRotation(final EntityRenderState state, final E entity, final float partialTicks) {
        if (!EntitySubLevelUtil.shouldKick(entity)) {
            return state;
        }

        final Quaterniond orientation = EntitySubLevelRotationHelper.getEntityOrientation(entity, x -> ((ClientSubLevel) x).renderPose(), partialTicks, EntitySubLevelRotationHelper.Type.ENTITY);

        if (orientation == null) {
            return state;
        }

        final Vec3 eyeOffset = entity.getEyePosition().subtract(entity.position());
        final Vec3 offset = Sable.HELPER.getEyePositionInterpolated(entity, partialTicks).subtract(entity.getEyePosition(partialTicks));

        final Quaternionf orientationf = new Quaternionf(orientation);
        final Matrix4f transform = new Matrix4f()
                .translate((float) offset.x, (float) offset.y, (float) offset.z)
                .translate((float) eyeOffset.x, (float) eyeOffset.y, (float) eyeOffset.z)
                .rotate(orientationf)
                .translate((float) -eyeOffset.x, (float) -eyeOffset.y, (float) -eyeOffset.z);

        ((EntityRenderStateExtension) state).sable$setRotation(transform, orientationf);
        return state;
    }

    /**
     * Rotates the whole entity (including shadows and fire) with the sub-level it is rendered in
     */
    @WrapMethod(method = "submit")
    private void sable$rotateWithSubLevel(final EntityRenderState state, final CameraRenderState cameraRenderState, final double x, final double y, final double z, final PoseStack poseStack, final SubmitNodeCollector collector, final Operation<Void> original) {
        final Quaternionfc orientation = ((EntityRenderStateExtension) state).sable$getSubLevelOrientation();

        if (orientation == null) {
            original.call(state, cameraRenderState, x, y, z, poseStack, collector);
            return;
        }

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(orientation);
        poseStack.translate(-x, -y, -z);
        original.call(state, cameraRenderState, x, y, z, poseStack, collector);
        poseStack.popPose();
    }

    @WrapOperation(method = "submit", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V"))
    private void sable$rotateEntity(final EntityRenderer<?, ?> instance, final EntityRenderState state, final PoseStack poseStack, final SubmitNodeCollector collector, final CameraRenderState cameraRenderState, final Operation<Void> original) {
        final EntityRenderStateExtension extension = (EntityRenderStateExtension) state;
        final org.joml.Matrix4fc transform = extension.sable$getRotationTransform();
        final Quaternionfc orientation = extension.sable$getRotationOrientation();

        if (transform == null || orientation == null) {
            original.call(instance, state, poseStack, collector, cameraRenderState);
            return;
        }

        poseStack.pushPose();
        poseStack.mulPose(transform);

        // Billboarded content (name tags, ...) must be counter-rotated to still face the camera
        final CameraRenderState counterRotated = new CameraRenderState();
        counterRotated.blockPos = cameraRenderState.blockPos;
        counterRotated.pos = cameraRenderState.pos;
        counterRotated.initialized = cameraRenderState.initialized;
        counterRotated.entityPos = cameraRenderState.entityPos;
        counterRotated.orientation = new Quaternionf(orientation).conjugate().mul(cameraRenderState.orientation);

        original.call(instance, state, poseStack, collector, counterRotated);
        poseStack.popPose();
    }

    @WrapOperation(method = "submit", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitFlame(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lorg/joml/Quaternionf;)V"))
    private void sable$rotateFlame(final SubmitNodeCollector instance, final PoseStack poseStack, final EntityRenderState state, final Quaternionf quaternion, final Operation<Void> original) {
        final org.joml.Matrix4fc transform = ((EntityRenderStateExtension) state).sable$getRotationTransform();

        if (transform == null) {
            original.call(instance, poseStack, state, quaternion);
            return;
        }

        poseStack.pushPose();
        poseStack.mulPose(transform);
        original.call(instance, poseStack, state, quaternion);
        poseStack.popPose();
    }
}
