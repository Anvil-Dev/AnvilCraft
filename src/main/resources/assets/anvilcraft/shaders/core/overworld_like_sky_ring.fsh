#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:fog.glsl>

uniform sampler2D Sampler0;

in vec4 vertexColor;
in vec2 texCoord0;
in vec3 skyPosition;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
#ifdef ALPHA_CUTOUT
    if (color.a < ALPHA_CUTOUT) {
        discard;
    }
#endif
    // Sky haze follows elevation, so terrain render distance cannot hide the orbital rings.
    float elevation = max(normalize(skyPosition).y, 0.0);
    float haze = 1.0 - 0.65 * elevation;
    fragColor = vec4(mix(color.rgb, FogColor.rgb, haze * FogColor.a), color.a);
}
