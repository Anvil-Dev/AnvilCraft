#ifndef ANVILCRAFT_MUN_SUN
#define ANVILCRAFT_MUN_SUN
// 与 MunSkyMath 的太阳立方体共用尺寸、倾角与弱透视，供天空和月面光照使用。
const float SUN_BODY_HALF_SIZE = 0.05;
const float SUN_PERSPECTIVE = 0.25;
const float SUN_DISC_HALF_SIZE = 0.071006049;
const float SUN_DISC_AREA = 0.016304838;

mat3 sunRotation() {
    float c = cos(radians(35.0));
    float s = sin(radians(35.0)) * sqrt(0.5);
    float a = (1.0 + c) * 0.5;
    float b = (1.0 - c) * 0.5;
    return mat3(a, s, b, -s, c, s, b, -s, a);
}

vec3 sunCorner(int corner, float scale) {
    vec3 offset = vec3((corner & 1) == 0 ? -1.0 : 1.0,
        (corner & 2) == 0 ? -1.0 : 1.0, (corner & 4) == 0 ? -1.0 : 1.0);
    offset = sunRotation() * offset * SUN_BODY_HALF_SIZE * scale;
    return vec3(offset.x, 1.0 + offset.y * SUN_PERSPECTIVE, offset.z);
}

vec2 sunOutline(int index) {
    int corners[6] = int[6](2, 0, 1, 5, 7, 6);
    vec3 point = sunCorner(corners[index], 1.0);
    return point.xz / point.y;
}
#endif
