package dev.simulated_team.simulated.mixin.diagram;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import dev.simulated_team.simulated.mixin_interface.diagram.LightTextureExtension;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.OptionalInt;

/**
 * Swaps the light texture for the flat "diagram" light curve while contraption diagrams and end sea shadows render.
 * <p>
 * On 1.21.11 the light texture is a GPU render target without CPU pixels, so the curve is written into a small texture
 * once and blitted over the light texture. {@link LightTexture#updateLightTexture} restores it, since the update flag is
 * set.
 */
@Mixin(LightTexture.class)
public abstract class LightTextureMixin implements LightTextureExtension {

    @Shadow
    private boolean updateLightTexture;

    @Shadow
    @Final
    private GpuTextureView textureView;

    @Unique
    private GpuTexture simulated$diagramTexture;

    @Unique
    private GpuTextureView simulated$diagramTextureView;

    @Unique
    private float simulated$diagramBrightness = Float.NaN;

    @Unique
    private static void simulated$clampColor(final Vector3f color) {
        color.set(Mth.clamp(color.x, 0.0F, 1.0F), Mth.clamp(color.y, 0.0F, 1.0F), Mth.clamp(color.z, 0.0F, 1.0F));
    }

    @Unique
    private static float simulated$notGamma(final float value) {
        final float inverse = 1.0F - value;
        return 1.0F - inverse * inverse * inverse * inverse;
    }

    @Unique
    private static float simulated$getBrightness(final int lightLevel) {
        final float f = (float)lightLevel / 15.0F;
        return f / (4.0F - 3.0F * f);
    }

    @Override
    public void simulated$makeDiagramLightTexture(final float brightnessMultiplier) {
        final CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();

        if (this.simulated$diagramTexture == null) {
            this.simulated$diagramTexture = RenderSystem.getDevice().createTexture(() -> "Simulated diagram light texture",
                    GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING, TextureFormat.RGBA8, 16, 16, 1, 1);
            this.simulated$diagramTextureView = RenderSystem.getDevice().createTextureView(this.simulated$diagramTexture);
        }

        if (this.simulated$diagramBrightness != brightnessMultiplier) {
            this.simulated$diagramBrightness = brightnessMultiplier;

            try (final NativeImage pixels = new NativeImage(16, 16, false)) {
                final Vector3f color = new Vector3f();

                for (int x = 0; x < 16; ++x) {
                    for (int y = 0; y < 16; ++y) {
                        final float brightness = simulated$getBrightness(y) * 0.6f + 0.15f; // block-light curve for diagrams
                        final float brightnessG = brightness * ((brightness * 0.6F + 0.4F) * 0.6F + 0.4F);
                        final float brightnessB = brightness * (brightness * brightness * 0.6F + 0.4F);
                        color.set(brightness, brightnessG, brightnessB);
                        color.lerp(new Vector3f(0.99F, 1.12F, 1.0F), 0.25F);
                        simulated$clampColor(color);

                        final float gamma = 0.55f;
                        final Vector3f notGamma = new Vector3f(simulated$notGamma(color.x), simulated$notGamma(color.y), simulated$notGamma(color.z));
                        color.lerp(notGamma, Math.max(0.0F, gamma));
                        color.lerp(new Vector3f(0.75F, 0.75F, 0.75F), 0.04F);
                        simulated$clampColor(color);
                        color.mul(255.0F);
                        color.mul(brightnessMultiplier);

                        final int r = (int) color.x();
                        final int g = (int) color.y();
                        final int b = (int) color.z();
                        pixels.setPixelABGR(y, x, -16777216 | b << 16 | g << 8 | r);
                    }
                }

                encoder.writeToTexture(this.simulated$diagramTexture, pixels);
            }
        }

        try (final RenderPass renderPass = encoder.createRenderPass(() -> "Simulated diagram light texture", this.textureView, OptionalInt.empty())) {
            renderPass.setPipeline(RenderPipelines.TRACY_BLIT);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.bindTexture("InSampler", this.simulated$diagramTextureView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            renderPass.draw(0, 3);
        }

        this.updateLightTexture = true;
    }

    @Inject(method = "close", at = @At("TAIL"))
    private void simulated$closeDiagramTexture(final CallbackInfo ci) {
        if (this.simulated$diagramTexture != null) {
            this.simulated$diagramTextureView.close();
            this.simulated$diagramTexture.close();
            this.simulated$diagramTexture = null;
            this.simulated$diagramTextureView = null;
        }
    }
}
