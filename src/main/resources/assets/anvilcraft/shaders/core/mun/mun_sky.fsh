#version 150
#moj_import <anvilcraft:mun/mun_sun_render.glsl>

uniform sampler2D Sampler0;
uniform mat4 SkyRotation;
uniform mat4 EarthRotation;
uniform vec3 EarthCenter;
uniform vec3 SunDirection;
uniform float EarthHalfSize;
uniform float EarthPerspective;
uniform float AtmosphereThickness;
uniform float Daylight;
in vec3 localRay;
out vec4 fragColor;

bool earth(vec3 origin, vec3 direction, out vec3 color, out float surfaceDistance) {
    surfaceDistance = 10000.0;
    vec2 interval = cubeInterval(origin, direction, EarthHalfSize);
    if (interval.y < max(interval.x, 0.0)) return false;
    surfaceDistance = interval.x;
    vec3 point = origin + surfaceDistance * direction;
    vec3 p = clamp(point / (2.0 * EarthHalfSize) + 0.5, 0.0, 1.0);
    vec3 face = abs(point);
    vec3 normal;
    vec2 uv;
    vec2 tile;
    if (face.x >= face.y && face.x >= face.z) {
        normal = vec3(sign(point.x), 0.0, 0.0);
        tile = point.x > 0.0 ? vec2(0.0, 1.0) : vec2(2.0, 1.0);
        uv = vec2(point.x > 0.0 ? 1.0 - p.z : p.z, 1.0 - p.y);
    } else if (face.y >= face.z) {
        normal = vec3(0.0, sign(point.y), 0.0);
        tile = point.y > 0.0 ? vec2(1.0, 0.0) : vec2(1.0, 2.0);
        uv = vec2(p.x, point.y > 0.0 ? p.z : 1.0 - p.z);
    } else {
        normal = vec3(0.0, 0.0, sign(point.z));
        tile = point.z > 0.0 ? vec2(3.0, 1.0) : vec2(1.0, 1.0);
        uv = vec2(point.z > 0.0 ? p.x : 1.0 - p.x, 1.0 - p.y);
    }
    float light = max(dot(mat3(EarthRotation) * normal, SunDirection), 0.0);
    vec3 albedo = texture(Sampler0, (tile + clamp(uv, 0.001, 0.999)) / 4.0).rgb;
    color = pow(albedo, vec3(0.86)) * (0.1 + 0.9 * sqrt(light));
    return true;
}

vec4 atmosphere(vec3 origin, vec3 direction, float surfaceDistance) {
    if (AtmosphereThickness <= 0.0) return vec4(0.0);
    vec2 interval = cubeInterval(origin, direction, EarthHalfSize + AtmosphereThickness);
    float entry = max(interval.x, 0.0);
    float exit = min(interval.y, surfaceDistance);
    if (exit <= entry) return vec4(0.0);
    float stepLength = (exit - entry) / 12.0;
    float scaleHeight = AtmosphereThickness * 0.36;
    float column = 0.0;
    float scattering = 0.0;
    for (int i = 0; i < 12; i++) {
        vec3 point = origin + direction * (entry + (float(i) + 0.5) * stepLength);
        float altitude = max(max(abs(point.x), abs(point.y)), abs(point.z)) - EarthHalfSize;
        float density = exp(-max(altitude, 0.0) / scaleHeight)
            * (1.0 - smoothstep(AtmosphereThickness * 0.65, AtmosphereThickness, altitude));
        vec3 normal = mat3(EarthRotation) * normalize(point);
        float light = 0.38 + 0.62 * max(dot(normal, SunDirection), 0.0);
        column += density * stepLength;
        scattering += density * stepLength * light;
    }
    float opacity = 1.0 - exp(-column * 0.12 / scaleHeight);
    vec3 haze = vec3(0.20, 0.50, 0.95) * scattering / max(column, 1e-8);
    return vec4(haze, opacity);
}

float stars(vec3 ray) {
    vec3 p = ray * 280.0;
    vec3 cell = floor(p);
    float seed = fract(sin(dot(cell, vec3(127.1, 311.7, 74.7))) * 43758.5453);
    float point = 1.0 - smoothstep(0.12, 0.36, length(fract(p) - 0.5));
    return seed > 0.996 ? point * (0.45 + 0.55 * seed) : 0.0;
}

void main() {
    vec3 localDirection = normalize(localRay);
    fragColor = vec4(0.0);
    if (localDirection.y <= 0.0) return;
    vec3 ray = transpose(mat3(SkyRotation)) * localDirection;
    mat3 inverseEarth = transpose(mat3(EarthRotation));
    // 逆变换弱透视的深度压缩；归一化后仍按地球本地距离积分大气散射。
    vec3 origin = inverseEarth * (-EarthCenter / EarthPerspective);
    vec3 earthRay = ray + EarthCenter * dot(ray, EarthCenter) * (1.0 / EarthPerspective - 1.0);
    vec3 direction = normalize(inverseEarth * earthRay);
    vec3 color;
    float surfaceDistance;
    if (!earth(origin, direction, color, surfaceDistance)) {
        vec3 tangent = vec3(SunDirection.y, -SunDirection.x, 0.0);
        float forward = dot(ray, SunDirection);
        vec3 starRay = vec3(dot(ray, tangent), forward, ray.z);
        color = vec3(stars(starRay) * mix(0.85, 0.3, Daylight));
        if (forward > 0.0 && max(abs(starRay.x), abs(starRay.z)) < forward * 0.4) {
            vec4 solar = modelSun(starRay, Daylight);
            color = solar.rgb + color * (1.0 - solar.a);
        }
    }
    vec4 haze = atmosphere(origin, direction, surfaceDistance);
    color = mix(color, haze.rgb, haze.a);
    fragColor = vec4(color, 0.0);
}
