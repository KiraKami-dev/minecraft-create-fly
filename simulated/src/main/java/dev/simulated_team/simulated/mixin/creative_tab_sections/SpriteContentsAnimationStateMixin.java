package dev.simulated_team.simulated.mixin.creative_tab_sections;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.simulated_team.simulated.mixin_interface.TickerExtension;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

// 1.21.11: SpriteContents$Ticker.tickAndUpload became SpriteContents$AnimationState.tick (the atlas draws dirty frames)
@Mixin(SpriteContents.AnimationState.class)
public class SpriteContentsAnimationStateMixin implements TickerExtension {

    @Unique
    private boolean simulated$playing = true;

    @WrapOperation(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/texture/SpriteContents$AnimationState;frame:I", opcode = 181/* PUTFIELD */))
    private void simulated$putField(final SpriteContents.AnimationState instance, final int value, final Operation<Void> original) {
        if (this.simulated$isPlaying()) {
            original.call(instance, value);
        }
    }

    @Override
    public void simulated$setPlaying(final boolean playing) {
        this.simulated$playing = playing;
    }

    @Override
    public boolean simulated$isPlaying() {
        return this.simulated$playing;
    }

}
