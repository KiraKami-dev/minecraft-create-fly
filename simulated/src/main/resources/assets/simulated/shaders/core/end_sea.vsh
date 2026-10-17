#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

// End sea layers (ported from the Veil program simulated:end_sea)
layout(std140) uniform EndSeaInfo {
    float ShadowVolumeSize;
    float StartY;
    float CameraY;
};

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;

out vec2 texCoord;
out vec2 shadowTexCoord;
out vec4 vertexColor;
out float planeY;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color;
    texCoord = UV0;
    shadowTexCoord = UV2;
    planeY = Position.y + CameraY;
}
