package dev.simulated_team.simulated.mixin.world_presets;

import com.llamalad7.mixinextras.sugar.Local;
import dev.simulated_team.simulated.content.worldgen.SimulatedWorldPreset;
import dev.simulated_team.simulated.index.SimWorldPresets;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.screens.worldselection.CreateWorldScreen$WorldTab")
public class WorldTabMixin {

	// Captured by type: Minecraft's jar has no local variable table, and 1.21.11 added locals after the world type
	// button, which the old positional LocalCapture relied on.
	@Inject(method = "<init>", at = @At("TAIL"))
	private void simulated$init(final CreateWorldScreen createWorldScreen, final CallbackInfo ci, @Local final CycleButton<WorldCreationUiState.WorldTypeEntry> cycleButton) {
		createWorldScreen.getUiState()
				.addListener(worldCreationUiState -> {
					final WorldCreationUiState.WorldTypeEntry worldType = worldCreationUiState.getWorldType();
					final Holder<WorldPreset> preset = worldType.preset();

					if (preset != null && preset.unwrapKey().isPresent()) {
						final ResourceKey<WorldPreset> key = preset.unwrapKey().get();
						final SimulatedWorldPreset simPreset = SimWorldPresets.PRESETS.get(key.identifier());

						if(simPreset != null && simPreset.description() != null) {
							cycleButton.setTooltip(Tooltip.create(simPreset.description()));
						}
					}
				});
	}
}
