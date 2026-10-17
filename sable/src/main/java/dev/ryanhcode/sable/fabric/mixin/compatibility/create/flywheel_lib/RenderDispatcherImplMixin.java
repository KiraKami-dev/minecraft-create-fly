package dev.ryanhcode.sable.fabric.mixin.compatibility.create.flywheel_lib;

import com.zurrtum.create.client.flywheel.api.backend.RenderContext;
import com.zurrtum.create.client.flywheel.impl.visualization.VisualManagerImpl;
import com.zurrtum.create.client.flywheel.impl.visualization.VisualizationManagerImpl;
import dev.ryanhcode.sable.fabric.compatibility.create.flywheel.FlywheelCompat;
import dev.ryanhcode.sable.fabric.mixinterface.compatibility.create.flywheel.BlockEntityStorageExtension;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.zurrtum.create.client.flywheel.impl.visualization.VisualizationManagerImpl$RenderDispatcherImpl")
public class RenderDispatcherImplMixin {

    @Shadow(remap = false)
    @Final
    private VisualizationManagerImpl this$0;

    @Inject(method = "onStartLevelRender", at = @At("HEAD"), remap = false)
    private void sable$onStartLevelRender(final RenderContext ctx, final CallbackInfo ci) {
        FlywheelCompat.preVisualizationFrame(ctx.level(), ctx.partialTick());
        ((BlockEntityStorageExtension) ((VisualManagerImpl<?, ?>) this.this$0.blockEntities()).getStorage())
                .sable$preFlywheelFrame();
    }

}
