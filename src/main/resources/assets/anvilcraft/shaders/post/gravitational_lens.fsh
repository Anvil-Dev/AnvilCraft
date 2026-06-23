#version 330

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;

// UBO layout (binding = 0):
//   vec4[0]: LensParams (count, lensStrength, eventHorizonRadius, perspectiveScale)
//   vec4[1..256]: BlackHole (screenU, screenV, cameraDistance, lensDirection)
layout (std140, binding = 0) uniform BlackHoles {
    vec4 LensParams;
    vec4 BlackHole[256];
};

in vec2 texCoord;
out vec4 fragColor;

vec2 getHolePos(int i) { return BlackHole[i].xy; }
float getHoleDist(int i) { return BlackHole[i].z; }
float getLensDir(int i) { return BlackHole[i].w; }

void main() {
    vec2 uv = texCoord;
    float aspectRatio = InSize.x / InSize.y;

    vec2 offset = vec2(0.0);
    int count = int(LensParams.x);
    float lensStrength = LensParams.y;
    float eventHorizonRadius = LensParams.z;
    float perspectiveScale = LensParams.w;

    // --- Gravitational displacement ---
    for (int i = 0; i < 256; i++) {
        if (i >= count) break;

        vec2 holeUv = getHolePos(i);
        vec2 toHole = holeUv - uv;
        toHole.x *= aspectRatio;
        float dist = length(toHole);

        if (dist < 0.0001) continue;

        vec2 dir = toHole / dist;
        float perspScale = perspectiveScale / max(getHoleDist(i), 0.1);
        float lensDir = getLensDir(i);

        float gravity = lensStrength * perspScale / (dist * dist);

        vec2 lensOffset;
        if (lensDir < 0.0) {
            // Concave: push away from center, scaled by |lensDir|
            lensOffset = -dir * gravity * (-lensDir);
        } else {
            // Convex: pull toward center
            lensOffset = dir * gravity * lensDir;
        }
        lensOffset.x /= aspectRatio;
        offset += lensOffset;
    }

    vec3 color = texture(DiffuseSampler, uv + offset).rgb;

    // --- Render event horizon (convex, on-screen black holes only) ---
    for (int i = 0; i < 256; i++) {
        if (i >= count) break;

        // Concave lenses have no event horizon
        if (getLensDir(i) <= 0.0) continue;

        vec2 holeUv = getHolePos(i);

        // Skip event horizon when the hole center is off-screen
        if (holeUv.x < 0.0 || holeUv.x > 1.0 || holeUv.y < 0.0 || holeUv.y > 1.0) continue;

        float perspS = perspectiveScale / max(getHoleDist(i), 0.1);
        vec2 toHole = uv - holeUv;
        toHole.x *= aspectRatio;
        float dist = length(toHole);
        float horizonMask = 1.0 - smoothstep(eventHorizonRadius * perspS * 0.95, eventHorizonRadius * perspS * 1.05, dist);

        color = mix(color, vec3(0.0, 0.0, 0.0), horizonMask);
    }

    fragColor = vec4(color, 1.0);
}
