package dev.simulated_team.simulated.fabric.mixin.client;

import dev.simulated_team.simulated.events.SimulatedCommonClientEvents;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NeoForge's {@code InputEvent.Key}: fired after the game handled a key press.
 */
@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "keyPress", at = @At("TAIL"))
    private void simulated$afterKeyPress(final long window, final int action, final KeyEvent keyEvent, final CallbackInfo ci) {
        if (window == this.minecraft.getWindow().handle()) {
            SimulatedCommonClientEvents.onAfterKeyPress(keyEvent.key(), keyEvent.scancode(), action, keyEvent.modifiers());
        }
    }
}
