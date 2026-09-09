#version 150

#moj_import <fog.glsl>
#moj_import <anvilcraft:mun_surface.glsl>

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
uniform float AlphaCutoff;
in float vertexDistance;
in vec4 vertexColor;
in vec3 blockLight;
in float skyAccess;
in vec3 worldPosition;
in vec3 surfaceNormal;
in vec2 texCoord0;
out vec4 fragColor;

void main() {
    vec4 texel = texture(Sampler0, texCoord0);
    if (texel.a < AlphaCutoff) discard;
    vec3 normal = normalize(surfaceNormal);
    float direct = surfaceSunlight(worldPosition, normal, skyAccess);
    vec3 albedo = texel.rgb * vertexColor.rgb * ColorModulator.rgb;
    vec4 color = vec4(surfaceColor(albedo, blockLight, direct), 1.0);
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
    fragColor.a = direct;
}
