#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;
in float vertexDistance;
in float cylindricalVertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
out vec4 fragColor;

void main() {
    vec4 surface = texture(Sampler0, texCoord0);
    if (surface.a < 0.1) discard;
    float exposure = max(0.04, exp(vertexColor.a * log(25.0)) - 1.0);
    vec3 color = surface.rgb * vertexColor.rgb;
    float peak = max(color.r, max(color.g, color.b));
    float surfaceGain = max(vertexColor.r, max(vertexColor.g, vertexColor.b));
    float brightness = sqrt(1.0 - exp(-peak * exposure / max(surfaceGain, 0.00001))) * surfaceGain;
    vec3 mapped = color * (brightness / max(peak, 0.00001));
    fragColor = apply_fog(vec4(mapped, 1.0) * ColorModulator, vertexDistance, cylindricalVertexDistance,
        FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
