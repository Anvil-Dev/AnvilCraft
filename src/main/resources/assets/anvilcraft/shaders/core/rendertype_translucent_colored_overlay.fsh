#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

//TODO THIS IS A BAD IMPLEMENTATION
#ifdef OVERLAY_COLOR
const vec4 color = vec4(OVERLAY_COLOR_R, OVERLAY_COLOR_G, OVERLAY_COLOR_B, OVERLAY_COLOR_A);
#else
const vec4 color = vec4(1, 1, 1, 1);
#endif

uniform sampler2D Sampler0;

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    #ifdef ALPHA_CUTOUT
    if (color.a < ALPHA_CUTOUT) {
        discard;
    }
    #endif
    color = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
    color = mix(color, OverlayColor);
    fragColor = color;
}
