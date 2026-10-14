package dev.ryanhcode.sable.mixin.entity.entity_rendering.shadows;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ryanhcode.sable.mixinhelpers.entity.entity_rendering.shadows.SubLevelShadowList;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ShadowFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws the free-form sub-level shadow quads submitted through {@link SubLevelShadowList}.
 */
@Mixin(ShadowFeatureRenderer.class)
public class ShadowFeatureRendererMixin {

    @Shadow
    @Final
    private static RenderType SHADOW_RENDER_TYPE;

    @Inject(method = "render", at = @At("TAIL"))
    private void sable$renderSubLevelShadows(final SubmitNodeCollection submitNodeCollection, final MultiBufferSource.BufferSource bufferSource, final CallbackInfo ci) {
        VertexConsumer vertexConsumer = null;
        final Vector3f position = new Vector3f();

        for (final SubmitNodeStorage.ShadowSubmit shadowSubmit : submitNodeCollection.getShadowSubmits()) {
            if (!(shadowSubmit.pieces() instanceof final SubLevelShadowList list)) {
                continue;
            }

            if (vertexConsumer == null) {
                vertexConsumer = bufferSource.getBuffer(SHADOW_RENDER_TYPE);
            }

            for (final float[] vertex : list.vertices) {
                shadowSubmit.pose().transformPosition(vertex[0], vertex[1], vertex[2], position);
                vertexConsumer.addVertex(position.x(), position.y(), position.z(), ((int) vertex[5]) << 24 | 0xFFFFFF, vertex[3], vertex[4], OverlayTexture.NO_OVERLAY, 15728880, 0.0F, 1.0F, 0.0F);
            }
        }
    }
}
