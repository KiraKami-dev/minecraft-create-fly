#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

// Port of the Veil program aeronautics:hot_air_overlay
layout(std140) uniform HotAirInfo {
    // x: scroll, y: cutoff y (local to the balloon mesh origin)
    vec4 HotAirParams;
};

in vec3 Position;
in vec2 UV0;
in vec4 Color;
in vec3 Normal;

out vec2 texCoord0;
out vec4 vertexColor;
out float vertY;
out float directionless;

const vec3 UP = vec3(0.0, 1.0, 0.0);

float pixelAlign(float a) {
    return floor(a * 16.0) / 16.0;
}

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    directionless = abs(dot(Normal, UP));

    vec2 uv = UV0 + vec2(0.0, pixelAlign(HotAirParams.x * (1.0 - directionless * 0.4)));
    if (directionless != 0.0) {
        uv /= vec2(1.0, 20.0);
    }
    texCoord0 = uv;
    vertexColor = Color * vec4(vec3(1.0), clamp(Position.y - HotAirParams.y, 0.0, 1.0));
    vertY = Position.y;
}
