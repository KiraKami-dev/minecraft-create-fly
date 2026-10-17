#version 330

// Fullscreen triangle for the contraption diagram post pass (ported from the Veil program)
layout(std140) uniform DiagramInfo {
    vec4 LineColor;
    vec4 LineShadowColor;
    vec2 InSize;
    float PaletteOffset;
    float FadeScale;
};

out vec2 texCoord;
out vec2 oneTexel;

void main() {
    vec2 uv = vec2(gl_VertexID & 1, gl_VertexID & 2);
    gl_Position = vec4(uv * vec2(3.0) - vec2(1.0), 0.0, 1.0);
    texCoord = uv * vec2(1.5);
    oneTexel = 1.0 / InSize;
}
