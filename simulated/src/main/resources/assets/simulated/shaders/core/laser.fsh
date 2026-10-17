#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec2 lengthData;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    // the end of the beam tapers off
    float endTaper = (lengthData.x - 1.0) / (lengthData.y - 1.0);
    vec4 color = vertexColor;
    color.a *= (1.0 - max(endTaper, 0.0));

    // fades out into the fog
    fragColor = color * ColorModulator * (1.0 - total_fog_value(sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd));
}
