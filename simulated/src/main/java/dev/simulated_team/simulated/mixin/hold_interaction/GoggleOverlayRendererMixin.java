package dev.simulated_team.simulated.mixin.hold_interaction;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.client.content.equipment.goggles.GoggleOverlayRenderer;
import dev.simulated_team.simulated.index.SimClickInteractions;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// The Mth.clamp target is a Minecraft member, so these injectors keep remapping on (remap = false would leave the
// descriptor unmapped in production); renderOverlay itself is a Create Fly method and keeps its name.
@Mixin(GoggleOverlayRenderer.class)
public class GoggleOverlayRendererMixin {

    @Shadow(remap = false) public static int hoverTicks;

    @Inject(method = "renderOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(FFF)F", shift = At.Shift.BEFORE))
    private static void decrementRenderTicks(final CallbackInfo ci) {
        if (SimClickInteractions.STEERING_WHEEL_MANAGER.isActive()) {
            hoverTicks = Mth.clamp(hoverTicks - 2, 0, 24);
        }
    }

    @WrapOperation(method = "renderOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(FFF)F"))
    private static float fixPartialTicks(final float value, final float min, final float max, final Operation<Float> original, @Local(argsOnly = true) final DeltaTracker deltaTracker) {
        if (SimClickInteractions.STEERING_WHEEL_MANAGER.isActive()) {
            return Mth.clamp(hoverTicks - deltaTracker.getGameTimeDeltaTicks(), 0, 24) / 24;
        }
        return original.call(value, min, max);
    }

    @Inject(method = "renderOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(FFF)F"), cancellable = true)
    private static void dontRenderTheText(final Minecraft mc, final GuiGraphics guiGraphics, final DeltaTracker deltaTracker, final CallbackInfo ci) {
        if (hoverTicks - deltaTracker.getGameTimeDeltaTicks() <= 0) {
            // renderOverlay pushed the GUI pose right before this point
            guiGraphics.pose().popMatrix();
            ci.cancel();
        }
    }
}
