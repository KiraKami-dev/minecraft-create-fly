package dev.ryanhcode.sable.mixin.entity.entity_rendering.shadows;

import dev.ryanhcode.sable.mixinhelpers.entity.entity_rendering.shadows.SubLevelEntityShadowRenderer;
import dev.ryanhcode.sable.mixinhelpers.entity.entity_rendering.shadows.SubLevelShadowExtension;
import dev.ryanhcode.sable.mixinhelpers.entity.entity_rendering.shadows.SubLevelShadowList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Computes the shadows cast onto sub-levels while the entity render state is extracted.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererShadowMixin<T extends Entity, S extends EntityRenderState> {

    @Shadow
    protected abstract float getShadowRadius(S entityRenderState);

    @Shadow
    protected abstract float getShadowStrength(S entityRenderState);

    @Inject(method = "createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;", at = @At("RETURN"))
    private void sable$extractSubLevelShadows(final T entity, final float partialTick, final CallbackInfoReturnable<S> cir) {
        final S state = cir.getReturnValue();
        final SubLevelShadowExtension extension = (SubLevelShadowExtension) state;
        extension.sable$setSubLevelShadows(null);

        if (!Minecraft.getInstance().options.entityShadows().get() || state.isInvisible) {
            return;
        }

        final float radius = Math.min(this.getShadowRadius(state), 32.0F);
        if (radius <= 0.0F) {
            return;
        }

        final float strength = (float) ((1.0 - state.distanceToCameraSq / 256.0) * this.getShadowStrength(state));
        if (strength <= 0.0F) {
            return;
        }

        final SubLevelShadowList shadows = new SubLevelShadowList();
        SubLevelEntityShadowRenderer.renderEntityShadowOnSubLevels(entity, strength, partialTick, radius, shadows);
        if (!shadows.vertices.isEmpty()) {
            extension.sable$setSubLevelShadows(shadows);
        }
    }
}
