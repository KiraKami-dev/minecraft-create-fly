package dev.ryanhcode.sable.mixin.respawn_point.sleeping;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.mixinterface.respawn_point.SleepingSubLevelOrientationExtension;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState> {

    /**
     * Captures the render orientation of the sub-level the entity is sleeping in, as the render state has no entity reference.
     */
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    private void sable$extractSleepingOrientation(final T livingEntity, final S state, final float partialTick, final CallbackInfo ci) {
        final SleepingSubLevelOrientationExtension extension = (SleepingSubLevelOrientationExtension) state;
        extension.sable$setSleepingSubLevelOrientation(null);

        if (livingEntity.getBedOrientation() == null) {
            return;
        }

        final Optional<BlockPos> sleepingPos = livingEntity.getSleepingPos();
        if (sleepingPos.isPresent()) {
            final SubLevel subLevel = Sable.HELPER.getContaining(livingEntity.level(), sleepingPos.get());

            if (subLevel instanceof final ClientSubLevel clientSubLevel) {
                extension.sable$setSleepingSubLevelOrientation(new Quaternionf(clientSubLevel.renderPose().orientation()));
            }
        }
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER))
    private void sable$setupRotations(final S state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState cameraRenderState, final CallbackInfo ci) {
        if (state.bedOrientation == null) {
            return;
        }

        final Quaternionf orientation = ((SleepingSubLevelOrientationExtension) state).sable$getSleepingSubLevelOrientation();
        if (orientation != null) {
            poseStack.mulPose(orientation);
        }
    }

}
