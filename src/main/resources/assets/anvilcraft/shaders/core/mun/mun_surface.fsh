#version 330
#ifdef MUN_SHADOW_HISTORY
#extension GL_ARB_shader_image_load_store : require
#endif
#moj_import <minecraft:fog.glsl>
#moj_import <anvilcraft:mun/mun_surface_uniforms.glsl>
#moj_import <anvilcraft:mun/mun_surface.glsl>
#ifdef MUN_TERRAIN
#moj_import <minecraft:globals.glsl>
#moj_import <minecraft:chunksection.glsl>
#define ColorModulator vec4(1.0)
#else
#moj_import <minecraft:dynamictransforms.glsl>
#endif
uniform sampler2D Sampler0;
#ifdef DISSOLVE
uniform sampler2D DissolveMaskSampler;
#endif
in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec4 vertexColor;
in vec4 overlayColor;
in vec3 blockLight;
in float skyAccess;
in vec3 worldPosition;
in vec2 texCoord0;
out vec4 fragColor;
#ifdef MUN_TERRAIN
vec4 sampleNearest(sampler2D source, vec2 uv, vec2 pixelSize, vec2 du, vec2 dv, vec2 texelScreenSize) {
    // Convert our UV back up to texel coordinates and find out how far over we are from the center of each pixel
    vec2 uvTexelCoords = uv / pixelSize;
    vec2 texelCenter = round(uvTexelCoords) - 0.5f;
    vec2 texelOffset = uvTexelCoords - texelCenter;

    // Move our offset closer to the texel center based on texel size on screen
    texelOffset = (texelOffset - 0.5f) * pixelSize / texelScreenSize + 0.5f;
    texelOffset = clamp(texelOffset, 0.0f, 1.0f);

    uv = (texelCenter + texelOffset) * pixelSize;
    return textureGrad(source, uv, du, dv);
}

vec4 sampleNearest(sampler2D source, vec2 uv, vec2 pixelSize) {
    vec2 du = dFdx(uv);
    vec2 dv = dFdy(uv);
    vec2 texelScreenSize = sqrt(du * du + dv * dv);
    return sampleNearest(source, uv, pixelSize, du, dv, texelScreenSize);
}

// Rotated Grid Super-Sampling
vec4 sampleRGSS(sampler2D source, vec2 uv, vec2 pixelSize) {
    vec2 du = dFdx(uv);
    vec2 dv = dFdy(uv);

    vec2 texelScreenSize = sqrt(du * du + dv * dv);
    float maxTexelSize = max(texelScreenSize.x, texelScreenSize.y);

    float minPixelSize = min(pixelSize.x, pixelSize.y);

    float transitionStart = minPixelSize * 1.0;
    float transitionEnd = minPixelSize * 2.0;
    float blendFactor = smoothstep(transitionStart, transitionEnd, maxTexelSize);

    float duLength = length(du);
    float dvLength = length(dv);
    float minDerivative = min(duLength, dvLength);
    float maxDerivative = max(duLength, dvLength);

    float effectiveDerivative = sqrt(minDerivative * maxDerivative);

    float mipLevelExact = max(0.0, log2(effectiveDerivative / minPixelSize));

    float mipLevelLow = floor(mipLevelExact);
    float mipLevelHigh = mipLevelLow + 1.0;
    float mipBlend = fract(mipLevelExact);

    const vec2 offsets[4] = vec2[](
    vec2(0.125, 0.375),
    vec2(-0.125, -0.375),
    vec2(0.375, -0.125),
    vec2(-0.375, 0.125)
    );

    vec4 rgssColorLow = vec4(0.0);
    vec4 rgssColorHigh = vec4(0.0);
    for (int i = 0; i < 4; ++i) {
        vec2 sampleUV = uv + offsets[i] * pixelSize;
        rgssColorLow += textureLod(source, sampleUV, mipLevelLow);
        rgssColorHigh += textureLod(source, sampleUV, mipLevelHigh);
    }
    rgssColorLow *= 0.25;
    rgssColorHigh *= 0.25;

    vec4 rgssColor = mix(rgssColorLow, rgssColorHigh, mipBlend);

    vec4 nearestColor = sampleNearest(source, uv, pixelSize, du, dv, texelScreenSize);

    return mix(nearestColor, rgssColor, blendFactor);
}

#endif
void main() {
    vec3 geometricNormal = cross(dFdx(worldPosition), dFdy(worldPosition));
    vec3 normal = normalize(dot(geometricNormal, geometricNormal) > 1e-12 ? geometricNormal : vec3(0.0, 1.0, 0.0));
    if (!gl_FrontFacing) normal = -normal;
#ifdef MUN_TERRAIN
    vec4 texel = UseRgss == 1 ? sampleRGSS(Sampler0, texCoord0, 1.0 / TextureSize)
        : sampleNearest(Sampler0, texCoord0, 1.0 / TextureSize);
#else
    vec4 texel = texture(Sampler0, texCoord0);
#endif
#ifdef ALPHA_CUTOUT
    if (texel.a < ALPHA_CUTOUT) discard;
#endif
    vec4 light = surfaceLight(worldPosition, normal, skyAccess);
    vec3 albedo = texel.rgb * vertexColor.rgb * ColorModulator.rgb;
    albedo = mix(overlayColor.rgb, albedo, overlayColor.a);
    float vertexAlpha = vertexColor.a;
#ifdef DISSOLVE
    if (vertexAlpha < texture(DissolveMaskSampler, texCoord0).a) discard;
    vertexAlpha = 1.0;
#endif
    float alpha = texel.a * vertexAlpha * ColorModulator.a;
    vec4 color = vec4(surfaceColor(albedo, blockLight, light.rgb, normal, skyAccess, light.a), alpha);
#ifdef MUN_TERRAIN
    color = mix(FogColor * vec4(1, 1, 1, color.a), color, ChunkVisibility);
#endif
    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance,
        FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
