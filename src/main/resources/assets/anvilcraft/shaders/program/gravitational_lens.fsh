#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;

// Up to 4 black hole screen positions in UV coordinates (0-1)
uniform float BlackHole1X;
uniform float BlackHole1Y;
uniform float BlackHole2X;
uniform float BlackHole2Y;
uniform float BlackHole3X;
uniform float BlackHole3Y;
uniform float BlackHole4X;
uniform float BlackHole4Y;

// Distance from camera to each black hole (world units)
uniform float BlackHole1Dist;
uniform float BlackHole2Dist;
uniform float BlackHole3Dist;
uniform float BlackHole4Dist;

uniform float BlackHoleCount;
uniform float LensStrength;
uniform float EventHorizonRadius;
uniform float PerspectiveScale; // reference distance (default 10.0)

in vec2 texCoord;
out vec4 fragColor;

vec2 getHolePos(int i) {
    if (i == 0) return vec2(BlackHole1X, BlackHole1Y);
    if (i == 1) return vec2(BlackHole2X, BlackHole2Y);
    if (i == 2) return vec2(BlackHole3X, BlackHole3Y);
    return vec2(BlackHole4X, BlackHole4Y);
}

float getHoleDist(int i) {
    if (i == 0) return BlackHole1Dist;
    if (i == 1) return BlackHole2Dist;
    if (i == 2) return BlackHole3Dist;
    return BlackHole4Dist;
}

void main() {
    vec2 uv = texCoord;
    float aspectRatio = InSize.x / InSize.y;

    vec2 offset = vec2(0.0);
    int count = int(BlackHoleCount);

    // --- Gravitational displacement ---
    for (int i = 0; i < 4; i++) {
        if (i >= count) break;

        vec2 holeUv = getHolePos(i);
        vec2 toHole = holeUv - uv;
        toHole.x *= aspectRatio;
        float dist = length(toHole);

        if (dist < 0.0001) continue;

        vec2 dir = toHole / dist;
        float perspScale = PerspectiveScale / max(getHoleDist(i), 0.1);

        float gravity = LensStrength * perspScale / (dist * dist);
        float scaledHorizon = EventHorizonRadius * perspScale;
        float mask = step(scaledHorizon, dist);

        vec2 lensOffset = dir * gravity * mask;
        lensOffset.x /= aspectRatio;
        offset += lensOffset;
    }

    vec3 color = texture(DiffuseSampler, uv + offset).rgb;

    // --- Render event horizon ---
    for (int i = 0; i < 4; i++) {
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
