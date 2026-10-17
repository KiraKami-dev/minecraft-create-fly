package dev.ryanhcode.sable.fabric.mixin.compatibility.create.belt;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.client.content.kinetics.belt.BeltRenderer;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Make upright items face the the camera properly on belts
 * <p>
 * Create Fly extracts the camera position into the belt render state ({@code BeltRenderState#camera}) instead of
 * querying {@code Camera#getPosition()} while rendering items, so every read of it in {@code renderItem} is moved into
 * the plot of the sub-level instead.
 */
@Mixin(BeltRenderer.class)
public class BeltRendererMixin {

    @ModifyExpressionValue(method = "renderItem", at = @At(value = "FIELD", target = "Lcom/zurrtum/create/client/content/kinetics/belt/BeltRenderer$BeltRenderState;camera:Lnet/minecraft/world/phys/Vec3;", opcode = Opcodes.GETFIELD, remap = true), remap = false)
    private Vec3 sable$renderViewEntityPosition(final Vec3 original, @Local(argsOnly = true) final BeltRenderer.BeltRenderState state) {
        final ClientSubLevel subLevel = Sable.HELPER.getContainingClient(state.blockPos);
        if (subLevel != null) {
            return subLevel.renderPose().transformPositionInverse(original);
        } else {
            return original;
        }
    }
}
