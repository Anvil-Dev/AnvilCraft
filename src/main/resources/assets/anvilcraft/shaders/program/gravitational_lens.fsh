#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;

// Up to 8 black hole screen positions in UV coordinates (0-1)
uniform float BlackHole1X;
uniform float BlackHole1Y;
uniform float BlackHole2X;
uniform float BlackHole2Y;
uniform float BlackHole3X;
uniform float BlackHole3Y;
uniform float BlackHole4X;
uniform float BlackHole4Y;
uniform float BlackHole5X;
uniform float BlackHole5Y;
uniform float BlackHole6X;
uniform float BlackHole6Y;
uniform float BlackHole7X;
uniform float BlackHole7Y;
uniform float BlackHole8X;
uniform float BlackHole8Y;

// Distance from camera to each black hole (world units)
uniform float BlackHole1Dist;
uniform float BlackHole2Dist;
uniform float BlackHole3Dist;
uniform float BlackHole4Dist;
uniform float BlackHole5Dist;
uniform float BlackHole6Dist;
uniform float BlackHole7Dist;
uniform float BlackHole8Dist;

uniform float BlackHoleCount;
uniform float LensStrength;
uniform float EventHorizonRadius;
uniform float PerspectiveScale;

in vec2 texCoord;
out vec4 fragColor;

vec2 getHolePos(int i) {
    if (i == 0) return vec2(BlackHole1X, BlackHole1Y);
    if (i == 1) return vec2(BlackHole2X, BlackHole2Y);
    if (i == 2) return vec2(BlackHole3X, BlackHole3Y);
    if (i == 3) return vec2(BlackHole4X, BlackHole4Y);
    if (i == 4) return vec2(BlackHole5X, BlackHole5Y);
    if (i == 5) return vec2(BlackHole6X, BlackHole6Y);
    if (i == 6) return vec2(BlackHole7X, BlackHole7Y);
    return vec2(BlackHole8X, BlackHole8Y);
}

float getHoleDist(int i) {
    if (i == 0) return BlackHole1Dist;
    if (i == 1) return BlackHole2Dist;
    if (i == 2) return BlackHole3Dist;
    if (i == 3) return BlackHole4Dist;
    if (i == 4) return BlackHole5Dist;
    if (i == 5) return BlackHole6Dist;
    if (i == 6) return BlackHole7Dist;
    return BlackHole8Dist;
}

void main() {
    vec2 uv = texCoord;
    float aspectRatio = InSize.x / InSize.y;

    vec2 offset = vec2(0.0);
    int count = int(BlackHoleCount);

    // --- Gravitational displacement ---
    for (int i = 0; i < 8; i++) {
        if (i >= count) break;

        vec2 holeUv = getHolePos(i);
        vec2 toHole = holeUv - uv;
        toHole.x *= aspectRatio;
        float dist = length(toHole);

        if (dist < 0.0001) continue;

        vec2 dir = toHole / dist;
        float perspScale = PerspectiveScale / max(getHoleDist(i), 0.1);

        float gravity = LensStrength * perspScale / (dist * dist);

        vec2 lensOffset = dir * gravity;
        lensOffset.x /= aspectRatio;
        offset += lensOffset;
    }

    vec3 color = texture(DiffuseSampler, uv + offset).rgb;

    // --- Render event horizon ---
    for (int i = 0; i < 8; i++) {
        if (i >= count) break;

        vec2 holeUv = getHolePos(i);
        float perspS = PerspectiveScale / max(getHoleDist(i), 0.1);
        vec2 toHole = uv - holeUv;
        toHole.x *= aspectRatio;
        float dist = length(toHole);
        float horizonMask = 1.0 - step(EventHorizonRadius * perspS, dist);

        color = mix(color, vec3(0.0, 0.0, 0.0), horizonMask);
    }

    fragColor = vec4(color, 1.0);
}
