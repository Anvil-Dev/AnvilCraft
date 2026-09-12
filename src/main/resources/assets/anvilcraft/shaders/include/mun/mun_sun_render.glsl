#moj_import <anvilcraft:mun/mun_sun.glsl>

uniform sampler2D Sampler1;
uniform vec4 SunUvBounds;

vec2 cubeInterval(vec3 origin, vec3 direction, float halfSize) {
    vec3 inverseRay = sign(direction + vec3(1e-12)) / max(abs(direction), vec3(1e-9));
    vec3 first = (-vec3(halfSize) - origin) * inverseRay;
    vec3 second = (vec3(halfSize) - origin) * inverseRay;
    vec3 near = min(first, second);
    vec3 far = max(first, second);
    return vec2(max(max(near.x, near.y), near.z), min(min(far.x, far.y), far.z));
}

vec4 modelSun(vec3 ray, float daylight) {
    mat3 inverseSun = transpose(sunRotation());
    vec3 origin = inverseSun * vec3(0.0, -1.0 / SUN_PERSPECTIVE, 0.0);
    vec3 direction = inverseSun * vec3(ray.x, ray.y / SUN_PERSPECTIVE, ray.z);
    vec2 interval = cubeInterval(origin, direction, SUN_BODY_HALF_SIZE);
    if (interval.y >= max(interval.x, 0.0)) {
        vec3 point = origin + interval.x * direction;
        vec3 p = clamp(point / (2.0 * SUN_BODY_HALF_SIZE) + 0.5, 0.0, 1.0);
        vec3 face = abs(point);
        vec2 uv;
        float brightness;
        if (face.x >= face.y && face.x >= face.z) {
            uv = vec2(point.x > 0.0 ? 1.0 - p.z : p.z, 1.0 - p.y);
            brightness = 0.97;
        } else if (face.y >= face.z) {
            uv = vec2(point.y > 0.0 ? 1.0 - p.x : p.x, 1.0 - p.z);
            brightness = 1.0;
        } else {
            uv = vec2(point.z > 0.0 ? p.x : 1.0 - p.x, 1.0 - p.y);
            brightness = 0.94;
        }
        vec2 atlasUv = mix(SunUvBounds.xy, SunUvBounds.zw, clamp(uv, 0.001, 0.999));
        vec3 surface = mix(texture(Sampler1, atlasUv).rgb, vec3(1.0), 0.9);
        return vec4(surface * vec3(1.0, 0.985, 0.94) * brightness, 1.0);
    }
    // 按到日面轮廓的距离连续衰减，避免离散壳层形成一圈圈暗色条带。
    vec2 projected = ray.xz / ray.y;
    float distance = 1.0;
    for (int edge = 0; edge < 6; edge++) {
        vec2 start = sunOutline(edge);
        vec2 segment = sunOutline((edge + 1) % 6) - start;
        float along = clamp(dot(projected - start, segment) / dot(segment, segment), 0.0, 1.0);
        distance = min(distance, length(projected - start - segment * along));
    }
    float glow = 0.8 * exp(-distance / 0.009) + 0.35 * exp(-distance / 0.035) + 0.1 * exp(-distance / 0.09);
    glow *= 1.0 - smoothstep(0.22, 0.30, distance);
    return vec4(vec3(1.0, 0.92, 0.74) * daylight * glow, 0.0);
}

