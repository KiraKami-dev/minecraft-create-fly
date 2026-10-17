package dev.simulated_team.simulated.fabric.mixin.client;

import dev.simulated_team.simulated.events.SimulatedCommonClientEvents;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NeoForge's {@code InputEvent.InteractionKeyMappingTriggered} for the use key: fired at the start of each hand
 * iteration in {@code startUseItem}; cancelling it stops the use without swinging the hand.
 */
@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(method = "startUseItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;", ordinal = 0),
            cancellable = true)
    private void simulated$useItemKeyTriggered(final CallbackInfo ci) {
        if (SimulatedCommonClientEvents.useItemMappingTriggered()) {
            ci.cancel();
        }
    }
}
