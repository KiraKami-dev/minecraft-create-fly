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
    // mappings, in dev and in production). Minecraft jars carry no local variable table, so the local's type is the
    // inferred one: TooltipFlag.Default (the return type of asCreative()).
    @ModifyVariable(method = "method_60369", at = @At(value = "STORE"))
    private TooltipFlag.Default markAsCreativeSearch(final TooltipFlag.Default value) {
        ((TooltipFlagExtension) (Object) value).simulated$setCreativeSearch(true);
        return value;
    }
}
