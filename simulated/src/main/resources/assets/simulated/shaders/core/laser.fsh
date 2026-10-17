#version 330

// Port of pinwheel/shaders/program/laser/laser.fsh (Veil) to a 1.21.11 core shader.

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
// x always     (length + 0.5) / length
// y ranges [0, (length + 0.5) / length]
in vec2 lengthData;
in vec4 vertexColor;

out vec4 fragColor;

// Veil's linear_fog_fade: lasers fade out in fog instead of taking the fog color
float fog_fade(float vertexDistance, float fogStart, float fogEnd) {
    if (vertexDistance <= fogStart) {
        return 1.0;
    } else if (vertexDistance >= fogEnd) {
        return 0.0;
    }

    return smoothstep(fogEnd, fogStart, vertexDistance);
}

void main() {
    float endTaper = (lengthData.x - 1.0) / (lengthData.y - 1.0);
    vec4 color = vertexColor;
    color.a *= (1.0 - max(endTaper, 0.0));

    float fade = min(fog_fade(sphericalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd),
                     fog_fade(cylindricalVertexDistance, FogRenderDistanceStart, FogRenderDistanceEnd));
    fragColor = color * ColorModulator * fade;
}
