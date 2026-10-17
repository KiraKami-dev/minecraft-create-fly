package dev.ryanhcode.sable.fabric.mixin.compatibility.create.depot;

import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.client.content.logistics.depot.DepotRenderer;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Create Fly passes the camera position into {@link DepotRenderer#renderItem} (upstream read
 * {@code Camera#getPosition()} there), so the camera position argument is moved into the plot of the sub-level.
 */
@Mixin(DepotRenderer.class)
public class DepotRendererMixin {
    @ModifyVariable(method = "renderItem", at = @At("HEAD"), argsOnly = true, ordinal = 1, remap = false)
    private static Vec3 sable$renderViewEntityPosition(final Vec3 original, @Local(argsOnly = true, ordinal = 0) final Vec3 position) {
        final ClientSubLevel subLevel = Sable.HELPER.getContainingClient(position);
        if (subLevel != null) {
            return subLevel.renderPose().transformPositionInverse(original);
        } else {
            return original;
        }
    }
}
