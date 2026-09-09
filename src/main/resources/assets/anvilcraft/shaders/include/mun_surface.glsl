uniform sampler2D Sampler3;
uniform vec3 SunDirection;
uniform float Sunlight;

bool columnBlocked(ivec2 column, float fromY, float toY, int size, int height) {
    int first = int(clamp(floor(min(fromY, toY) + 1e-5), 0.0, float(height - 1)));
    int last = int(clamp(floor(max(fromY, toY) - 1e-5), 0.0, float(height - 1)));
    if (last < first) {
        first = int(clamp(floor((fromY + toY) * 0.5), 0.0, float(height - 1)));
        last = first;
    }
    for (int band = first >> 5; band <= (last >> 5); band++) {
        uvec4 bytes = uvec4(round(texelFetch(Sampler3, ivec2(column.x, 1 + band * size + column.y), 0) * 255.0));
        uint occupied = bytes.r | (bytes.g << 8u) | (bytes.b << 16u) | (bytes.a << 24u);
        if (band == (first >> 5)) occupied &= 0xffffffffu << uint(first & 31);
        if (band == (last >> 5)) occupied &= 0xffffffffu >> uint(31 - (last & 31));
        if (occupied != 0u) return true;
    }
    return false;
}

float terrainVisibility(vec3 worldPosition, vec3 normal, float skyAccess) {
    ivec2 atlasSize = textureSize(Sampler3, 0);
    int size = atlasSize.x;
    int height = ((atlasSize.y - 1) / size) * 32;
    float fallback = skyAccess > 0.0 ? 1.0 : 0.0;
    if (height <= 0) return fallback;
    vec3 bounds = vec3(size, height, size);
    vec3 start = worldPosition + normal * 0.0001;
    vec3 inverseRay = sign(SunDirection + vec3(1e-12)) / max(abs(SunDirection), vec3(1e-12));
    vec3 first = -start * inverseRay;
    vec3 second = (bounds - start) * inverseRay;
    vec3 near = min(first, second);
    vec3 far = max(first, second);
    float entry = max(0.0, max(max(near.x, near.y), near.z));
    float exit = min(min(far.x, far.y), far.z);
    if (exit <= entry) return fallback;

    ivec2 column = ivec2(clamp(floor((start + SunDirection * (entry + 1e-5)).xz), 0.0, float(size - 1)));
    ivec2 step = ivec2(sign(SunDirection.xz));
    vec2 boundary = vec2(column) + max(vec2(step), vec2(0.0));
    vec2 next = (boundary - start.xz) * inverseRay.xz;
    vec2 delta = abs(inverseRay.xz);
    if (step.x == 0) next.x = 1e30;
    if (step.y == 0) next.y = 1e30;
    bool incomplete = entry > 0.0;

    // 按网格边界推进，检查光线经过的完整高度区间，低角度也不会漏过细小遮挡。
    for (int i = 0; i < size * 2; i++) {
        float end = min(exit, min(next.x, next.y));
        int tile = (column.x >> 4) + (column.y >> 4) * (size >> 4);
        bool known = texelFetch(Sampler3, ivec2(tile, 0), 0).r > 0.0;
        incomplete = incomplete || !known;
        if (known && end > entry && columnBlocked(column, start.y + SunDirection.y * entry,
            start.y + SunDirection.y * end, size, height)) return 0.0;
        if (end >= exit) {
            bool reachesSky = SunDirection.y > 0.0 && far.y <= min(far.x, far.z);
            return incomplete || !reachesSky ? fallback : 1.0;
        }
        bvec2 advance = lessThanEqual(next, next.yx);
        if (advance.x) {
            column.x += step.x;
            next.x += delta.x;
        }
        if (advance.y) {
            column.y += step.y;
            next.y += delta.y;
        }
        if (any(lessThan(column, ivec2(0))) || any(greaterThanEqual(column, ivec2(size)))) return fallback;
        entry = end;
    }
    return fallback;
}

float surfaceSunlight(vec3 worldPosition, vec3 normal, float skyAccess) {
    float direct = sqrt(max(dot(normal, SunDirection), 0.0)) * Sunlight;
    if (direct > 0.001) direct *= terrainVisibility(worldPosition, normal, skyAccess);
    return direct;
}

vec3 surfaceColor(vec3 albedo, vec3 blockLight, float direct) {
    vec3 light = min(blockLight + vec3(direct), vec3(1.0));
    // 提亮中间色调，避免月壤和斜射日光在无大气天空下显得过暗。
    return pow(clamp(albedo * light, 0.0, 1.0), vec3(0.82));
}
