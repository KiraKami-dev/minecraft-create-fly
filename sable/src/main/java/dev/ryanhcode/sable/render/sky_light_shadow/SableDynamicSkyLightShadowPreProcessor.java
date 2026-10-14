package dev.ryanhcode.sable.render.sky_light_shadow;

/**
 * Names of the sky light shadow shader inputs.
 * <p>
 * PORT-TODO(1.21.11): In 1.21.1 this was a glslprocessor pre-processor that rewrote the chunk vertex shaders to
 * sample the shadow map in {@code minecraft_sample_lightmap}. 1.21.11 has no shader pre-processing, so the injection
 * (the old body, see git history) would have to become a variant of the terrain vertex shader in a custom
 * {@code RenderPipeline}. See {@link SableSkyLightShadows}.
 */
public class SableDynamicSkyLightShadowPreProcessor {
    public static final String SAMPLER_NAME = "SableShadowSampler";
    public static final String SHADOW_VOLUME_SIZE_UNIFORM = "SableShadowVolumeSize";
    public static final String ENABLE_UNIFORM = "SableShadowsEnabled";
    public static final String SHADOW_ORIGIN_UNIFORM = "SableShadowOrigin";

    private SableDynamicSkyLightShadowPreProcessor() {
    }
}
