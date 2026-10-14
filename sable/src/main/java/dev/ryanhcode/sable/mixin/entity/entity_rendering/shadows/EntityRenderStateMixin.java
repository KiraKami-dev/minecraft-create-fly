package dev.ryanhcode.sable.mixin.entity.entity_rendering.shadows;

import dev.ryanhcode.sable.mixinhelpers.entity.entity_rendering.shadows.SubLevelShadowExtension;
import dev.ryanhcode.sable.mixinhelpers.entity.entity_rendering.shadows.SubLevelShadowList;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public class EntityRenderStateMixin implements SubLevelShadowExtension {

    @Unique
    private SubLevelShadowList sable$subLevelShadows;

    @Override
    public SubLevelShadowList sable$getSubLevelShadows() {
        return this.sable$subLevelShadows;
    }

    @Override
    public void sable$setSubLevelShadows(final SubLevelShadowList shadows) {
        this.sable$subLevelShadows = shadows;
    }
}
