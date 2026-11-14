#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform vec4 FogColor;

in vec4 vertexColor;
in vec2 texCoord0;
in vec3 skyPosition;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    if (color.a < 0.1) {
        discard;
    }
    // 按仰角混合天空雾色，避免地形视距截断束星环。
    float elevation = max(normalize(skyPosition).y, 0.0);
    float haze = 1.0 - 0.65 * elevation;
    fragColor = vec4(mix(color.rgb, FogColor.rgb, haze * FogColor.a), color.a);
}
