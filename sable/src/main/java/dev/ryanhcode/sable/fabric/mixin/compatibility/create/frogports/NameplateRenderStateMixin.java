package dev.ryanhcode.sable.fabric.mixin.compatibility.create.frogports;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.zurrtum.create.client.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.ryanhcode.sable.fabric.mixinterface.compatibility.create.frogports.NameplateRenderStateExtension;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Orients the hover nameplates of block entities on sub-levels towards the camera, by cancelling out the sub-level
 * rotation already present in the pose stack (see {@link SmartBlockEntityRendererMixin}).
 */
@Mixin(SmartBlockEntityRenderer.NameplateRenderState.class)
public class NameplateRenderStateMixin implements NameplateRenderStateExtension {

    @Unique
    private @Nullable ClientSubLevel sable$subLevel;

    @Override
    public void sable$setSubLevel(@Nullable final ClientSubLevel subLevel) {
        this.sable$subLevel = subLevel;
    }

    @Override
    public @Nullable ClientSubLevel sable$getSubLevel() {
        return this.sable$subLevel;
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitNameTag(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;ILnet/minecraft/network/chat/Component;ZIDLnet/minecraft/client/renderer/state/CameraRenderState;)V", remap = true), remap = false)
    private void sable$renderNameTag(final SubmitNodeCollector instance,
                                     final PoseStack poseStack,
                                     final Vec3 pos,
                                     final int i,
                                     final Component label,
                                     final boolean seeThrough,
                                     final int light,
                                     final double distance,
                                     final CameraRenderState cameraState,
                                     final Operation<Void> original) {
        final ClientSubLevel subLevel = this.sable$subLevel;

        if (subLevel == null) {
            original.call(instance, poseStack, pos, i, label, seeThrough, light, distance, cameraState);
            return;
        }

        // The name tag is oriented with the camera orientation when submitted
        final Quaternionf cameraOrientation = cameraState.orientation;
        cameraState.orientation = new Quaternionf(cameraOrientation).premul(new Quaternionf(subLevel.renderPose().orientation()).conjugate());

        try {
            original.call(instance, poseStack, pos, i, label, seeThrough, light, distance, cameraState);
        } finally {
            cameraState.orientation = cameraOrientation;
        }
    }

}
