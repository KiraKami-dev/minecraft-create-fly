package dev.ryanhcode.sable.mixin.entity.entity_rendering.shadows;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.mixinhelpers.entity.entity_rendering.shadows.SubLevelShadowExtension;
import dev.ryanhcode.sable.mixinhelpers.entity.entity_rendering.shadows.SubLevelShadowList;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.renderer.state.CameraRenderState;

/**
 * Render shadows on sub-levels
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    /**
     * Submits the sub-level shadow quads (computed at extraction time by {@link EntityRendererMixin}) alongside the vanilla shadow.
     */
    @Inject(method = "submit", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V"))
    private <S extends EntityRenderState> void sable$submitShadowsOnSubLevels(final S entityRenderState,
                                                                              final CameraRenderState cameraRenderState,
                                                                              final double d, final double e, final double f,
                                                                              final PoseStack poseStack,
                                                                              final SubmitNodeCollector submitNodeCollector,
                                                                              final CallbackInfo ci) {
        final SubLevelShadowList shadows = ((SubLevelShadowExtension) entityRenderState).sable$getSubLevelShadows();
        if (shadows != null && !shadows.vertices.isEmpty()) {
            submitNodeCollector.submitShadow(poseStack, entityRenderState.shadowRadius, shadows);
        }
    }

}
