#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

vec2 hash(vec2 p) {
    return 2.0 * fract(sin(p * mat2(127.1, 311.7, 269.5, 183.3)) * 43758.5453123) - 1.0;
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);

    float a = dot(hash(i + vec2(0.0, 0.0)), f - vec2(0.0, 0.0));
    float b = dot(hash(i + vec2(1.0, 0.0)), f - vec2(1.0, 0.0));
    float c = dot(hash(i + vec2(0.0, 1.0)), f - vec2(0.0, 1.0));
    float d = dot(hash(i + vec2(1.0, 1.0)), f - vec2(1.0, 1.0));

    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float perlin(vec2 p) {
    float v = 0.0;
    float s = 1.0;
    for (int i = 0; i < 6; i++) {
        v += s * noise(p);
        s *= 0.5;
        p *= 2.0;
    }
    return v;
}

void main() {
    float along = texCoord0.x;
    float perp = texCoord0.y * 2.0 - 1.0;

    float time = GameTime * 240000.0;
    float seed = floor(time * 0.2);

    float displacement = perlin(vec2(along * 3.0 + seed * 1.3, seed * 0.7)) * 0.8;
    displacement += perlin(vec2(along * 14.0 + seed * 2.7, seed * 1.1)) * 0.12;

    float taper = smoothstep(0.0, 0.06, along) * smoothstep(1.0, 0.943, along);
    displacement *= taper;

    float dist = abs(perp - displacement);

    float core = 0.006 / (dist + 0.002);
    float innerGlow = 0.03 / (dist * dist + 0.015);
    float outerGlow = 0.06 / (dist * dist + 0.12);

    float intensity = (core + innerGlow * 0.5 + outerGlow * 0.15) * taper;

    float branchSeed = floor(seed * 0.5);
    float branchPos = fract(sin(branchSeed * 127.1) * 43758.5453);
    float branchStart = branchPos;
    float branchEnd = branchPos + 0.25;
    float branchRange = smoothstep(branchStart - 0.02, branchStart + 0.02, along)
                      * smoothstep(branchEnd + 0.02, branchEnd - 0.02, along);
    if (branchRange > 0.0) {
        float branchAlong = (along - branchStart) / 0.25;
        float branchDisp = displacement
            + perlin(vec2(branchAlong * 10.0 + branchSeed, branchSeed * 0.9)) * 0.35 * branchAlong;
        float branchDist = abs(perp - branchDisp);
        float branchIntensity = 0.003 / (branchDist + 0.004) * branchRange;
        intensity += branchIntensity * taper;
    }

    intensity = clamp(intensity, 0.0, 4.0);

    vec4 color = vertexColor * ColorModulator;
    color.rgb *= intensity;
    color.a = clamp(intensity * 0.4, 0.0, 1.0) * color.a;

    if (color.a < 0.01) discard;

    fragColor = color;
}