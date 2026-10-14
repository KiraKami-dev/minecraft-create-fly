package dev.ryanhcode.sable.mixin.conduit;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.ConduitRenderer;
import net.minecraft.client.renderer.blockentity.state.CondiutRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConduitRenderer.class)
public class ConduitRendererMixin {

    /**
     * The eye billboards towards the camera, so it has to undo the rotation of the sub-level it is in first.
     * Ordinal 5 is the {@code poseStack.mulPose(cameraRenderState.orientation)} call.
     */
    @Inject(method = "submit(Lnet/minecraft/client/renderer/blockentity/state/CondiutRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionfc;)V", ordinal = 5))
    private void sable$reorientEye(final CondiutRenderState renderState, final PoseStack poseStack, final SubmitNodeCollector collector, final CameraRenderState cameraRenderState, final CallbackInfo ci) {
        final Minecraft minecraft = Minecraft.getInstance();
        final SubLevel subLevel = Sable.HELPER.getContaining(minecraft.level, renderState.blockPos);
        if (subLevel != null) {
            final float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            poseStack.mulPose(((ClientSubLevel) subLevel).renderPose(partialTick).orientation().get(new Quaternionf()).invert());
        }
    }
}
