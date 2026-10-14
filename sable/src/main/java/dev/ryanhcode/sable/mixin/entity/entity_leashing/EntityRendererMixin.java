package dev.ryanhcode.sable.mixin.entity.entity_leashing;

import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Since 1.21.11 the leash geometry is computed while extracting the render state ({@code extractRenderState}),
 * instead of while rendering.
 */
@Mixin(EntityRenderer.class)
public class EntityRendererMixin {

    /**
     * Moves the leash end point (given in the frame of the holder) into the frame of the leashed entity.
     */
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void sable$transformLeashEnds(final Entity leashedEntity, final EntityRenderState state, final float f, final CallbackInfo ci) {
        if (state.leashStates == null) {
            return;
        }

        final ActiveSableCompanion helper = Sable.HELPER;
        final SubLevel leashedSubLevel = helper.getContaining(leashedEntity);

        for (final EntityRenderState.LeashState leashState : state.leashStates) {
            final Vector3d end = JOMLConversion.toJOML(leashState.end);
            final SubLevel holdingSubLevel = helper.getContaining(leashedEntity.level(), end);

            if (holdingSubLevel != null) {
                holdingSubLevel.logicalPose().transformPosition(end);
            }

            if (leashedSubLevel != null) {
                leashedSubLevel.logicalPose().transformPositionInverse(end);
            }

            leashState.end = JOMLConversion.toMojang(end);
        }
    }

    @Redirect(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getEyePosition(F)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 sable$getEyePosition(final Entity instance, final float f, @Local(argsOnly = true, ordinal = 0) final Entity leashedEntity) {
        final ActiveSableCompanion helper = Sable.HELPER;
        final SubLevel leashedSubLevel = helper.getContaining(leashedEntity);

        final Vector3d eyePosition = JOMLConversion.toJOML(instance.getEyePosition(f));
        final SubLevel holdingSubLevel = helper.getContaining(leashedEntity.level(), eyePosition);

        if (holdingSubLevel != null) {
            holdingSubLevel.logicalPose().transformPosition(eyePosition);
        }

        if (leashedSubLevel != null) {
            leashedSubLevel.logicalPose().transformPositionInverse(eyePosition);
        }

        return JOMLConversion.toMojang(eyePosition);
    }
}
