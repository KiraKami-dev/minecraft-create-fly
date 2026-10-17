package dev.simulated_team.simulated.mixin.end_sea;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.shaders.ShaderType;
import dev.simulated_team.simulated.content.end_sea.EndSeaFadeTransformer;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Patches the end sea fade into Sable's sub-level terrain shader while shader sources load. Replaces the Veil shader
 * pre-processor registration of upstream.
 */
@Mixin(ShaderManager.class)
public class ShaderManagerMixin {

    @ModifyExpressionValue(method = "loadShader", at = @At(value = "INVOKE", target = "Ljava/lang/String;join(Ljava/lang/CharSequence;Ljava/lang/Iterable;)Ljava/lang/String;"))
    private static String simulated$patchSubLevelTerrain(final String source, @Local(argsOnly = true) final Identifier location, @Local(argsOnly = true) final ShaderType type) {
        return EndSeaFadeTransformer.transform(location, type, source);
    }

}
