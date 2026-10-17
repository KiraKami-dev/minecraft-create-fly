package dev.ryanhcode.sable.fabric.mixin.compatibility.create.frogports;

import com.zurrtum.create.client.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.fabric.mixinterface.compatibility.create.frogports.NameplateRenderStateExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes the hover nameplates of block entities on sub-levels face the camera.
 * <br>
 * Upstream redirected the camera orientation in {@code renderNameplateOnHover}. Create Fly extracts a
 * {@link SmartBlockEntityRenderer.NameplateRenderState} instead and vanilla orients the name tag at submit time, so
 * the sub-level is remembered on the render state here and the orientation is fixed in
 * {@link NameplateRenderStateMixin}.
 */
@Mixin(SmartBlockEntityRenderer.class)
public class SmartBlockEntityRendererMixin {

    @Inject(method = "getNameplateRenderState", at = @At("RETURN"), remap = false)
    private static void sable$rememberSubLevel(final SmartBlockEntity blockEntity,
                                               final BlockPos pos,
                                               final Vec3 cameraPos,
                                               final Component tag,
                                               final float yOffset,
                                               final int light,
                                               final CallbackInfoReturnable<SmartBlockEntityRenderer.NameplateRenderState> cir) {
        final SmartBlockEntityRenderer.NameplateRenderState state = cir.getReturnValue();

        if (state != null) {
            ((NameplateRenderStateExtension) (Object) state).sable$setSubLevel(Sable.HELPER.getContainingClient(blockEntity));
        }
    }

}
