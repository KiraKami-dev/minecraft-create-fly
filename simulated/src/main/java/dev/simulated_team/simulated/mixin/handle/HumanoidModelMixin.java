package dev.simulated_team.simulated.mixin.handle;

import com.zurrtum.create.client.foundation.render.SkyhookRenderState;
import dev.simulated_team.simulated.content.blocks.handle.PlayerHoldingHandleRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(HumanoidModel.class)
public class HumanoidModelMixin<T extends HumanoidRenderState> {

    @Shadow
    @Final
    public ModelPart body;

    // 1.21.11 models animate from render states instead of entities. Players are AvatarRenderStates, which Create Fly
    // already tags with the player UUID (AvatarRendererMixin -> SkyhookRenderState, used the same way for skyhooks).
    // The exact descriptor avoids the EntityModel bridge method.
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("RETURN"))
    private void simulated$afterSetupAnim(final T renderState, final CallbackInfo callbackInfo) {
        if (!(renderState instanceof final AvatarRenderState avatarState))
            return;
        final UUID uuid = ((SkyhookRenderState) avatarState).create$getUuid();
        if (uuid != null) {
            PlayerHoldingHandleRenderer.afterSetupAnim(uuid, (HumanoidModel<?>) (Object) this);
        }
    }
}
