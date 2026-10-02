layout(std140) uniform OverworldSky {
    mat4 InverseProjection;
    mat4 InverseView;
    mat4 MoonRotation;
    vec4 MoonCenterAndVisibility;
    vec4 SunDirectionPadding;
    vec4 SunUvBounds;
};
#define MoonCenter MoonCenterAndVisibility.xyz
#define Visibility MoonCenterAndVisibility.w
#define SunDirection SunDirectionPadding.xyz
