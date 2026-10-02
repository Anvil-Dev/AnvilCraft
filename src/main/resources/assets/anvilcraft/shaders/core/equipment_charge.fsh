#version 330

uniform sampler2D Sampler0;
#moj_import <minecraft:dynamictransforms.glsl>

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 texel = texture(Sampler0, texCoord0);
    float brightness = max(max(texel.r, texel.g), texel.b);
    fragColor = vec4(vec3(brightness), texel.a) * vertexColor * ColorModulator;
    if (fragColor.a < 0.1) discard;
}
