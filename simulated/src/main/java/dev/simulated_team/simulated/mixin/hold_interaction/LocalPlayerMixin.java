package dev.simulated_team.simulated.mixin.hold_interaction;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import dev.simulated_team.simulated.util.hold_interaction.HoldInteractionManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin extends AbstractClientPlayer {
    public LocalPlayerMixin(final ClientLevel clientLevel, final GameProfile gameProfile) {
        super(clientLevel, gameProfile);
    }

    @Inject(method = "isShiftKeyDown", at = @At("RETURN"), cancellable = true)
    private void simulated$handlerShiftBlock(final CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && !HoldInteractionManager.canCrouch()) {
            cir.setReturnValue(false);
        }
    }

    // 1.21.11 sends the shift key to the server as part of ServerboundPlayerInputPacket(input.keyPresses), compared
    // against lastSentInput every tick (upstream wrapped Input.shiftKeyDown, which only the riding packet read).
    // Masking every keyPresses read in tick() keeps the packet, its change detection and lastSentInput consistent.
    @WrapOperation(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/ClientInput;keyPresses:Lnet/minecraft/world/entity/player/Input;", opcode = 180/* GETFIELD */))
    private Input simulated$shhhhDontTellTheServer(final ClientInput instance, final Operation<Input> original) {
        final Input input = original.call(instance);
        if (input.shift() && !HoldInteractionManager.canCrouch()) {
            return new Input(input.forward(), input.backward(), input.left(), input.right(), input.jump(), false, input.sprint());
        }
        return input;
    }
}
