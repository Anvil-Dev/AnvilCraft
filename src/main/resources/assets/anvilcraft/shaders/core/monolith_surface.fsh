#version 330

#moj_import <minecraft:fog.glsl>

uniform sampler2D Sampler0;

in vec4 texProj0;
flat in vec4 spriteBounds;
in float sphericalVertexDistance;
in float cylindricalVertexDistance;

out vec4 fragColor;

void main() {
    vec2 projectedUv = fract(texProj0.xy / texProj0.w * 32.0);
    vec2 texel = 1.0 / vec2(textureSize(Sampler0, 0));
    vec2 uv = spriteBounds.xy + clamp(projectedUv * spriteBounds.zw, texel * 0.5, spriteBounds.zw - texel * 0.5);
    vec4 color = textureLod(Sampler0, uv, 0.0);
    if (color.a < 0.1) discard;
    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance,
        FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
