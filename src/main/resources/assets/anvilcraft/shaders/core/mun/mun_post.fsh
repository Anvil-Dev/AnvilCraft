#version 150
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform mat4 InverseProjection;
uniform vec2 ProjectionScale;
uniform vec2 SceneTexelSize;
uniform vec2 TexelSize;
uniform vec2 BlurDirection;
uniform int Mode;
uniform int SampleCount;
uniform float Radius;
uniform float AoStrength;
uniform float GlareStrength;
uniform float Daylight;
in vec2 texCoord;
out vec4 fragColor;

const vec2 AO_OFFSETS[24] = vec2[24](
    vec2(0.70710678, 0.00000000),
    vec2(-0.90308875, 0.82730327),
    vec2(0.13823221, -1.57508471),
    vec2(1.13828488, 1.48469106),
    vec2(-2.08889275, -0.36949572),
    vec2(1.97878157, -1.25873886),
    vec2(-0.66186371, 2.46210000),
    vec2(-1.26224587, -2.43037762),
    vec2(2.73856864, 1.00012088),
    vec2(-2.84902435, 1.17603583),
    vec2(1.37341800, -2.93491448),
    vec2(1.01492095, 3.23572796),
    vec2(-3.05898356, -1.77274351),
    vec2(3.58853594, -0.78892955),
    vec2(-2.19002763, 3.11508892),
    vec2(-0.50594708, -3.90435879),
    vec2(3.10601889, 2.61775603),
    vec2(-4.17972782, 0.17284485),
    vec2(3.04879061, -3.03395383),
    vec2(-0.20397593, 4.41116695),
    vec2(-2.90093402, -3.47628851),
    vec2(4.59539981, 0.61830457),
    vec2(-3.89367299, 2.70911621),
    vec2(1.06397544, -4.72947738)
);

vec3 viewPosition(vec2 uv, float depth) {
    vec4 point = InverseProjection * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    return point.xyz / max(point.w, 1e-6);
}

vec3 viewPosition(vec2 uv) {
    return viewPosition(uv, texture(Sampler1, uv).r);
}

float ambientVisibility(vec3 center, float depth) {
    if (SampleCount == 0 || depth >= 0.999999) return 1.0;
    vec3 right = viewPosition(texCoord + vec2(SceneTexelSize.x, 0)) - center;
    vec3 left = center - viewPosition(texCoord - vec2(SceneTexelSize.x, 0));
    vec3 up = viewPosition(texCoord + vec2(0, SceneTexelSize.y)) - center;
    vec3 down = center - viewPosition(texCoord - vec2(0, SceneTexelSize.y));
    vec3 normal = normalize(cross(abs(right.z) < abs(left.z) ? right : left, abs(up.z) < abs(down.z) ? up : down));
    float occlusion = 0.0;
    vec2 radius = min(vec2(128.0) * SceneTexelSize, Radius * ProjectionScale / max(-center.z * 2.0, 0.1));
    radius *= inversesqrt(float(SampleCount));
    for (int index = 0; index < 24; index++) {
        if (index >= SampleCount) break;
        vec2 uv = texCoord + AO_OFFSETS[index] * radius;
        if (any(lessThan(uv, vec2(0))) || any(greaterThan(uv, vec2(1)))) continue;
        float sampleDepth = texture(Sampler1, uv).r;
        if (sampleDepth >= 0.999999) continue;
        vec3 delta = viewPosition(uv, sampleDepth) - center;
        float distance = length(delta);
        if (distance > 0.015 && distance < Radius) {
            occlusion += max(dot(normal, delta / distance) - 0.08, 0.0) * (1.0 - smoothstep(Radius * 0.25, Radius, distance));
        }
    }
    return 1.0 - AoStrength * occlusion / float(SampleCount);
}

void main() {
    if (Mode == 2) {
        vec4 effects = texture(Sampler0, texCoord);
        fragColor = vec4(effects.rgb * GlareStrength, effects.a);
        return;
    }
    if (Mode == 1) {
        vec4 sum = vec4(0);
        float aoWeight = 0.0;
        float centerDepth = SampleCount > 0 ? viewPosition(texCoord).z : 0.0;
        for (int index = -2; index <= 2; index++) {
            vec2 uv = texCoord + BlurDirection * TexelSize * float(index);
            float weight = index == 0 ? 0.375 : (abs(index) == 1 ? 0.25 : 0.0625);
            vec4 sampleValue = texture(Sampler0, uv);
            sum.rgb += sampleValue.rgb * weight;
            float edge = SampleCount > 0 ? exp(-abs(viewPosition(uv).z - centerDepth) / max(0.08, abs(centerDepth) * 0.005)) : 1.0;
            sum.a += sampleValue.a * weight * edge;
            aoWeight += weight * edge;
        }
        fragColor = vec4(sum.rgb, sum.a / max(aoWeight, 1e-5));
        return;
    }
    vec3 color = texture(Sampler0, texCoord).rgb;
    float brightness = max(max(color.r, color.g), color.b);
    float depth = texture(Sampler1, texCoord).r;
    float highlight = depth < 0.999999 ? smoothstep(0.55, 0.95, brightness) * mix(0.2, 1.0, Daylight) : 0.0;
    float ao = ambientVisibility(viewPosition(texCoord, depth), depth);
    ao = mix(ao, 1.0, smoothstep(0.65, 0.95, brightness) * 0.8);
    fragColor = vec4(color * highlight, ao);
}
