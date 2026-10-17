package dev.ryanhcode.sable.fabric.mixin.compatibility.create.flywheel;

import com.zurrtum.create.client.flywheel.api.backend.RenderContext;
import com.zurrtum.create.client.flywheel.api.visualization.VisualizationManager;
import com.zurrtum.create.client.flywheel.impl.visualization.VisualManagerImpl;
import com.zurrtum.create.client.flywheel.impl.visualization.VisualizationManagerImpl;
import com.zurrtum.create.client.flywheel.impl.visualization.storage.BlockEntityStorage;
import dev.ryanhcode.sable.fabric.mixinterface.compatibility.create.flywheel.BlockEntityStorageExtension;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.zurrtum.create.client.flywheel.impl.visualization.VisualizationManagerImpl$RenderDispatcherImpl")
public class RenderDispatcherImplMixin {

    @SuppressWarnings("unchecked")
    @Inject(method = "onStartLevelRender", at = @At("HEAD"))
    private void sable$onStartLevelRender(final RenderContext ctx, final CallbackInfo ci) {
        if (VisualizationManager.get(ctx.level()) instanceof final VisualizationManagerImpl manager) {
            final BlockEntityStorage storage = ((VisualManagerImpl<BlockEntity, BlockEntityStorage>) manager.blockEntities()).getStorage();
            ((BlockEntityStorageExtension) storage).sable$preFlywheelFrame(ctx.level(), ctx.partialTick());
        }
    }
}
