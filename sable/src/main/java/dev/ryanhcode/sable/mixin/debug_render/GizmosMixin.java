package dev.ryanhcode.sable.mixin.debug_render;

import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.debug.PlotSpaceGizmo;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.Gizmo;
import net.minecraft.gizmos.Gizmos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Takes sub-levels into account for debug gizmos (formerly {@code DebugRenderer#renderFilledBox}): gizmos emitted in
 * plot space are drawn where the sub-level is rendered.
 */
@Mixin(Gizmos.class)
public class GizmosMixin {

    @ModifyVariable(method = "addGizmo", at = @At("HEAD"), argsOnly = true)
    private static Gizmo sable$wrapPlotSpaceGizmo(final Gizmo gizmo) {
        if (gizmo instanceof PlotSpaceGizmo) {
            return gizmo;
        }

        final ClientSubLevelContainer container = SubLevelContainer.getContainer(Minecraft.getInstance().level);
        if (container == null || container.getAllSubLevels().isEmpty()) {
            return gizmo;
        }

        return new PlotSpaceGizmo(gizmo);
    }
}
