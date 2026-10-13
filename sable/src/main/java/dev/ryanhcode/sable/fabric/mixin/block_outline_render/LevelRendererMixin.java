package dev.ryanhcode.sable.fabric.mixin.block_outline_render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.BlockOutlineRenderState;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Transforms block hover outlines for sub-levels.
 */
@Mixin(value = LevelRenderer.class, priority = 400)
public abstract class LevelRendererMixin {

    // Storage to avoid repeated allocation
    private final @Unique Quaternionf sable$orientationStorage = new Quaternionf();

    @Shadow
    @Nullable
    private ClientLevel level;

    @WrapOperation(method = "renderBlockOutline", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderHitOutline(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;DDDLnet/minecraft/client/renderer/state/BlockOutlineRenderState;IF)V"))
    private void sable$renderSubLevelHitOutline(final LevelRenderer instance, final PoseStack poseStack, final VertexConsumer consumer, final double camX, final double camY, final double camZ, final BlockOutlineRenderState state, final int color, final float lineWidth, final Operation<Void> original) {
        final ClientSubLevel subLevel = this.level != null ? (ClientSubLevel) Sable.HELPER.getContaining(this.level, state.pos()) : null;

        if (subLevel == null) {
            original.call(instance, poseStack, consumer, camX, camY, camZ, state, color, lineWidth);
            return;
        }

        final Pose3dc pose = subLevel.renderPose();
        final Vector3dc position = pose.position();
        final Vector3dc rotationPoint = pose.rotationPoint();
        final Vector3dc scale = pose.scale();

        poseStack.pushPose();
        poseStack.translate((float) (position.x() - camX), (float) (position.y() - camY), (float) (position.z() - camZ));
        poseStack.mulPose(this.sable$orientationStorage.set(pose.orientation()));
        poseStack.scale((float) scale.x(), (float) scale.y(), (float) scale.z());

        // The shape is drawn relative to the rotation point, which keeps the plot coordinates out of float precision
        original.call(instance, poseStack, consumer, rotationPoint.x(), rotationPoint.y(), rotationPoint.z(), state, color, lineWidth);

        poseStack.popPose();
    }
}
