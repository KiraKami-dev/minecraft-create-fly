#version 330

// Port of pinwheel/shaders/program/laser/laser.vsh (Veil) to a 1.21.11 core shader.

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec2 UV0;
in vec4 Color;

out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec2 lengthData;
out vec4 vertexColor;

void main() {
    vec3 pos = Position + ModelOffset;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    sphericalVertexDistance = fog_spherical_distance(pos);
    cylindricalVertexDistance = fog_cylindrical_distance(pos);
    lengthData = UV0;
    vertexColor = Color;
}
