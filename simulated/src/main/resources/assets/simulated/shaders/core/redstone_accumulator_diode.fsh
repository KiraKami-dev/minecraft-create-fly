#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in float litFrac;
in vec4 vertexColor;
in vec4 vertexLight;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    // left half of the diode sprite: lit by the world, right half (16 texels further): glowing
    vec4 colA = texture(Sampler0, texCoord0) * vertexLight;
    vec4 colB = texture(Sampler0, texCoord0 + vec2(16.0, 0.0) / vec2(textureSize(Sampler0, 0)));
    vec4 color = mix(colB, colA, litFrac) * vertexColor * ColorModulator;
    if (color.a < 0.1) {
        discard;
    }
    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
