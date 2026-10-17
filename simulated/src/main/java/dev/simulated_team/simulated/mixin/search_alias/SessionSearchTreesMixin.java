package dev.simulated_team.simulated.mixin.search_alias;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.simulated_team.simulated.client.SearchAlias;
import net.minecraft.client.multiplayer.SessionSearchTrees;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

@Mixin(SessionSearchTrees.class)
public class SessionSearchTreesMixin {

    // 1.21.11: the call lives in the static lambda of SessionSearchTrees.getTooltipLines(Stream, TooltipContext, TooltipFlag)
    @WrapOperation(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getTooltipLines(Lnet/minecraft/world/item/Item$TooltipContext;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/TooltipFlag;)Ljava/util/List;"))
    private static List<Component> simulated$getTooltipLines(ItemStack instance, Item.TooltipContext i, Player list, TooltipFlag mutablecomponent, Operation<List<Component>> original) {
        List<Component> tooltipLines = original.call(instance, i, list, mutablecomponent);
        final List<String> aliases = SearchAlias.getAliases(instance);
        if (aliases.isEmpty()) {
            return tooltipLines;
        }
        // hidden tooltips come back as immutable lists
        tooltipLines = new ArrayList<>(tooltipLines);
        tooltipLines.addAll(aliases.stream().map(Component::literal).toList());
        return tooltipLines;
    }

}
