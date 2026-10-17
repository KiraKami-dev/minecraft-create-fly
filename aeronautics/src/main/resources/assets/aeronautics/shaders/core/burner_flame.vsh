#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

// Port of the Veil program aeronautics:burner_flame. The per-burner uniforms travel in the vertices
// (see HotAirBurnerRenderer): Color.r = Intensity, Color.g = Palette, UV2.x = FlameRenderTime / FLAME_TIME_PERIOD * 32767.

const float FLAME_TIME_PERIOD = 10.0 / 3.0;

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;

out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec2 texCoord0;
out float flameRenderTime;
out float intensity;
out float palette;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);

    texCoord0 = UV0;
    intensity = Color.r;
    palette = Color.g;
    flameRenderTime = float(UV2.x) / 32767.0 * FLAME_TIME_PERIOD;
}
