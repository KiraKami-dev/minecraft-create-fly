package dev.ryanhcode.sable.fabric.mixin.dynamic_directional_shading;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ryanhcode.sable.mixinterface.dynamic_directional_shading.ModelBlockRendererCacheExtension;
import dev.ryanhcode.sable.render.dynamic_shade.SableDynamicDirectionalShading;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MutableQuadViewImpl;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.AbstractRenderContext;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Indigo counterpart of {@link dev.ryanhcode.sable.render.dynamic_shade.SubLevelVertexConsumer}: on sub-levels,
 * shade-less quads get a normal pointing straight up, so dynamic shading leaves them at full brightness.
 */
@Mixin(AbstractRenderContext.class)
public class IndigoRenderContextMixin {

    @WrapOperation(method = "bufferQuad(Lnet/fabricmc/fabric/impl/client/indigo/renderer/mesh/MutableQuadViewImpl;Lcom/mojang/blaze3d/vertex/VertexConsumer;)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;addVertex(FFFIFFIIFFF)V"))
    private void sable$addVertex(final VertexConsumer consumer, final float x, final float y, final float z, final int color, final float u, final float v,
                                 final int overlay, final int light, final float normalX, final float normalY, final float normalZ,
                                 final Operation<Void> original, final MutableQuadViewImpl quad, final VertexConsumer vertexConsumer) {
        if (!quad.diffuseShade() && SableDynamicDirectionalShading.isEnabled() && ((ModelBlockRendererCacheExtension) ModelBlockRenderer.CACHE.get()).sable$getOnSubLevel()) {
            original.call(consumer, x, y, z, color, u, v, overlay, light, 0.0f, 1.0f, 0.0f);
        } else {
            original.call(consumer, x, y, z, color, u, v, overlay, light, normalX, normalY, normalZ);
        }
    }
}
