package dev.simulated_team.simulated.mixin.plunger_launcher;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.simulated_team.simulated.SimulatedClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * First person rendering of the plunger launcher. Upstream the render handler listened to NeoForge's
 * {@code RenderHandEvent}, which fires right before {@code renderArmWithItem}.
 */
@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private EntityRenderDispatcher entityRenderDispatcher;

    @Inject(method = "renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V", at = @At("HEAD"), cancellable = true)
    private void simulated$renderPlungerLauncher(final AbstractClientPlayer player, final float partialTick, final float pitch, final InteractionHand hand,
                                                 final float swingProgress, final ItemStack stack, final float equipProgress, final PoseStack poseStack,
                                                 final SubmitNodeCollector collector, final int light, final CallbackInfo ci) {
        if (SimulatedClient.PLUNGER_LAUNCHER_RENDER_HANDLER.onRenderPlayerHand(stack, this.minecraft, this.entityRenderDispatcher,
                (ItemInHandRenderer) (Object) this, poseStack, collector, light, partialTick, hand, equipProgress, swingProgress)) {
            ci.cancel();
        }
    }
}
