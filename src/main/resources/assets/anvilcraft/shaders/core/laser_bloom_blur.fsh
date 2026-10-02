#version 330

uniform sampler2D DiffuseSampler;
layout(std140) uniform BlurDirection {
    vec2 BlurDir;
};

in vec2 texCoord;
out vec4 fragColor;

const float weight[7] = float[](0.227027, 0.1945946, 0.1516216, 0.124054, 0.016216, 0.0111, 0.0100);

void main() {
    vec2 texOffset = 1.0 / textureSize(DiffuseSampler, 0);
    vec3 result = texture(DiffuseSampler, texCoord).rgb * weight[0];
    for (int i = 1; i < 7; ++i) {
        vec2 offset = BlurDir * texOffset * i * 1.943;
        result += texture(DiffuseSampler, texCoord + offset).rgb * weight[i];
        result += texture(DiffuseSampler, texCoord - offset).rgb * weight[i];
    }
    fragColor = vec4(result * 1.105, 1.0);
}
