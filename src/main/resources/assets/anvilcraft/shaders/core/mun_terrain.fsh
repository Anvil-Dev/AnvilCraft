#version 150
#ifdef GL_ARB_shader_image_load_store
#extension GL_ARB_shader_image_load_store : enable
#define MUN_SHADOW_HISTORY
#endif

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
    vec3 geometricNormal = cross(dFdx(worldPosition), dFdy(worldPosition));
    vec3 normal = normalize(dot(geometricNormal, geometricNormal) > 1e-12 ? geometricNormal : surfaceNormal);
    if (!gl_FrontFacing) normal = -normal;
    vec4 texel = texture(Sampler0, texCoord0);
    if (texel.a < AlphaCutoff) discard;
    vec4 light = surfaceLight(worldPosition, normal, skyAccess);
    vec3 albedo = texel.rgb * vertexColor.rgb * ColorModulator.rgb;
    float alpha = texel.a * vertexColor.a * ColorModulator.a;
    vec4 color = vec4(surfaceColor(albedo, blockLight, light.rgb, normal, skyAccess, light.a), alpha);
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
