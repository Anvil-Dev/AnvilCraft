layout(std140) uniform MunSurface {
    vec4 SolarReferenceData;
    vec4 SolarOriginData;
    vec4 SolarSettings;
    vec4 SolarHorizons[8];
    vec4 SolarOcclusion[12];
    vec4 MoonCamera;
};
#define SolarReference SolarReferenceData.xyz
#define SolarOrigin SolarOriginData.xyz
#define SolarEclipseCount int(SolarSettings.x)
#define SolarEclipseCoverage SolarSettings.y
#define AmbientFloor SolarSettings.z
#define CameraPosition MoonCamera.xyz
#define SolarHorizon0 SolarHorizons[0].xy
#define SolarHorizon1 SolarHorizons[1].xy
#define SolarHorizon2 SolarHorizons[2].xy
#define SolarHorizon3 SolarHorizons[3].xy
#define SolarHorizon4 SolarHorizons[4].xy
#define SolarHorizon5 SolarHorizons[5].xy
#define SolarHorizon6 SolarHorizons[6].xy
#define SolarHorizon7 SolarHorizons[7].xy
#define SolarEclipse0 SolarOcclusion[0].xy
#define SolarEclipse1 SolarOcclusion[1].xy
#define SolarEclipse2 SolarOcclusion[2].xy
#define SolarEclipse3 SolarOcclusion[3].xy
#define SolarEclipse4 SolarOcclusion[4].xy
#define SolarEclipse5 SolarOcclusion[5].xy
#define SolarEclipse6 SolarOcclusion[6].xy
#define SolarEclipse7 SolarOcclusion[7].xy
#define SolarEclipse8 SolarOcclusion[8].xy
#define SolarEclipse9 SolarOcclusion[9].xy
#define SolarEclipse10 SolarOcclusion[10].xy
#define SolarEclipse11 SolarOcclusion[11].xy
