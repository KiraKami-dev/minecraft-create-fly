#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <minecraft:chunksection.glsl>
#moj_import <minecraft:projection.glsl>

// Sub-level terrain. Same inputs and outputs as minecraft:core/terrain, but the section position is relative to an
// anchor block of the sub-level (ChunkPosition) and the sub-level pose is applied before the camera rotation.
layout(std140) uniform SableSubLevel {
    // Anchor-relative plot position -> camera-relative world position
    mat4 SubLevelPose;
    // Block face brightness: down, up, north, south
    vec4 SubLevelFaceShadeA;
    // Block face brightness: west, east, unused, unused
    vec4 SubLevelFaceShadeB;
    // x: dynamic directional shading (0 or 1), y: sky light scale, z: fog (0 or 1), w: unused
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
    vec3 pos = (SubLevelPose * vec4(Position + vec3(ChunkPosition), 1.0)).xyz;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    sphericalVertexDistance = fog_spherical_distance(pos) * SubLevelParams.z;
    cylindricalVertexDistance = fog_cylindrical_distance(pos) * SubLevelParams.z;

    ivec2 lightUv = ivec2(vec2(UV2) * vec2(1.0, SubLevelParams.y));
    vertexColor = Color * minecraft_sample_lightmap(Sampler2, lightUv);

    if (SubLevelParams.x > 0.5) {
        vec3 worldNormal = normalize(mat3(SubLevelPose) * Normal);
        vertexColor.rgb *= sable_block_brightness(worldNormal);
    }

    texCoord0 = UV0;
}
