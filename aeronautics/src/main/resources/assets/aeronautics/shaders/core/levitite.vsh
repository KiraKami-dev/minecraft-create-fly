#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <minecraft:chunksection.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <aeronautics:levitite.glsl>

// Levitite effect layers on sub-levels. Upstream used a tessellation shader (vertex + tessellation evaluation stages);
// blaze3d has no tessellation, so this draws the compiled solid section meshes of the sub-level again, skips every quad
// that is not textured with a levitite sprite, and does the vertex work of both stages per vertex. The layer 0 shimmer
// noise moved to the fragment shader.

// Same block as sable:core/sub_level_terrain
layout(std140) uniform SableSubLevel {
    mat4 SubLevelPose;
    vec4 SubLevelFaceShadeA;
    vec4 SubLevelFaceShadeB;
    vec4 SubLevelParams;
};

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler2;

out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;
out vec3 localPosition;
out float ghostLayerFullness;
out float viewDepth;

vec4 minecraft_sample_lightmap(sampler2D lightMap, ivec2 uv) {
    return texture(lightMap, clamp((uv / 256.0) + 0.5 / 16.0, vec2(0.5 / 16.0), vec2(15.5 / 16.0)));
}

float sable_block_brightness(vec3 worldNormal) {
    float darkFromD = pow(clamp(-worldNormal.y, 0.0, 1.0), 3.0) * SubLevelFaceShadeA.x;
    float darkFromU = pow(clamp(worldNormal.y, 0.0, 1.0), 3.0) * SubLevelFaceShadeA.y;
    float darkFromN = pow(clamp(-worldNormal.z, 0.0, 1.0), 2.0) * SubLevelFaceShadeA.z;
    float darkFromS = pow(clamp(worldNormal.z, 0.0, 1.0), 2.0) * SubLevelFaceShadeA.w;
    float darkFromW = pow(clamp(-worldNormal.x, 0.0, 1.0), 2.0) * SubLevelFaceShadeB.x;
    float darkFromE = pow(clamp(worldNormal.x, 0.0, 1.0), 2.0) * SubLevelFaceShadeB.y;

    return darkFromD + darkFromU + darkFromN + darkFromS + darkFromW + darkFromE;
}

void main() {
    texCoord0 = UV0;
    vertexColor = vec4(0.0);
    sphericalVertexDistance = 0.0;
    cylindricalVertexDistance = 0.0;
    localPosition = vec3(0.0);
    ghostLayerFullness = 0.0;
    viewDepth = 0.0;

    if (!levitite_is_levitite_uv(UV0)) {
        // Not a levitite quad: collapse it outside of the clip volume
        gl_Position = vec4(0.0, 0.0, -2.0, 1.0);
        return;
    }

    vec3 plotPos = Position + vec3(ChunkPosition);
    vec3 localPos = plotPos + AnchorOffset.xyz;
    float layerIndex = LayerParams.x;
    float onSublevel = LayerParams.y;

    vec3 vel = getVelocity(localPos);
    float l = onSublevel > 0.0 ? length(vel) : 0.0;
    float falloff = exp(-l / levitite_transition_speed());
    float gravityScale = 1.0 - exp(-levitite_gravity_strength() / 3.0);
    ghostLayerFullness = (1.0 - falloff * falloff) * gravityScale;

    if (layerIndex != 0.0) {
        // ghost layers trail along the local velocity (upstream vertex stage)
        vec3 trail = vel * (1.0 - exp(-l / levitite_transition_speed())) / (l + 0.001);
        plotPos += trail * onSublevel * layerIndex * 0.15;

        // and wobble with noise (upstream tessellation evaluation stage)
        vec3 p = localPos / 50.0;
        float t = levitite_time() / 200.0;

        //drag vector has low speed component, complement of that, and lastly the transition spike
        vec3 drag = getDragState(vel);
        //these are mapped to the noise textures in the order of mid, low and high frequency
        //such that the noise is fairly mobile at low speeds, frantic at the transition and calm at higher speeds
        vec3 modifier = drag.yxz * vec3(0.4, 0.3, 0.2);

        vec3 nudge = getNoiseFromPositionAndTime(vec4(p, t), modifier);
        if (l > 0.0001) { //squish the noise offsets to move more along the direction of motion as the speed increases
            nudge = mix(vel * dot(nudge, vel) / dot(vel, vel), nudge, mix(1.0, 0.1, (1.0 - falloff) * (1.0 - falloff)));
        }
        nudge *= ghostLayerFullness;
        plotPos += nudge;
    }

    vec3 pos = (SubLevelPose * vec4(plotPos, 1.0)).xyz;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
    //slight offset towards the camera to avoid z-fighting with the stationary fallback blocks
    gl_Position.z -= 0.00001;
    viewDepth = gl_Position.z;

    sphericalVertexDistance = fog_spherical_distance(pos) * SubLevelParams.z;
    cylindricalVertexDistance = fog_cylindrical_distance(pos) * SubLevelParams.z;

    ivec2 lightUv = ivec2(vec2(UV2) * vec2(1.0, SubLevelParams.y));
    vertexColor = Color * minecraft_sample_lightmap(Sampler2, lightUv);

    if (SubLevelParams.x > 0.5) {
        vec3 worldNormal = normalize(mat3(SubLevelPose) * Normal);
        vertexColor.rgb *= sable_block_brightness(worldNormal);
    }

    localPosition = localPos;
}
