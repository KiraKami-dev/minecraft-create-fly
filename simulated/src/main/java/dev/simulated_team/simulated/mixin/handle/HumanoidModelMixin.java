package dev.simulated_team.simulated.mixin.handle;

import dev.simulated_team.simulated.content.blocks.handle.PlayerHoldingHandleRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public class HumanoidModelMixin<T extends HumanoidRenderState> {

    @Shadow
    @Final
    public ModelPart body;

    // 1.21.11 models animate from render states instead of entities; players are AvatarRenderStates, which carry the
    // entity id. The exact descriptor avoids the EntityModel bridge method.
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("RETURN"))
    private void simulated$afterSetupAnim(final T renderState, final CallbackInfo callbackInfo) {
        if (!(renderState instanceof final AvatarRenderState avatarState))
            return;
        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !(level.getEntity(avatarState.id) instanceof final AbstractClientPlayer player))
            return;
        PlayerHoldingHandleRenderer.afterSetupAnim(player, (HumanoidModel<?>) (Object) this);
    }
}
