#version 330

uniform sampler2D DiffuseSampler;
uniform sampler2D BloomSampler;
in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 colorGame = texture(DiffuseSampler, texCoord);
    vec4 bloom = texture(BloomSampler, texCoord);
    vec3 finalColor = colorGame.rgb + bloom.rgb * pow(0.08, length(colorGame.rgb) * 0.8);
    fragColor = vec4(finalColor, colorGame.a + bloom.a);
}
