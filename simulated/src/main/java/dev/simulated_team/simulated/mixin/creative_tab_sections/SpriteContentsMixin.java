package dev.simulated_team.simulated.mixin.creative_tab_sections;

import dev.simulated_team.simulated.mixin_interface.SpriteContentsExtension;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpriteContents.class)
public class SpriteContentsMixin implements SpriteContentsExtension {

    @Unique
    private SpriteContents.AnimationState simulated$ticker = null;

    @Override
    public SpriteContents.AnimationState simulated$getTicker() {
        return this.simulated$ticker;
    }

    @Override
    public void simulated$setTicker(final SpriteContents.AnimationState ticker) {
        this.simulated$ticker = ticker;
    }

    // 1.21.11: SpriteContents.Ticker became SpriteContents.AnimationState, created per atlas upload
    @Inject(method = "createAnimationState", at = @At("RETURN"))
    private void simulated$createTicker(final CallbackInfoReturnable<SpriteContents.AnimationState> cir) {
        this.simulated$setTicker(cir.getReturnValue());
    }

}
