package dev.simulated_team.simulated.mixin_interface;

import net.minecraft.client.renderer.texture.SpriteContents;
import org.jetbrains.annotations.Nullable;

/**
 * Gives access to the animation of a sprite. On 1.21.11 {@code SpriteContents.Ticker} became
 * {@link SpriteContents.AnimationState} (created by {@code SpriteContents#createAnimationState}); every animation state
 * implements {@link TickerExtension} (creative_tab_sections mixins).
 */
public interface SpriteContentsExtension {
    /**
     * @return the animation state of this sprite, or null if it is not animated
     */
    @Nullable
    TickerExtension simulated$getTicker();

    void simulated$setTicker(@Nullable SpriteContents.AnimationState ticker);
}
