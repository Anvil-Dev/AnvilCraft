#ifndef ANVILCRAFT_MUN_SOLAR
#define ANVILCRAFT_MUN_SOLAR
uniform vec3 SolarReference;
uniform vec3 SolarOrigin;
uniform int SolarEclipseCount;
uniform float SolarEclipseCoverage;
uniform vec2 SolarHorizon0;
uniform vec2 SolarHorizon1;
uniform vec2 SolarHorizon2;
uniform vec2 SolarHorizon3;
uniform vec2 SolarHorizon4;
uniform vec2 SolarHorizon5;
uniform vec2 SolarHorizon6;
uniform vec2 SolarHorizon7;
uniform vec2 SolarEclipse0;
uniform vec2 SolarEclipse1;
uniform vec2 SolarEclipse2;
uniform vec2 SolarEclipse3;
uniform vec2 SolarEclipse4;
uniform vec2 SolarEclipse5;
uniform vec2 SolarEclipse6;
uniform vec2 SolarEclipse7;
uniform vec2 SolarEclipse8;
uniform vec2 SolarEclipse9;
uniform vec2 SolarEclipse10;
uniform vec2 SolarEclipse11;

// This field depends on world position and time, never on the viewer's solar direction.
void solarFrame(vec3 position, vec3 reference, vec3 origin, vec2 horizon[8], out vec3 sun, out vec3 latitude) {
    vec2 world = position.xz / 2048.0 + origin.xz;
    float radius = length(world);
    vec2 radial = radius > 1e-9 ? world / radius : vec2(1.0, 0.0);
    float edge = 0.0;
    for (int index = 0; index < 8; index++) edge = max(edge, dot(radial, horizon[index]));
    float tangent = max(abs(world.x), abs(world.y)) * (sqrt(1.0 + edge * edge) + edge);
    float cosine = (1.0 - tangent * tangent) / (1.0 + tangent * tangent);
    float sine = 2.0 * tangent / (1.0 + tangent * tangent);
    float along = dot(radial, reference.xz);
    float horizontal = along * (cosine - 1.0) - reference.y * sine;
    sun = vec3(reference.x + radial.x * horizontal, reference.y * cosine + along * sine,
        reference.z + radial.y * horizontal);
    latitude = vec3(radial.x * sine, cosine, radial.y * sine);
}

void solarFrame(vec3 position, out vec3 sun, out vec3 latitude) {
    vec2 horizon[8] = vec2[8](SolarHorizon0, SolarHorizon1, SolarHorizon2, SolarHorizon3, SolarHorizon4, SolarHorizon5, SolarHorizon6, SolarHorizon7);
    solarFrame(position, SolarReference, SolarOrigin, horizon, sun, latitude);
}

vec3 solarDirection(vec3 position) {
    vec3 sun;
    vec3 latitude;
    solarFrame(position, sun, latitude);
    return sun;
}

// Canonical shadow rays are vertical. All casters and receivers use the same world field.
vec3 solarProject(vec3 position, vec3 sun) {
    float height = (position.y + SolarOrigin.y) / max(sun.y, 0.025);
    return position - vec3(sun.x * height, 0.0, sun.z * height);
}

vec3 solarProject(vec3 position) {
    return solarProject(position, solarDirection(position));
}

void solarAreaVertex(vec2 point, inout bool started, inout vec2 first, inout vec2 last, inout float area) {
    if (!started) { first = point; started = true; }
    else area += last.x * point.y - last.y * point.x;
    last = point;
}

float solarClippedArea(vec3 plane, bool eclipse) {
    const float radius = 0.0675;
    vec2 square[4] = vec2[4](vec2(-radius), vec2(radius, -radius), vec2(radius), vec2(-radius, radius));
    vec2 polygon[12] = vec2[12](SolarEclipse0, SolarEclipse1, SolarEclipse2, SolarEclipse3, SolarEclipse4, SolarEclipse5, SolarEclipse6, SolarEclipse7, SolarEclipse8, SolarEclipse9, SolarEclipse10, SolarEclipse11);
    int count = eclipse ? SolarEclipseCount : 4;
    vec2 previous = eclipse ? polygon[count - 1] : square[3];
    float previousDistance = dot(plane, vec3(previous, 1.0));
    bool started = false;
    vec2 first = vec2(0.0);
    vec2 last = vec2(0.0);
    float area = 0.0;
    for (int index = 0; index < 12; index++) {
        if (index >= count) break;
        vec2 current = eclipse ? polygon[index] : square[index];
        float distance = dot(plane, vec3(current, 1.0));
        if ((previousDistance >= 0.0) != (distance >= 0.0)) {
            vec2 intersection = mix(previous, current, previousDistance / (previousDistance - distance));
            solarAreaVertex(intersection, started, first, last, area);
        }
        if (distance >= 0.0) solarAreaVertex(current, started, first, last, area);
        previous = current;
        previousDistance = distance;
    }
    return abs(area + last.x * first.y - last.y * first.x) * 0.5;
}

float solarDaylight(vec3 sun, vec3 latitude) {
    vec2 slope = vec2(dot(latitude, vec3(SolarReference.y, -SolarReference.x, 0.0)), latitude.z);
    float extent = 0.0675 * (abs(slope.x) + abs(slope.y));
    if (sun.y <= -extent) return 0.0;
    if (sun.y >= extent) return 1.0 - SolarEclipseCoverage;
    vec3 plane = vec3(slope, sun.y);
    float area = solarClippedArea(plane, false);
    if (SolarEclipseCount >= 3) area -= solarClippedArea(plane, true);
    return clamp(area / (4.0 * 0.0675 * 0.0675), 0.0, 1.0);
}
#endif
