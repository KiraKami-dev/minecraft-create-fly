package dev.ryanhcode.sable.mixin.sublevel_render.impl.sodium;

import com.mojang.blaze3d.opengl.GlSampler;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.ryanhcode.sable.SableClient;
import dev.ryanhcode.sable.mixinterface.sublevel_render.sodium.GlBufferAccessor;
import dev.ryanhcode.sable.render.sky_light_shadow.SableSkyLightShadows;
import dev.ryanhcode.sable.render.water_occlusion.WaterOcclusionRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.DefaultShaderInterface;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL31C;
import org.lwjgl.opengl.GL33C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Feeds the water occlusion depth buffers and the sky light shadow map to the patched Sodium chunk shaders
 * (see {@link ShaderLoaderMixin}). Sodium drives its program with raw GL, so this does too, on texture units
 * and a uniform buffer binding Sodium doesn't use.
 */
@Mixin(value = DefaultShaderInterface.class, remap = false)
public class DefaultShaderInterfaceMixin {

    private static final int SABLE_CLOSE_UNIT = 2;
    private static final int SABLE_FAR_UNIT = 3;
    private static final int SABLE_SHADOW_UNIT = 4;
    private static final int SABLE_SHADOW_BINDING = 5;

    @Inject(method = "setupState", at = @At("TAIL"))
    private void sable$setupState(final TerrainRenderPass pass, final FogParameters parameters, final GpuSampler terrainSampler, final CallbackInfo ci) {
        final int program = GL20C.glGetInteger(GL20C.GL_CURRENT_PROGRAM);
        final GpuSampler nearest = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);

        final WaterOcclusionRenderer water = SableClient.WATER_OCCLUSION_RENDERER;
        final boolean occlude = pass.isTranslucent() && water.isActive();
        GL20C.glUniform1i(GL20C.glGetUniformLocation(program, "u_SableOcclude"), occlude ? 1 : 0);
        if (occlude) {
            sable$bind(SABLE_CLOSE_UNIT, water.getCloseDepthView(), nearest);
            sable$bind(SABLE_FAR_UNIT, water.getFarDepthView(), nearest);
            GL20C.glUniform1i(GL20C.glGetUniformLocation(program, "u_SableCloseDepth"), SABLE_CLOSE_UNIT);
            GL20C.glUniform1i(GL20C.glGetUniformLocation(program, "u_SableFarDepth"), SABLE_FAR_UNIT);
        }

        final boolean shadow = SableSkyLightShadows.isActive();
        GL20C.glUniform1i(GL20C.glGetUniformLocation(program, "u_SableShadowOn"), shadow ? 1 : 0);
        if (shadow) {
            sable$bind(SABLE_SHADOW_UNIT, SableSkyLightShadows.getDepthView(), nearest);
            GL20C.glUniform1i(GL20C.glGetUniformLocation(program, "u_SableShadowDepth"), SABLE_SHADOW_UNIT);
            GL31C.glUniformBlockBinding(program, GL31C.glGetUniformBlockIndex(program, "SableShadow"), SABLE_SHADOW_BINDING);
            GL30C.glBindBufferBase(GL31C.GL_UNIFORM_BUFFER, SABLE_SHADOW_BINDING, ((GlBufferAccessor) SableSkyLightShadows.getUniformBuffer()).sable$getHandle());
        }
    }

    @Inject(method = "resetState", at = @At("TAIL"))
    private void sable$resetState(final CallbackInfo ci) {
        for (int unit = SABLE_CLOSE_UNIT; unit <= SABLE_SHADOW_UNIT; unit++) {
            GL33C.glBindSampler(unit, 0);
        }
    }

    private static void sable$bind(final int unit, final GpuTextureView view, final GpuSampler sampler) {
        GlStateManager._activeTexture(GL20C.GL_TEXTURE0 + unit);
        GlStateManager._bindTexture(((GlTexture) view.texture()).glId());
        GL33C.glBindSampler(unit, ((GlSampler) sampler).getId());
    }
}
