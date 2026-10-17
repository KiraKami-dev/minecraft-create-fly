package dev.simulated_team.simulated.mixin.conditional_display_target;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.api.behaviour.display.DisplayTarget;
import com.zurrtum.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import com.zurrtum.create.content.redstone.displayLink.DisplayLinkContext;
import com.zurrtum.create.client.content.redstone.displayLink.DisplayLinkScreen;
import com.zurrtum.create.client.foundation.gui.widget.Label;
import dev.simulated_team.simulated.api.ConditionalDisplayTarget;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DisplayLinkScreen.class)
public class DisplayLinkScreenMixin {
    @Shadow(remap = false) private DisplayLinkBlockEntity blockEntity;

    @Shadow(remap = false) Label targetLineLabel;

    // Component (a Minecraft class) is in the target descriptor, so the @At keeps remap = true
    @WrapOperation(method = "initGathererOptions", at = @At(value = "INVOKE", target = "Lcom/zurrtum/create/api/behaviour/display/DisplayTarget;getLineOptionText(I)Lnet/minecraft/network/chat/Component;", remap = true), remap = false)
    private Component simulated$displayConditionalError(final DisplayTarget instance, final int line, final Operation<Component> original, @Local final ClientLevel level) {
        final DisplayLinkContext context = new DisplayLinkContext(level, this.blockEntity);
        if (instance instanceof final ConditionalDisplayTarget cdt && !cdt.allowsWriting(context)) {
            // 1.21.11 text colours need an alpha channel
            this.targetLineLabel.colored(0xFF000000 | ChatFormatting.GRAY.getColor());
            return cdt.getErrorMessage(context);
        }
        return original.call(instance, line);
    }
}
