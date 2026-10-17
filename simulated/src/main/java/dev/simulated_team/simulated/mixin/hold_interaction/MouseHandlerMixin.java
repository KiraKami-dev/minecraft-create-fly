package dev.simulated_team.simulated.mixin.hold_interaction;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.simulated_team.simulated.events.SimulatedCommonClientEvents;
import dev.simulated_team.simulated.util.SimDistUtil;
import dev.simulated_team.simulated.util.click_interactions.InteractCallback;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    // 1.21.11 dropped the int invert-Y local (both axes are now inverted inline in the turn() arguments), so this wraps
    // the turn call and hands over the deltas the player would turn by, like upstream's (j, k * l).
    @WrapOperation(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void simulated$turnPlayer(final LocalPlayer instance, final double yRot, final double xRot, final Operation<Void> original) {
        if (SimDistUtil.getClientPlayer() != null && !SimDistUtil.getClientPlayer().isSpectator()) {
            final InteractCallback.Result status = SimulatedCommonClientEvents.onMouseMove(yRot, xRot);
            if (status.cancelled()) {
                return;
            }
        }
        original.call(instance, yRot, xRot);
    }

    // 1.21.11: onPress(long, int button, int action, int modifiers) became onButton(long, MouseButtonInfo, int action);
    // the second MouseButtonInfo local is the one after the macOS ctrl-click right-click simulation (upstream read the
    // button parameter after that adjustment).
    @Inject(method = "onButton",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getOverlay()Lnet/minecraft/client/gui/screens/Overlay;", ordinal = 0),
            cancellable = true)
    private void simulated$preOnPress(final long windowPointer, final MouseButtonInfo rawButtonInfo, final int action, final CallbackInfo ci, @Local(ordinal = 1) final MouseButtonInfo buttonInfo) {
        if (SimDistUtil.getClientPlayer() != null && !SimDistUtil.getClientPlayer().isSpectator()) {
            final InteractCallback.Result status = SimulatedCommonClientEvents.onBeforeMouseInput(InteractCallback.Input.mouse(buttonInfo.button()), buttonInfo.modifiers(), action);
            if (status.cancelled()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "onScroll",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getOverlay()Lnet/minecraft/client/gui/screens/Overlay;", ordinal = 0),
            cancellable = true)
    private void simulated$preOnScroll(final long l, final double d, final double e, final CallbackInfo ci, @Local(ordinal = 3) final double deltaX, @Local(ordinal = 4) final double deltaY) {
        if (SimDistUtil.getClientPlayer() != null && !SimDistUtil.getClientPlayer().isSpectator()) {
            final InteractCallback.Result status = SimulatedCommonClientEvents.onMouseScroll(deltaX, deltaY);
            if (status.cancelled()) {
                ci.cancel();
            }
        }
    }
}
