package dev.simulated_team.simulated.mixin.ponder;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.zurrtum.create.client.ponder.foundation.render.SceneRenderState;
import com.zurrtum.create.client.ponder.foundation.render.SceneRenderer;
import dev.simulated_team.simulated.mixin_interface.ponder.PonderSceneExtension;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Upstream targeted PonderUI.renderScene; Create Fly renders ponder scenes as a picture-in-picture element, and the
// base plate shadow ("kool shadow fx") moved to SceneRenderer.renderScene with the same constant and translate calls.
@Mixin(SceneRenderer.class)
public class SceneRendererMixin {

    @ModifyConstant(method = "renderScene", constant = @Constant(intValue = 0x66_000000, ordinal = 0), remap = false)
    private static int customShadowFade(final int constant, @Local(argsOnly = true) final SceneRenderState state) {
        final int alpha = (int)((constant >> 24) * ((PonderSceneExtension) state.scene()).simulated$getBasePlateAnimationTimer(state.partialTicks()));
        return (alpha << 24) | (constant & 0x00_FFFFFF);
    }

    // ordinal 1 is the translate to the base plate offset, right before the shadow quads
    @Inject(method = "renderScene", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V", ordinal = 1, shift = At.Shift.AFTER, remap = true), remap = false)
    private static void shadowTranslate(final CallbackInfo ci, @Local(argsOnly = true) final SceneRenderState state, @Local(argsOnly = true) final PoseStack ms) {
        final Vec3 offset = ((PonderSceneExtension) state.scene()).simulated$getShadowOffset(state.partialTicks());
        ms.translate(offset.x, offset.y, offset.z);
    }
}
