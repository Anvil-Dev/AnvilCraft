// Solar declarations are shared with the shadow caster vertex shader.
#moj_import <anvilcraft:mun_solar.glsl>
uniform sampler2D ShadowMap0;
uniform sampler2D ShadowMap1;
uniform sampler2D ShadowMap2;
uniform sampler2D ShadowStaticMap0;
uniform sampler2D ShadowStaticMap1;
uniform sampler2D ShadowStaticMap2;
uniform usampler2D TranslucentShadowMap0;
uniform usampler2D TranslucentShadowMap1;
uniform usampler2D TranslucentShadowMap2;
uniform int TranslucentShadows;
uniform vec3 ShadowSolarReference;
uniform vec3 ShadowSolarOrigin;
uniform vec2 ShadowSolarHorizon0;
uniform vec2 ShadowSolarHorizon1;
uniform vec2 ShadowSolarHorizon2;
uniform vec2 ShadowSolarHorizon3;
uniform vec2 ShadowSolarHorizon4;
uniform vec2 ShadowSolarHorizon5;
uniform vec2 ShadowSolarHorizon6;
uniform vec2 ShadowSolarHorizon7;
#ifdef MUN_SHADOW_HISTORY
uniform isampler2D ShadowHistory;
uniform int ShadowHistoryFrame;
uniform int ShadowHistoryPrevious;
uniform ivec3 ShadowHistoryOrigin;
layout(rgba32i) uniform writeonly iimage2D ShadowHistoryOutput;
layout(r32ui) uniform uimage2D ShadowHistoryClaims;
#endif
uniform float AmbientFloor;
uniform mat4 ShadowMatrix0;
uniform mat4 ShadowMatrix1;
uniform mat4 ShadowMatrix2;
uniform vec4 ShadowInfo0;
uniform vec4 ShadowInfo1;
uniform vec4 ShadowInfo2;
uniform vec3 ShadowAnchor;
uniform int ShadowCount;
const float SHADOW_BORDER = 0.002;

vec3 shadowSolarDirection(vec3 position) {
    vec2 horizon[8] = vec2[8](ShadowSolarHorizon0, ShadowSolarHorizon1, ShadowSolarHorizon2, ShadowSolarHorizon3,
        ShadowSolarHorizon4, ShadowSolarHorizon5, ShadowSolarHorizon6, ShadowSolarHorizon7);
    vec3 sun;
    vec3 latitude;
    solarFrame(position, ShadowSolarReference, ShadowSolarOrigin, horizon, sun, latitude);
    return sun;
}

vec3 shadowSolarProject(vec3 position, vec3 sun) {
    return position - vec3(sun.x, 0.0, sun.z) * ((position.y + ShadowSolarOrigin.y) / max(sun.y, 0.025));
}

#ifdef MUN_SHADOW_HISTORY
uint shadowHash(uvec3 key, uint normal) {
    uint hash = key.x * 0x8da6b343u ^ key.y * 0xd8163841u ^ key.z * 0xcb1ab31fu ^ normal * 0x165667b1u;
    hash ^= hash >> 16u;
    hash *= 0x7feb352du;
    hash ^= hash >> 15u;
    return hash;
}
#endif

float stableShadowVisibility(vec3 receiver, vec3 normal, float coverage, bool animated) {
    float visible = step(0.5, coverage);
#ifdef MUN_SHADOW_HISTORY
    if (ShadowHistoryFrame == 0) return visible;
    ivec3 key = ivec3((uvec3(ShadowHistoryOrigin) << 12u) + uvec3(ivec3(round(receiver * 4096.0))));
    uvec3 quantizedNormal = uvec3(round(clamp(normal * 0.5 + 0.5, 0.0, 1.0) * 15.0));
    uint face = quantizedNormal.x | quantizedNormal.y << 4u | quantizedNormal.z << 8u;
    uint hash = shadowHash(uvec3(key), face);
    if (!animated && coverage > 0.35 && coverage < 0.65 && ShadowHistoryPrevious >= 0) {
        for (uint probe = 0u; probe < 4u; probe++) {
            uint slot = (hash + probe) & 1048575u;
            ivec4 previous = texelFetch(ShadowHistory, ivec2(slot & 1023u, slot >> 10u), 0);
            uint state = uint(previous.w);
            if (all(equal(previous.xyz, key)) && (state >> 14u) == uint(ShadowHistoryPrevious)
                && ((state >> 2u) & 4095u) == face) {
                if ((state & 2u) == 0u) visible = float(state & 1u);
                break;
            }
        }
    }
    // A single owner writes each record. Readers only access the completed previous-frame texture.
    uint claim = shadowHash(uvec3(key.zxy) ^ uvec3(0xa511e9b3u), face) | 1u;
    for (uint probe = 0u; probe < 4u; probe++) {
        uint slot = (hash + probe) & 1048575u;
        ivec2 pixel = ivec2(slot & 1023u, slot >> 10u);
        uint owner = imageAtomicCompSwap(ShadowHistoryClaims, pixel, 0u, claim);
        if (owner == 0u) {
            uint state = uint(ShadowHistoryFrame) << 14u | face << 2u | (animated ? 2u : 0u) | uint(visible);
            imageStore(ShadowHistoryOutput, pixel, ivec4(key, int(state)));
            break;
        }
        if (owner == claim) break;
    }
#endif
    return visible;
}

float cascadeWeight(mat4 matrix, vec4 info, vec3 projected, float distance, float fadeStart) {
    vec3 point = (matrix * vec4(projected, 1.0)).xyz * 0.5 + 0.5;
    if (any(lessThanEqual(point, vec3(SHADOW_BORDER))) || any(greaterThanEqual(point, vec3(1.0 - SHADOW_BORDER)))) return 0.0;
    float edge = max(abs(point.x * 2.0 - 1.0), abs(point.y * 2.0 - 1.0)) / (1.0 - 2.0 * SHADOW_BORDER);
    return 1.0 - max(smoothstep(fadeStart, 1.0, edge), smoothstep(info.x * fadeStart, info.x, distance));
}

vec2 cascadeVisibility(sampler2D depths, sampler2D staticDepths, mat4 matrix, vec3 projected, vec3 along, vec3 across) {
    vec3 point = (matrix * vec4(projected, 1.0)).xyz * 0.5 + 0.5;
    vec3 first = (matrix * vec4(along, 0.0)).xyz * 0.5;
    vec3 second = (matrix * vec4(across, 0.0)).xyz * 0.5;
    float determinant = first.x * second.y - first.y * second.x;
    vec2 gradient = abs(determinant) > 1e-14
        ? vec2(first.z * second.y - first.y * second.z, first.x * second.z - first.z * second.x) / determinant
        : vec2(0.0);
    ivec2 size = textureSize(depths, 0);
    vec2 location = point.xy * vec2(size) - 0.5;
    ivec2 base = ivec2(floor(location));
    vec2 fraction = fract(location);
    vec2 result = vec2(0.0);
    for (int sampleIndex = 0; sampleIndex < 4; sampleIndex++) {
        ivec2 offset = ivec2(sampleIndex % 2, sampleIndex / 2);
        ivec2 pixel = clamp(base + offset, ivec2(0), size - 1);
        vec2 center = (vec2(pixel) + 0.5) / vec2(size);
        float receiverDepth = point.z + dot(gradient, center - point.xy) - 0.000004;
        float depth = texelFetch(depths, pixel, 0).r;
        float visible = step(receiverDepth, depth);
        vec2 weight = mix(1.0 - fraction, fraction, vec2(offset));
        result.x += visible * weight.x * weight.y;
        if (visible < 0.5 && depth + 0.0000001 < texelFetch(staticDepths, pixel, 0).r) result.y = 1.0;
    }
    return result;
}

vec3 shadowReceiver(vec3 position, vec3 normal, out vec3 along, out vec3 across) {
    vec3 axis = abs(normal);
    int perpendicular = axis.x > axis.y && axis.x > axis.z ? 0 : (axis.y >= axis.z ? 1 : 2);
    // The floating render origin is a whole block, so this grid stays fixed in world space.
    vec3 receiver = (floor(position * 16.0) + 0.5) / 16.0;
    receiver[perpendicular] += dot(position - receiver, normal) / normal[perpendicular];
    along = vec3(0.0);
    across = vec3(0.0);
    along[(perpendicular + 1) % 3] = 0.03125;
    across[(perpendicular + 2) % 3] = 0.03125;
    along[perpendicular] = -dot(along, normal) / normal[perpendicular];
    across[perpendicular] = -dot(across, normal) / normal[perpendicular];
    return receiver;
}

float worldShadowVisibility(vec3 position, vec3 normal, float skyAccess) {
    float fallback = skyAccess > 0.0 ? 1.0 : 0.0;
    if (ShadowCount == 0) return fallback;
    vec3 along;
    vec3 across;
    vec3 receiver = shadowReceiver(position, normal, along, across);
    float distance = length(receiver - ShadowAnchor);
    float groundDistance = length(receiver.xz - ShadowAnchor.xz);
    float radius = ShadowCount > 2 ? ShadowInfo2.x : (ShadowCount > 1 ? ShadowInfo1.x : ShadowInfo0.x);
    if ((ShadowCount > 2 ? groundDistance : distance) >= radius) return fallback;
    vec3 biased = receiver + normal * 0.004;
    vec3 sun = shadowSolarDirection(biased);
    vec2 slope = sun.xz / max(sun.y, 0.025);
    vec3 projected = shadowSolarProject(biased, sun);
    along.xz -= slope * along.y;
    across.xz -= slope * across.y;
    float visibility = 0.0;
    bool animated = false;
    float remaining = 1.0;
    if (distance < ShadowInfo0.x) {
        float weight = cascadeWeight(ShadowMatrix0, ShadowInfo0, projected, distance, 0.8);
        if (weight > 0.0) {
            vec2 sampleValue = cascadeVisibility(ShadowMap0, ShadowStaticMap0, ShadowMatrix0, projected, along, across);
            visibility += weight * sampleValue.x;
            animated = animated || sampleValue.y > 0.0;
        }
        remaining -= weight;
        if (remaining <= 0.0) return stableShadowVisibility(receiver, normal, visibility, animated);
    }
    if (ShadowCount >= 2 && distance < ShadowInfo1.x) {
        float weight = remaining * cascadeWeight(ShadowMatrix1, ShadowInfo1, projected, distance, 0.85);
        if (weight > 0.0) {
            vec2 sampleValue = cascadeVisibility(ShadowMap1, ShadowStaticMap1, ShadowMatrix1, projected, along, across);
            visibility += weight * sampleValue.x;
            animated = animated || sampleValue.y > 0.0;
        }
        remaining -= weight;
        if (remaining <= 0.0) return stableShadowVisibility(receiver, normal, visibility, animated);
    }
    if (ShadowCount > 2 && groundDistance < ShadowInfo2.x) {
        float weight = remaining * cascadeWeight(ShadowMatrix2, ShadowInfo2, projected, groundDistance, 0.95);
        if (weight > 0.0) {
            vec2 sampleValue = cascadeVisibility(ShadowMap2, ShadowStaticMap2, ShadowMatrix2, projected, along, across);
            visibility += weight * sampleValue.x;
            animated = animated || sampleValue.y > 0.0;
        }
        remaining -= weight;
    }
    if (remaining >= 1.0) return fallback;
    return stableShadowVisibility(receiver, normal, visibility + remaining * fallback, animated);
}

vec3 translucentVisibility(usampler2D depths, mat4 matrix, vec3 projected, vec3 along, vec3 across) {
    vec3 point = (matrix * vec4(projected, 1.0)).xyz * 0.5 + 0.5;
    vec3 first = (matrix * vec4(along, 0.0)).xyz * 0.5;
    vec3 second = (matrix * vec4(across, 0.0)).xyz * 0.5;
    float determinant = first.x * second.y - first.y * second.x;
    vec2 gradient = abs(determinant) > 1e-14
        ? vec2(first.z * second.y - first.y * second.z, first.x * second.z - first.z * second.x) / determinant
        : vec2(0.0);
    ivec2 size = textureSize(depths, 0);
    vec2 location = point.xy * vec2(size) - 0.5;
    ivec2 base = ivec2(floor(location));
    vec2 fraction = fract(location);
    vec3 result = vec3(0.0);
    for (int sampleIndex = 0; sampleIndex < 4; sampleIndex++) {
        ivec2 offset = ivec2(sampleIndex % 2, sampleIndex / 2);
        ivec2 pixel = clamp(base + offset, ivec2(0), size - 1);
        vec2 center = (vec2(pixel) + 0.5) / vec2(size);
        float receiverDepth = point.z + dot(gradient, center - point.xy) - 0.000004;
        uvec2 data = texelFetch(depths, pixel, 0).rg;
        vec3 transmission = vec3(data.x & 255u, (data.x >> 8u) & 255u, (data.x >> 16u) & 255u) / 255.0;
        vec2 weight = mix(1.0 - fraction, fraction, vec2(offset));
        result += (receiverDepth <= float(data.y) / 16777215.0 ? vec3(1.0) : transmission) * weight.x * weight.y;
    }
    return result;
}

vec3 worldShadowTransmission(vec3 position, vec3 normal) {
    if (TranslucentShadows == 0 || ShadowCount == 0) return vec3(1.0);
    vec3 along;
    vec3 across;
    vec3 receiver = shadowReceiver(position, normal, along, across);
    float distance = length(receiver - ShadowAnchor);
    float groundDistance = length(receiver.xz - ShadowAnchor.xz);
    float radius = ShadowCount > 2 ? ShadowInfo2.x : ShadowInfo1.x;
    if ((ShadowCount > 2 ? groundDistance : distance) >= radius) return vec3(1.0);
    vec3 biased = receiver + normal * 0.004;
    vec3 sun = shadowSolarDirection(biased);
    vec2 slope = sun.xz / max(sun.y, 0.025);
    vec3 projected = shadowSolarProject(biased, sun);
    along.xz -= slope * along.y;
    across.xz -= slope * across.y;
    float weight = distance < ShadowInfo0.x ? cascadeWeight(ShadowMatrix0, ShadowInfo0, projected, distance, 0.8) : 0.0;
    vec3 transmission = vec3(0.0);
    if (weight > 0.0) transmission += weight * translucentVisibility(TranslucentShadowMap0, ShadowMatrix0, projected, along, across);
    float remaining = 1.0 - weight;
    if (remaining > 0.0 && distance < ShadowInfo1.x) {
        weight = remaining * cascadeWeight(ShadowMatrix1, ShadowInfo1, projected, distance, 0.85);
        if (weight > 0.0) transmission += weight * translucentVisibility(TranslucentShadowMap1, ShadowMatrix1, projected, along, across);
        remaining -= weight;
    }
    if (remaining > 0.0 && ShadowCount > 2) {
        weight = remaining * cascadeWeight(ShadowMatrix2, ShadowInfo2, projected, groundDistance, 0.95);
        if (weight > 0.0) transmission += weight * translucentVisibility(TranslucentShadowMap2, ShadowMatrix2, projected, along, across);
        remaining -= weight;
    }
    return transmission + vec3(remaining);
}

vec4 surfaceLight(vec3 position, vec3 normal, float skyAccess) {
    vec3 sun;
    vec3 latitude;
    solarFrame(position, sun, latitude);
    float daylight = solarDaylight(sun, latitude);
    float direct = max(dot(normal, sun), 0.0) * daylight;
    if (direct > 0.001 && ShadowCount > 0) {
        direct *= worldShadowVisibility(position, normal, skyAccess);
    }
    vec3 sunlight = vec3(direct);
    if (direct > 0.001) sunlight *= worldShadowTransmission(position, normal);
    return vec4(sunlight, daylight);
}

vec3 surfaceColor(vec3 albedo, vec3 blockLight, vec3 direct, vec3 normal, float skyAccess, float daylight) {
    vec3 base = max(blockLight, vec3(AmbientFloor) * smoothstep(0.2, 1.0, skyAccess) * daylight);
    float ambient = mix(0.84, 1.0, max(normal.y, 0.0));
    vec3 indirect = mix(base * ambient, base, smoothstep(vec3(0.65), vec3(1.0), base));
    return pow(clamp(albedo * min(indirect + direct, vec3(1.0)), 0.0, 1.0), vec3(0.82));
}
