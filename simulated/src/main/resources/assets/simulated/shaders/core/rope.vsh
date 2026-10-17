#version 330

// Port of pinwheel/shaders/program/rope/rope.vsh (Veil) to a 1.21.11 core shader.

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler2;

out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec4 vertexColor;
out vec3 lightmapColor;
out vec2 texCoord0;

vec4 minecraft_sample_lightmap(sampler2D lightMap, ivec2 uv) {
    return texture(lightMap, clamp((uv / 256.0) + 0.5 / 16.0, vec2(0.5 / 16.0), vec2(15.5 / 16.0)));
}

// Veil's block_brightness with the overworld face shades (Veil read them from VeilBlockFaceBrightness:
// down 0.5, up 1.0, north/south 0.8, west/east 0.6)
float block_brightness(vec3 worldNormal) {
    float darkFromD = pow(clamp(-worldNormal.y, 0.0, 1.0), 3.0) * 0.5;
    float darkFromU = pow(clamp(worldNormal.y, 0.0, 1.0), 3.0) * 1.0;
    float darkFromN = pow(clamp(-worldNormal.z, 0.0, 1.0), 2.0) * 0.8;
    float darkFromS = pow(clamp(worldNormal.z, 0.0, 1.0), 2.0) * 0.8;
    float darkFromW = pow(clamp(-worldNormal.x, 0.0, 1.0), 2.0) * 0.6;
    float darkFromE = pow(clamp(worldNormal.x, 0.0, 1.0), 2.0) * 0.6;

    return (darkFromD + darkFromU + darkFromN + darkFromS + darkFromW + darkFromE);
}

void main() {
    vec3 pos = Position + ModelOffset;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    sphericalVertexDistance = fog_spherical_distance(pos);
    cylindricalVertexDistance = fog_cylindrical_distance(pos);
    vertexColor = Color * block_brightness(Normal);
    lightmapColor = minecraft_sample_lightmap(Sampler2, UV2).rgb;
    texCoord0 = UV0;
}
