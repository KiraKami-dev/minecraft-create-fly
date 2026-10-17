package dev.simulated_team.simulated.mixin.rope;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.ZiplineClientManager;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.BlockOutlineRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    // 1.21.11: renderHitOutline draws from a BlockOutlineRenderState (it is called once more for the high contrast pass)
    @Inject(method = "renderHitOutline", at = @At("HEAD"), cancellable = true)
    private void sable$cancelBlockHitOutline(final PoseStack poseStack, final VertexConsumer consumer, final double camX, final double camY, final double camZ, final BlockOutlineRenderState outlineState, final int color, final float lineWidth, final CallbackInfo ci) {
        if (ZiplineClientManager.hoveringRope != null) {
            ci.cancel();
        }
    }
}
