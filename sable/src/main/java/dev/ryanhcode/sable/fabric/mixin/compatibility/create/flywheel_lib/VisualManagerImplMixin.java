package dev.ryanhcode.sable.fabric.mixin.compatibility.create.flywheel_lib;

import com.zurrtum.create.client.flywheel.api.task.Plan;
import com.zurrtum.create.client.flywheel.api.visual.DynamicVisual;
import com.zurrtum.create.client.flywheel.api.visualization.VisualManager;
import com.zurrtum.create.client.flywheel.api.visualization.VisualizationContext;
import com.zurrtum.create.client.flywheel.impl.visualization.VisualManagerImpl;
import com.zurrtum.create.client.flywheel.impl.visualization.storage.Storage;
import dev.ryanhcode.sable.fabric.mixinterface.compatibility.create.flywheel.BlockEntityStorageExtension;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VisualManagerImpl.class)
public abstract class VisualManagerImplMixin<T, S extends Storage<T>> implements VisualManager<T> {

    @Shadow(remap = false)
    @Final
    private S storage;

    @Inject(method = "framePlan", at = @At("HEAD"), remap = false)
    private void sable$preFramePlan(final VisualizationContext visualizationContext, final CallbackInfoReturnable<Plan<DynamicVisual.Context>> cir) {
        if (this.storage instanceof final BlockEntityStorageExtension extension) {
            extension.sable$setPlanVisualizationContext(visualizationContext);
        }
    }
}
