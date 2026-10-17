package dev.simulated_team.simulated.mixin_interface;

import net.minecraft.client.renderer.texture.SpriteContents;

/**
 * Gives access to the animation of a sprite. On 1.21.11 {@code SpriteContents.Ticker} became
 * {@link SpriteContents.AnimationState} (created by {@code SpriteContents#createAnimationState}).
 */
public interface SpriteContentsExtension {
    SpriteContents.AnimationState simulated$getTicker();
    void simulated$setTicker(SpriteContents.AnimationState ticker);
}
