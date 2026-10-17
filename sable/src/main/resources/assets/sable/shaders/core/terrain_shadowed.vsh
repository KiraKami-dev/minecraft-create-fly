#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <minecraft:chunksection.glsl>
#moj_import <minecraft:projection.glsl>

// minecraft:core/terrain, with the sky light darkened below sub-levels (SableSkyLightShadows).
layout(std140) uniform SableShadow {
    // xyz: camera-relative position of the shadow map camera, w: half size of the shadow volume
    vec4 SableShadowOrigin;
};

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler2;
// Top-down orthographic depth of all sub-levels, see SableSkyLightShadows
uniform sampler2D SableShadowSampler;

out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;

vec4 minecraft_sample_lightmap(sampler2D lightMap, ivec2 uv) {
    return texture(lightMap, clamp((uv / 256.0) + 0.5 / 16.0, vec2(0.5 / 16.0), vec2(15.5 / 16.0)));
}

float sable_sky_light_scale(vec3 pos) {
    float volumeSize = SableShadowOrigin.w;
    vec3 shadowOrigin = SableShadowOrigin.xyz;
    vec2 shadowUv = ((pos.xz - shadowOrigin.xz) * vec2(1.0, -1.0) + volumeSize) / (volumeSize * 2.0);

    float sampleSum = 0.0;
    const int sampleRadius = 3;
    for (int i = -sampleRadius; i <= sampleRadius; i++) {
        for (int j = -sampleRadius; j <= sampleRadius; j++) {
            float depthSample = texture(SableShadowSampler, shadowUv + vec2(i, j) / (volumeSize * 2.0)).r;

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
}

void main() {
    vec3 pos = Position + (ChunkPosition - CameraBlockPos) + CameraOffset;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    sphericalVertexDistance = fog_spherical_distance(pos);
    cylindricalVertexDistance = fog_cylindrical_distance(pos);

    ivec2 lightUv = ivec2(vec2(UV2) * vec2(1.0, sable_sky_light_scale(pos)));
    vertexColor = Color * minecraft_sample_lightmap(Sampler2, lightUv);
    texCoord0 = UV0;
}
