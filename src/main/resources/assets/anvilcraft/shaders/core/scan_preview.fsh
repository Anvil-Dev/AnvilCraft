#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform float GameTime;

in vec2 texCoord;
out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);

    // 亮度提取 — 用于蓝色染色混合
    float luminance = dot(color.rgb, vec3(0.299, 0.587, 0.114));

    // 扫描线间距与行号
    float lineSpacing = 4.0;
    float lineY = floor((texCoord.y * InSize.y) / lineSpacing);

    // 基于行号和时间变化的随机值
    float lineHash = hash(vec2(lineY, floor(GameTime * 3.0)));

    // 扫描线亮度变化 (0.7 ~ 1.0)
    float scanBrightness = 0.7 + 0.3 * lineHash;

    // 约30%的扫描线有水平抖动
    float jitterStrength = 2.0;
    float jitter = (lineHash - 0.5) * 2.0 * jitterStrength / InSize.x;
    float shouldJitter = step(0.7, hash(vec2(lineY, 42.0)));

    // 带水平偏移的采样
    vec2 jitteredCoord = texCoord + vec2(jitter * shouldJitter, 0.0);
    vec4 jitteredColor = texture(DiffuseSampler, jitteredCoord);

    // 蓝色染色
    vec3 blueTint = vec3(0.2, 0.45, 1.0);
    float jitteredLum = dot(jitteredColor.rgb, vec3(0.299, 0.587, 0.114));
    vec3 finalColor = mix(jitteredColor.rgb, blueTint * jitteredLum, 0.75);

    // 应用扫描线亮度
    finalColor *= scanBrightness;

    fragColor = vec4(finalColor, 0.85 * color.a);
}
