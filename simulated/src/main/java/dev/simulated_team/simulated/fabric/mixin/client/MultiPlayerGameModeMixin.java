package dev.simulated_team.simulated.fabric.mixin.client;

import dev.simulated_team.simulated.events.SimulatedCommonClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * NeoForge's {@code UseItemOnBlockEvent} in the {@code ITEM_AFTER_BLOCK} phase: the block didn't consume the click and
 * the held item is about to be used on it.
 */
@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "performUseItemOn",
            at = @At(value = "NEW", target = "(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/item/context/UseOnContext;"),
            cancellable = true)
    private void simulated$useItemAfterBlock(final LocalPlayer player, final InteractionHand hand, final BlockHitResult hitResult, final CallbackInfoReturnable<InteractionResult> cir) {
        if (SimulatedCommonClientEvents.useItemOnBlockEvent(this.minecraft.level, player, player.getItemInHand(hand), hand)) {
            cir.setReturnValue(InteractionResult.CONSUME);
        }
    }
}
