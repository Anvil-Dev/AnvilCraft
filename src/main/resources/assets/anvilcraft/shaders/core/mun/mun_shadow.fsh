#version 150
uniform sampler2D Sampler0;
in vec2 texCoord;
in vec2 opacity;
void main() {
    if (opacity.x < 0.5) {
        ivec2 size = textureSize(Sampler0, 0);
        ivec2 pixel = clamp(ivec2(texCoord * vec2(size)), ivec2(0), size - 1);
        if (texelFetch(Sampler0, pixel, 0).a * opacity.y < 0.5) discard;
    }
}
