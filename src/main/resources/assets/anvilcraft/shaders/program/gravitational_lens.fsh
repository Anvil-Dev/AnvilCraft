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

uniform float BlackHoleCount;
uniform float LensStrength;
uniform float EventHorizonRadius;

in vec2 texCoord;
out vec4 fragColor;

vec2 getHolePos(int i) {
    if (i == 0) return vec2(BlackHole1X, BlackHole1Y);
    if (i == 1) return vec2(BlackHole2X, BlackHole2Y);
    if (i == 2) return vec2(BlackHole3X, BlackHole3Y);
    return vec2(BlackHole4X, BlackHole4Y);
}

void main() {
    vec2 uv = texCoord;
    vec2 offset = vec2(0.0);
    int count = int(BlackHoleCount);

    // Combine gravitational displacement from all active black holes
    for (int i = 0; i < 4; i++) {
        if (i >= count) break;

        vec2 holeUv = getHolePos(i);
        vec2 toHole = holeUv - uv;
        float dist = length(toHole);

        if (dist < 0.0001) continue;

        vec2 dir = toHole / dist;

        // Inverse-square falloff (mirroring the reference gravitational lensing)
        float distSqr = dist * dist;
        float gravity = LensStrength / distSqr;

        // Mask out within event horizon to avoid singularity
        float mask = step(EventHorizonRadius, dist);

        offset += dir * gravity * mask;
    }

    vec3 color = texture(DiffuseSampler, uv + offset).rgb;

    // Render event horizon (black disk) for each black hole
    for (int i = 0; i < 4; i++) {
        if (i >= count) break;
        float dist = length(getHolePos(i) - uv);
        float horizonMask = 1.0 - step(EventHorizonRadius, dist);
        color = mix(color, vec3(0.0, 0.0, 0.0), horizonMask);
    }

    fragColor = vec4(color, 1.0);
}
