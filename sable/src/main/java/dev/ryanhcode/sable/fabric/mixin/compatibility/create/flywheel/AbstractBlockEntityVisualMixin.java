package dev.ryanhcode.sable.fabric.mixin.compatibility.create.flywheel;

import com.zurrtum.create.client.flywheel.api.visual.DynamicVisual;
import com.zurrtum.create.client.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.ryanhcode.sable.Sable;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.FrustumIntersection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Visuals on a sub-level sit in an embedding, so their position is neither where the camera nor the frustum sees them.
 * Always update and draw them; Flywheel's own culling still applies to the instances.
 */
@Mixin(value = AbstractBlockEntityVisual.class, remap = false)
public abstract class AbstractBlockEntityVisualMixin {

    @Shadow
    @Final
    protected BlockEntity blockEntity;

    @Inject(method = "isVisible", at = @At("HEAD"), cancellable = true)
    private void sable$isVisible(final FrustumIntersection frustum, final CallbackInfoReturnable<Boolean> cir) {
        if (Sable.HELPER.getContaining(this.blockEntity) != null) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "doDistanceLimitThisFrame", at = @At("HEAD"), cancellable = true)
    private void sable$doDistanceLimitThisFrame(final DynamicVisual.Context context, final CallbackInfoReturnable<Boolean> cir) {
        if (Sable.HELPER.getContaining(this.blockEntity) != null) {
            cir.setReturnValue(false);
        }
    }
}
