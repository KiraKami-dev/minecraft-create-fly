package dev.simulated_team.simulated.fabric.mixin.client;

import dev.simulated_team.simulated.events.SimulatedCommonClientEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * NeoForge's {@code ItemTooltipEvent}. Fabric's {@code ItemTooltipCallback} doesn't pass the player, which is
 * {@code null} while the creative search tree is built.
 */
@Mixin(ItemStack.class)
public class ItemStackMixin {

    // the first RETURN is the hidden tooltip, the second the built list (same point as Fabric's ItemTooltipCallback)
    @Inject(method = "getTooltipLines", at = @At(value = "RETURN", ordinal = 1))
    private void simulated$appendTooltip(final Item.TooltipContext context, @Nullable final Player player, final TooltipFlag flag, final CallbackInfoReturnable<List<Component>> cir) {
        SimulatedCommonClientEvents.appendTooltip((ItemStack) (Object) this, flag, player, cir.getReturnValue());
    }
}
