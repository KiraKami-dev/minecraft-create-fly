package dev.ryanhcode.sable.mixin.sublevel_render.impl.sodium;

import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Plot chunks are drawn by Sable's renderer, so Sodium shouldn't allocate sections (and regions) for them.
 */
@Mixin(value = RenderSectionManager.class, remap = false)
public class RenderSectionManagerMixin {

    @Shadow
    @Final
    private ClientLevel level;

    @Inject(method = "onSectionAdded", at = @At("HEAD"), cancellable = true)
    private void sable$skipPlotSections(final int x, final int y, final int z, final CallbackInfo ci) {
        final ClientSubLevelContainer container = SubLevelContainer.getContainer(this.level);
        if (container != null && container.inBounds(x, z)) {
            ci.cancel();
        }
    }
}
