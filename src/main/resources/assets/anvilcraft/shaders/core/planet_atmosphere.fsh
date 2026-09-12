#version 150

#moj_import <fog.glsl>

uniform vec3 CameraLocal;
uniform vec3 LightLocal;
uniform vec3 AtmosphereColor;
uniform float Thickness;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 ColorModulator;
in vec3 localPosition;
in float vertexDistance;
out vec4 fragColor;

vec2 cubeInterval(vec3 origin, vec3 direction, float halfSize) {
    vec3 inverseRay = sign(direction + vec3(1e-12)) / max(abs(direction), vec3(1e-9));
    vec3 first = (-vec3(halfSize) - origin) * inverseRay;
    vec3 second = (vec3(halfSize) - origin) * inverseRay;
    vec3 near = min(first, second);
    vec3 far = max(first, second);
    return vec2(max(max(near.x, near.y), near.z), min(min(far.x, far.y), far.z));
}

void main() {
    bool inside = max(abs(CameraLocal.x), max(abs(CameraLocal.y), abs(CameraLocal.z))) < 0.5 + Thickness;
    if (gl_FrontFacing == inside) discard;
    vec3 ray = normalize(localPosition - CameraLocal);
    vec2 shell = cubeInterval(CameraLocal, ray, 0.5 + Thickness);
    vec2 body = cubeInterval(CameraLocal, ray, 0.5);
    float entry = max(shell.x, 0.0);
    float exit = shell.y;
    if (body.y >= max(body.x, 0.0)) exit = min(exit, max(body.x, 0.0));
    if (exit <= entry || Thickness <= 0.0) discard;
    float stepLength = (exit - entry) / 12.0;
    float scaleHeight = Thickness * 0.36;
    float column = 0.0;
    float scattering = 0.0;
    // Same density, outer fade and twelve-step scattering integration as the Moon sky's Earth atmosphere.
    for (int i = 0; i < 12; i++) {
        vec3 point = CameraLocal + ray * (entry + (float(i) + 0.5) * stepLength);
        float altitude = max(abs(point.x), max(abs(point.y), abs(point.z))) - 0.5;
        float density = exp(-max(altitude, 0.0) / scaleHeight)
            * (1.0 - smoothstep(Thickness * 0.65, Thickness, altitude));
        float light = 0.38 + 0.62 * max(dot(normalize(point), LightLocal), 0.0);
        column += density * stepLength;
        scattering += density * stepLength * light;
    }
    float opacity = 1.0 - exp(-column * 0.12 / scaleHeight);
    vec3 haze = AtmosphereColor * scattering / max(column, 1e-8);
    fragColor = vec4(haze, opacity * linear_fog_fade(vertexDistance, FogStart, FogEnd)) * ColorModulator;
}
