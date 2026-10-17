#version 330

// Port of the Veil post pipeline aeronautics:soft_light: soft-light blends the hot air overlay onto the scene where the
// overlay is not behind the scene.
uniform sampler2D MainSampler;
uniform sampler2D MainDepthSampler;
uniform sampler2D OverlaySampler;
uniform sampler2D OverlayDepthSampler;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 baseColor = texture(MainSampler, texCoord);

    if (texture(OverlayDepthSampler, texCoord).r > texture(MainDepthSampler, texCoord).r) {
        fragColor = baseColor;
        return;
    }

    vec3 base = baseColor.rgb;
    vec4 blendColor = texture(OverlaySampler, texCoord);
    vec3 blend = blendColor.rgb;

    // from https://github.com/mattdesl/glsl-blend-soft-light/blob/master/index.glsl
    vec4 softLit = mix(vec4(mix(
        sqrt(base) * (2.0 * blend - 1.0) + 2.0 * base * (1.0 - blend),
        2.0 * base * blend + base * base * (1.0 - 2.0 * blend),
        step(base, vec3(0.5))
    ), baseColor.a), baseColor, 1.0 - blendColor.a);
    fragColor = softLit;
}
