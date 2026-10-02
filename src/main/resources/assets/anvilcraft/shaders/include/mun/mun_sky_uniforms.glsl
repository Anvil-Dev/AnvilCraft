layout(std140) uniform MunSky {
    mat4 InverseProjection;
    mat4 InverseView;
    mat4 SkyRotation;
    mat4 EarthRotation;
    vec4 EarthCenterAndSize;
    vec4 SunDirectionAndDaylight;
    vec4 EarthParameters;
    vec4 SunUvBounds;
};
#define EarthCenter EarthCenterAndSize.xyz
#define EarthHalfSize EarthCenterAndSize.w
#define SunDirection SunDirectionAndDaylight.xyz
#define Daylight SunDirectionAndDaylight.w
#define EarthPerspective EarthParameters.x
#define AtmosphereThickness EarthParameters.y
