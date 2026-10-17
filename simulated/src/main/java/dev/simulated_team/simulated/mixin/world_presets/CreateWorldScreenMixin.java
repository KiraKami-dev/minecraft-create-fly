package dev.simulated_team.simulated.mixin.world_presets;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.simulated_team.simulated.content.worldgen.SimulatedWorldPreset;
import dev.simulated_team.simulated.index.SimWorldPresets;
import dev.simulated_team.simulated.mixin_interface.PrimaryLevelDataExtension;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

// 1.21.11 builds the LevelSettings and PrimaryLevelData in onCreate() (before the experimental-features confirmation)
// and only hands the finished WorldData to createNewWorld(LayeredRegistryAccess, WorldData), so both hooks moved there.
@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin {

    @Shadow
    @Final
    WorldCreationUiState uiState;

    @Inject(method = "onCreate", at = @At("HEAD"))
    private void simulated$createNewWorld(final CallbackInfo ci) {
        final Holder<WorldPreset> holder = this.uiState.getWorldType().preset();
        if (holder == null) {
            return;
        }

        final Optional<ResourceKey<WorldPreset>> key = holder.unwrapKey();
        if (key.isEmpty()) {
            return;
        }

        final Identifier location = key.get().identifier();
        final SimulatedWorldPreset simPreset = SimWorldPresets.PRESETS.get(location);

        if (simPreset != null) {
            final GameRules gameRules = this.uiState.getGameRules();
            simPreset.modifyGameRules(gameRules);
        }
    }

    @ModifyExpressionValue(method = "onCreate", at = @At(value = "NEW", target = "Lnet/minecraft/world/level/storage/PrimaryLevelData;"))
    private PrimaryLevelData simulated$createNewWorld2(final PrimaryLevelData worldData) {
        final Holder<WorldPreset> holder = this.uiState.getWorldType().preset();
        if (holder == null) {
            return worldData;
        }

        final Optional<ResourceKey<WorldPreset>> key = holder.unwrapKey();
        if (key.isEmpty()) {
            return worldData;
        }

        ((PrimaryLevelDataExtension) worldData).setPreset(key.get().identifier());
        if (holder.is(SimWorldPresets.END_SEA.id())) {
            ((PrimaryLevelDataExtension) worldData).setEndDragonFight(new EndDragonFight.Data(false, true, true, false, Optional.empty(), Optional.empty(), Optional.empty()));
        }
        return worldData;
    }
}
