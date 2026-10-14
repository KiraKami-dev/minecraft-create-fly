package dev.ryanhcode.sable.mixinhelpers.entity.entity_rendering.shadows;

import org.jetbrains.annotations.Nullable;

/**
 * Implemented on {@link net.minecraft.client.renderer.entity.state.EntityRenderState}.
 */
public interface SubLevelShadowExtension {

    @Nullable
    SubLevelShadowList sable$getSubLevelShadows();

    void sable$setSubLevelShadows(@Nullable SubLevelShadowList shadows);
}
