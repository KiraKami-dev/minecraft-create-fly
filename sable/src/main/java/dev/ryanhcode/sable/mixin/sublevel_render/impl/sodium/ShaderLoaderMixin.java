package dev.ryanhcode.sable.mixin.sublevel_render.impl.sodium;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.caffeinemc.mods.sodium.client.gl.shader.ShaderLoader;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Adds Sable's water occlusion and sky light shadows to Sodium's chunk shaders. Both are gated by uniforms set per
 * pass in {@link DefaultShaderInterfaceMixin}, and mirror {@code terrain_occluded.fsh} and {@code terrain_shadowed.vsh}.
 */
@Mixin(value = ShaderLoader.class, remap = false)
public class ShaderLoaderMixin {

    @ModifyReturnValue(method = "getShaderSource", at = @At("RETURN"))
    private static String sable$patchChunkShader(final String source, final Identifier name) {
        final String path = name.getPath();
        if (path.equals("blocks/block_layer_opaque.fsh")) {
            return source
                    .replace("out vec4 fragColor;", """
                            out vec4 fragColor;

                            uniform bool u_SableOcclude;
                            uniform sampler2D u_SableCloseDepth;
                            uniform sampler2D u_SableFarDepth;""")
                    .replace("void main() {", """
                            void main() {
                                if (u_SableOcclude) {
                                    // the far buffer is rendered mirrored in x, see WaterOcclusionRenderer
                                    vec2 sableUv = gl_FragCoord.xy / vec2(textureSize(u_SableCloseDepth, 0));
                                    float sableCloseDepth = texture(u_SableCloseDepth, sableUv).r;
                                    float sableFarDepth = texture(u_SableFarDepth, vec2(1.0 - sableUv.x, sableUv.y)).r;
                                    if (gl_FragCoord.z > sableCloseDepth && gl_FragCoord.z < sableFarDepth) {
                                        discard;
                                    }
                                }
                            """);
        }

        if (path.equals("blocks/block_layer_opaque.vsh")) {
            return source
                    .replace("out vec4 v_Color;", """
                            out vec4 v_Color;

                            layout(std140) uniform SableShadow {
                                // xyz: camera-relative position of the shadow map camera, w: half size of the shadow volume
                                vec4 SableShadowOrigin;
                            };
                            uniform bool u_SableShadowOn;
                            uniform sampler2D u_SableShadowDepth;

                            float sable_sky_light_scale(vec3 pos) {
                                float volumeSize = SableShadowOrigin.w;
                                vec3 shadowOrigin = SableShadowOrigin.xyz;
                                vec2 shadowUv = ((pos.xz - shadowOrigin.xz) * vec2(1.0, -1.0) + volumeSize) / (volumeSize * 2.0);

                                float sampleSum = 0.0;
                                const int sampleRadius = 3;
                                for (int i = -sampleRadius; i <= sampleRadius; i++) {
                                    for (int j = -sampleRadius; j <= sampleRadius; j++) {
                                        float depthSample = texture(u_SableShadowDepth, shadowUv + vec2(i, j) / (volumeSize * 2.0)).r;

                                        // the shadow camera uses a near plane of 0.5 and a far plane of volumeSize
                                        float depth = 0.5 + depthSample * (volumeSize - 0.5);
                                        float y = shadowOrigin.y - depth;

                                        if (y >= pos.y) {
                                            float strength = clamp((y - pos.y - 2.0) / 15.0, 0.0, 1.0);
                                            sampleSum += 0.6 * strength;
                                        }
                                    }
                                }

                                float average = sampleSum / float((sampleRadius * 2 + 1) * (sampleRadius * 2 + 1));
                                return smoothstep(0.0, 1.0, 1.0 - average);
                            }""")
                    .replace("texture(u_LightTex, _vert_tex_light_coord)",
                            "texture(u_LightTex, u_SableShadowOn ? _vert_tex_light_coord * vec2(1.0, sable_sky_light_scale(position)) : _vert_tex_light_coord)");
        }

        return source;
    }
}
