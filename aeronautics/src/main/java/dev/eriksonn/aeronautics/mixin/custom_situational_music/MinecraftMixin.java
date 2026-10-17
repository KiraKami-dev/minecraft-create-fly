package dev.eriksonn.aeronautics.mixin.custom_situational_music;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.eriksonn.aeronautics.api.CustomSituationalMusic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import javax.annotation.Nullable;

@Mixin(Minecraft.class)
public class MinecraftMixin {

	@Shadow @Nullable public LocalPlayer player;

	@Shadow @Nullable public ClientLevel level;

	/**
	 * 1.21.1 replaced the {@code Musics.GAME} fallback (overworld biome without its own music). On 1.21.11 the music
	 * comes from the {@code BACKGROUND_MUSIC} environment attribute, whose overworld default is {@code Musics.GAME}, so
	 * the selected music is replaced when it is that default.
	 */
	@ModifyExpressionValue(method = "getSituationalMusic", at = @At(value = "INVOKE", target = "Ljava/util/Optional;orElse(Ljava/lang/Object;)Ljava/lang/Object;"))
	private Object aeronautics$getSituationalMusic(final Object original) {
		if (Musics.GAME.equals(original)) {
			final Music customMusic = CustomSituationalMusic.getSituationalMusic(this.level, this.player);

			if (customMusic != null) {
				return customMusic;
			}
		}
		return original;
	}
}
