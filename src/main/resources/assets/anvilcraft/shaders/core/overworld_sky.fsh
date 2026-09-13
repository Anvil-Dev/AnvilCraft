#version 150
#moj_import <anvilcraft:mun/mun_sun_render.glsl>

uniform sampler2D Sampler0;
uniform sampler2D Sampler2;
uniform mat4 MoonRotation;
uniform vec3 MoonCenter;
uniform vec3 SunDirection;
uniform float Visibility;
in vec3 localRay;
out vec4 fragColor;

const float MOON_HALF_SIZE = 0.045;
const float MOON_PERSPECTIVE = 0.25;

bool moon(vec3 ray, out vec3 color) {
    color = vec3(0.0);
    mat3 inverseMoon = transpose(mat3(MoonRotation));
    vec3 origin = inverseMoon * (-MoonCenter / MOON_PERSPECTIVE);
    vec3 direction = inverseMoon * (ray + MoonCenter * dot(ray, MoonCenter) * (1.0 / MOON_PERSPECTIVE - 1.0));
    vec2 interval = cubeInterval(origin, direction, MOON_HALF_SIZE);
    if (interval.y < max(interval.x, 0.0)) return false;
    vec3 point = origin + interval.x * direction;
    vec3 p = clamp(point / (2.0 * MOON_HALF_SIZE) + 0.5, 0.0, 1.0);
    vec3 face = abs(point);
    vec2 tile;
    vec2 uv;
    if (face.x >= face.y && face.x >= face.z) {
        tile = point.x > 0.0 ? vec2(0.0, 1.0) : vec2(2.0, 1.0);
        uv = vec2(point.x > 0.0 ? 1.0 - p.z : p.z, 1.0 - p.y);
    } else if (face.y >= face.z) {
        tile = point.y > 0.0 ? vec2(1.0, 0.0) : vec2(1.0, 2.0);
        uv = vec2(p.x, point.y > 0.0 ? p.z : 1.0 - p.z);
    } else {
        tile = point.z > 0.0 ? vec2(3.0, 1.0) : vec2(1.0, 1.0);
        uv = vec2(point.z > 0.0 ? p.x : 1.0 - p.x, 1.0 - p.y);
    }
    // 保留模型的立方体轮廓和六面 UV，用径向受光形成连续移动的月相明暗交界。
    vec3 normal = mat3(MoonRotation) * normalize(point);
    float light = dot(normal, SunDirection);
    float terminator = smoothstep(-0.025, 0.025, light);
    vec3 albedo = texture(Sampler0, (tile + clamp(uv, 0.001, 0.999)) / 4.0).rgb;
    color = mix(albedo, vec3(1.0), 0.22) * terminator * sqrt(max(light, 0.0));
    return true;
}

// 大气只衰减来自天体的光；前景天空由实际渲染结果提供，暗面不再写入黑色。
vec3 transmission(vec3 ray) {
    float airMass = 1.0 / max(ray.y + 0.04, 0.08);
    return exp(-vec3(0.025, 0.070, 0.160) * max(airMass - 1.0, 0.0));
}

vec3 moonGlow(vec3 ray) {
    vec3 tangent = normalize(cross(ray, vec3(0.0, 0.0, 1.0)));
    vec3 vertical = cross(tangent, ray);
    vec3 glow = vec3(0.0);
    for (int i = 0; i < 8; i++) {
        float angle = float(i) * 0.785398163;
        vec3 offset = tangent * cos(angle) + vertical * sin(angle);
        vec3 light;
        if (moon(normalize(ray + offset * 0.0035), light)) glow += light * 0.008;
        if (moon(normalize(ray + offset * 0.009), light)) glow += light * 0.002;
    }
    return glow;
}

// 主世界太阳的大气光晕随高度扩张；日面外沿从相同亮度连续衰减到天空。
vec3 solarTransmission(float altitude) {
    float airMass = 1.0 / max(altitude + 0.04, 0.08);
    float brightness = mix(0.80, 1.0, smoothstep(0.0, 0.45, altitude));
    return exp(-vec3(0.035, 0.120, 0.280) * max(airMass - 1.0, 0.0)) * brightness;
}

vec3 solarHalo(vec3 ray, float altitude, float radius) {
    vec2 projected = ray.xz / ray.y;
    float distance = 2.0;
    for (int edge = 0; edge < 6; edge++) {
        vec2 start = sunOutline(edge);
        vec2 segment = sunOutline((edge + 1) % 6) - start;
        float along = clamp(dot(projected - start, segment) / dot(segment, segment), 0.0, 1.0);
        distance = min(distance, length(projected - start - segment * along));
    }
    float height = smoothstep(0.0, 0.85, altitude);
    float profile = 0.68 * exp(-distance / mix(0.0025, 0.025, height))
        + 0.24 * exp(-distance / mix(0.012, 0.115, height))
        + 0.08 * exp(-distance / mix(0.025, 0.240, height));
    profile *= 1.0 - smoothstep(radius * 0.7, radius, distance);
    return (vec3(1.0) - exp(-vec3(1.0, 0.985, 0.94) * 4.0)) * profile;
}

void main() {
    vec3 ray = normalize(localRay);
    if (Visibility <= 0.0) discard;
    vec3 tangent = vec3(SunDirection.y, -SunDirection.x, 0.0);
    vec3 sunRay = vec3(dot(ray, tangent), dot(ray, SunDirection), ray.z);
    float solarAltitude = max(SunDirection.y, 0.0);
    float haloRadius = mix(0.09, 0.75, smoothstep(0.0, 0.85, solarAltitude));
    bool nearSun = sunRay.y > 0.0 && max(abs(sunRay.x), abs(sunRay.z)) < sunRay.y * (haloRadius + 0.085);
    bool nearMoon = dot(ray, MoonCenter) > 0.993;
    if (!nearSun && !nearMoon) discard;
    vec3 sky = texelFetch(Sampler2, ivec2(gl_FragCoord.xy), 0).rgb;
    vec3 transmitted = transmission(ray);
    vec3 light = vec3(0.0);
    bool moonSurface = false;
    if (nearMoon) {
        moonSurface = moon(ray, light);
        light = (light * 1.25 + moonGlow(ray)) * transmitted;
    }
    vec4 solar = vec4(0.0);
    if (nearSun && max(abs(sunRay.x), abs(sunRay.z)) < sunRay.y * 0.085) {
        solar = modelSun(sunRay, 0.0);
    }
    vec3 solarLight = solarTransmission(solarAltitude);
    // 月球遮挡太阳、太阳光晕和背景星点，大气底色始终位于日月前方。
    if (moonSurface) {
        vec3 reflected = vec3(1.0) - exp(-light);
        fragColor = vec4(sky + (vec3(1.0) - sky) * reflected, 1.0) * Visibility;
        return;
    }
    vec3 halo = nearSun && solar.a == 0.0 ? solarHalo(sunRay, solarAltitude, haloRadius) * solarLight : vec3(0.0);
    if (solar.a > 0.0) {
        vec3 emission = (vec3(1.0) - exp(-solar.rgb * 4.0)) * solarLight;
        fragColor = vec4(sky + (vec3(1.0) - sky) * emission, 1.0) * Visibility;
    } else {
        // 只给受光区域扩散少量辉光，避免给整个月球套一圈发亮轮廓。
        vec3 glow = (light + halo) * (vec3(1.0) - sky);
        fragColor = vec4(glow, 0.0) * Visibility;
    }
}
