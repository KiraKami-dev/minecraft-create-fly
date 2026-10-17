#version 330

// Levitite effect parameters, per sub-level and layer (upstream: aeronautics:levitite_utils + plain uniforms).
// All positions are "local": plot position relative to the sub-level rotation point, in the sub-level frame.
layout(std140) uniform LevititeInfo {
    // Sub-level orientation (local -> world rotation)
    mat4 Orientation;
    mat4 MaterialMatrixSlow;
    mat4 MaterialMatrixFast;
    // xyz: anchor block - rotation point (anchor-relative plot position -> local position)
    vec4 AnchorOffset;
    // xyz: linear velocity (local frame, blocks per second), w: gravity strength
    vec4 LinearVelocity;
    // xyz: angular velocity (local frame, radians per second), w: material transition speed
    vec4 AngularVelocity;
    // xyz: world position of the rotation point (mod 10000), w: time in ticks
    vec4 SubLevelPosition;
    // x: layer index (0: block, 1/-1: ghost layers), y: on a sub-level (0 or 1)
    vec4 LayerParams;
    // Block atlas rectangles (minU, minV, maxU, maxV) of the levitite sprites
    vec4 SpriteRects[4];
};

uniform sampler2D Noise;

float levitite_transition_speed() {
    return AngularVelocity.w;
}

float levitite_gravity_strength() {
    return LinearVelocity.w;
}

float levitite_time() {
    return SubLevelPosition.w;
}

bool levitite_is_levitite_uv(vec2 uv) {
    for (int i = 0; i < 4; i++) {
        vec4 rect = SpriteRects[i];
        if (uv.x >= rect.x && uv.y >= rect.y && uv.x <= rect.z && uv.y <= rect.w) {
            return true;
        }
    }
    return false;
}

vec3 getVelocity(vec3 pos) {
    return LinearVelocity.xyz - cross(pos, AngularVelocity.xyz);
}

vec3 getGlobalPosition(vec3 localPos) {
    return mat3(Orientation) * localPos + SubLevelPosition.xyz;
}

vec3 getDragState(vec3 vel) {
    float s = levitite_transition_speed();
    float x2 = dot(vel, vel) / (s * s);
    float Q = exp(-1.5 * x2);
    float Q2 = -0.446260320297; // = -2*exp(-1.5)

    float h0 = dot(vel, mat3(MaterialMatrixFast) * vel);
    float h1 = dot(vel, mat3(MaterialMatrixSlow) * vel);
    float h10 = h1 - h0;

    float peak = Q2 * h10 + h0;

    float D = peak > 0.0 ? ((1.0 - 3.0 * x2) * Q * h10 + h0) / peak : 0.0; //derivative of drag function x*(exp(-1.5*x^2/s^2)*h10+h0) normalized to unit magnitude

    D = max(D, 0.0);

    return vec3(Q, 1.0 - Q, D);
}

vec3 getNoiseFromPositionAndTime(vec4 p, vec3 noiseScale) {
    //matrices divisible by 10 to allow position to tile using sufficiently high power of ten wrapping distance
    mat2 R1 = mat2(25, 3, 2, 15) / 10.0;
    mat2 R2 = mat2(15, 1, 2, -25) / 10.0;

    mat3 M1 = mat3(
        10, 2, -1,
        4, 10, -5,
        -3, -2, 10
    ) / 10.0;

    //linearly transform position input to avoid artificial-looking repetition
    mat3 M2 = transpose(M1);
    vec4 p1 = vec4(M1 * p.xyz, p.w);
    vec4 p2 = vec4(M2 * p.xyz, p.w);

    //each coordinate gets a positive and negative noise slice on the two other coordinates, offset by time
    //noise textures from https://opengameart.org/content/700-noise-textures
    vec3 noiseOutX = texture(Noise, R1 * p1.zw).xyz - texture(Noise, R2 * p2.yw).xyz;
    vec3 noiseOutY = texture(Noise, R1 * p1.xw).xyz - texture(Noise, R2 * p2.zw).xyz;
    vec3 noiseOutZ = texture(Noise, R1 * p1.yw).xyz - texture(Noise, R2 * p2.xw).xyz;
    mat3 mat = mat3(noiseOutX, noiseOutY, noiseOutZ);
    return transpose(mat) * noiseScale;
}
