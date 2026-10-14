package dev.ryanhcode.sable.mixin.debug_render;

import dev.ryanhcode.sable.SableClient;
import dev.ryanhcode.sable.debug.SubLevelDebugGizmos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Emits the sub-level debug gizmos together with the vanilla debug renderers.
 */
@Mixin(DebugRenderer.class)
public class DebugRendererMixin {

    @Inject(method = "emitGizmos", at = @At("TAIL"))
    private void sable$emitGizmos(final Frustum frustum, final double camX, final double camY, final double camZ, final float partialTick, final CallbackInfo ci) {
        final Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.ENTITY_HITBOXES) && !minecraft.showOnlyReducedInfo()) {
            SubLevelDebugGizmos.emit();
        }

        SableClient.GIZMO_HANDLER.emitGizmos(partialTick);
    }
}
