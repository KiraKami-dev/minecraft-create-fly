package dev.simulated_team.simulated.mixin.tooltip_flag;

import dev.simulated_team.simulated.mixin_interface.tooltip_flag.TooltipFlagExtension;
import net.minecraft.client.multiplayer.SessionSearchTrees;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(SessionSearchTrees.class)
public class SessionSearchTreesMixin {
    // method_60369 is the reload lambda of updateCreativeTooltips (lambdas keep their intermediary names under Mojang
    // mappings, in dev and in production); its only TooltipFlag local is NORMAL.asCreative().
    @ModifyVariable(method = "method_60369", at = @At(value = "STORE"))
    private TooltipFlag markAsCreativeSearch(final TooltipFlag value) {
        ((TooltipFlagExtension) value).simulated$setCreativeSearch(true);
        return value;
    }
}
