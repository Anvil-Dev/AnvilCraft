#version 150

uniform sampler2D Sampler0;
uniform vec2 TexelSize;
uniform float GlareStrength;
in vec2 texCoord;
out vec4 fragColor;

vec3 highlight(vec2 uv) {
    vec4 color = texture(Sampler0, uv);
    float brightness = max(max(color.r, color.g), color.b);
    return color.rgb * color.a * smoothstep(0.45, 0.9, brightness);
}

void main() {
    vec3 base = texture(Sampler0, texCoord).rgb;
    if (GlareStrength <= 0.0) {
        fragColor = vec4(base, 1.0);
        return;
    }
    vec3 glow = highlight(texCoord) * 0.2;
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            if (x != 0 || y != 0) glow += highlight(texCoord + vec2(x, y) * TexelSize * 2.5) * 0.1;
        }
    }
    fragColor = vec4(base + glow * GlareStrength, 1.0);
}
