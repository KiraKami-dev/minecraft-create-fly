package dev.ryanhcode.sable.fabric.mixin.compatibility.create.flywheel_lib;

import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.fabric.compatibility.create.flywheel.SableFlywheelShaderOverrides;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Makes Flywheel load Sable's replacement shaders ({@code assets/sable/flywheel_overrides/...}) instead of its own copies.
 * Every shader source, whether listed up-front or pulled in through an {@code #include}, is read through
 * {@code ShaderSources.SourceFinder#readResource}.
 */
@Mixin(targets = "com.zurrtum.create.client.flywheel.backend.glsl.ShaderSources$SourceFinder")
public class ShaderSourcesMixin {

    @Shadow(remap = false)
    @Final
    private ResourceManager manager;

    @ModifyVariable(method = "readResource", at = @At("HEAD"), argsOnly = true, remap = false)
    private Resource sable$useSableShaderOverride(final Resource resource, @Local(argsOnly = true) final Identifier location) {
        return SableFlywheelShaderOverrides.apply(this.manager, location, resource);
    }
}
