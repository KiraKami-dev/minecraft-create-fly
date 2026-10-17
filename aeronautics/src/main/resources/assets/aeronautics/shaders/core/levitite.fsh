#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <aeronautics:levitite.glsl>

uniform sampler2D Sampler0;
#ifdef GHOSTS
// Depth of the scene behind the ghost layers (the pass has no depth attachment)
uniform sampler2D DepthSampler;
#endif

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in vec3 localPosition;
in float ghostLayerFullness;
in float viewDepth;

out vec4 fragColor;

vec3 rgb2hsv(vec3 c) {
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = c.g < c.b ? vec4(c.bg, K.wz) : vec4(c.gb, K.xy);
    vec4 q = c.r < p.x ? vec4(p.xyw, c.r) : vec4(c.r, p.yzx);

    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

float linearizeDepth(float s) {
    // Same calculation mojang does, to linearize depths using the projection matrix values
    return -ProjMat[3].z / (s * -2.0 + 1.0 - ProjMat[2].z);
}

vec3 getGhostColorHsv(vec3 hsv, bool forwards) {
    hsv += forwards ? vec3(-0.45, 0.0, 0.0) : vec3(0.25, 0.0, 0.0);
    hsv.x = mod(hsv.x + 1.0, 1.0);
    hsv.z = pow(hsv.z, 0.8);
    return hsv;
}

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor;
    vec3 hsv = rgb2hsv(color.rgb);
    float layerIndex = LayerParams.x;
    float onSublevel = LayerParams.y;
    float ghostAlpha = ghostLayerFullness * 0.3;

#ifndef GHOSTS
    //keep slight noisy shimmer for stationary blocks, otherwise use similar scaling as the noisy vertex offsets
    vec3 drag = onSublevel > 0.0 ? getDragState(getVelocity(localPosition)) : vec3(1.0, 0.0, 0.0);
    float t = levitite_time() / 500.0;
    vec3 localNoise = getNoiseFromPositionAndTime(vec4(localPosition / 10.0, t), vec3(1.0, 0.0, 0.0) * vec3(0.7, 1.0, 1.0));
    vec3 p = getGlobalPosition(localPosition);
    vec3 n = getNoiseFromPositionAndTime(vec4(p / 10.0, t), drag.yxz * vec3(0.7, 1.0, 1.0));
    float gravityScale = 1.0 - exp(-levitite_gravity_strength() / 3.0);
    n = mix(localNoise, n, gravityScale);
    float ghostNoiseMagnitude = abs(n.x + n.y + n.z) * max(0.3, ghostLayerFullness);

    float s = exp(-ghostNoiseMagnitude);
    hsv.z = pow(hsv.z, s);
    hsv.y = pow(hsv.y, s);
    color.rgb = hsv2rgb(hsv);

    if (onSublevel > 0.0) {
        vec3 ghostTotal = hsv2rgb(getGhostColorHsv(hsv, false)) + hsv2rgb(getGhostColorHsv(hsv, true));
        color.rgb -= ghostTotal * ghostAlpha * 0.2;
    }
#else
    hsv = getGhostColorHsv(hsv, layerIndex < 0.0);
    color.rgb = hsv2rgb(hsv);

    float depthSample = texture(DepthSampler, gl_FragCoord.xy / ScreenSize).r;
    float depth2 = linearizeDepth(depthSample);

    float fade = smoothstep(-2.0, 4.0, (depth2 - viewDepth) * 16.0);
    if (fade < 0.001) {
        discard;
    }

    color.a *= ghostAlpha * fade;
#endif

    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
