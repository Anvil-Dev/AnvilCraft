#version 330 core

uniform sampler2D DiffuseSampler;

layout (std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout (std140) uniform BlackHoles {
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

    vec3 color = texture(DiffuseSampler, uv).rgb;

    int count = int(LensParams.x);
    float lensStrength = LensParams.y;
    float eventHorizonRadius = LensParams.z;
    float perspectiveScale = LensParams.w;

    // --- Gravitational displacement ---
    vec2 offset = vec2(0.0);
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
            lensOffset = -dir * gravity * (-lensDir);
        } else {
            lensOffset = dir * gravity * lensDir;
        }
        lensOffset.x /= aspectRatio;
        offset += lensOffset;
    }

    color = texture(DiffuseSampler, uv + offset).rgb;

    // --- Render event horizon (convex, on-screen black holes only) ---
    for (int i = 0; i < 256; i++) {
        if (i >= count) break;
        if (getLensDir(i) <= 0.0) continue;

        vec2 holeUv = getHolePos(i);
        if (holeUv.x < 0.0 || holeUv.x > 1.0 || holeUv.y < 0.0 || holeUv.y > 1.0) continue;

        float perspS = perspectiveScale / max(getHoleDist(i), 0.1);
        vec2 toHole = uv - holeUv;
        toHole.x *= aspectRatio;
        float dist = length(toHole);
        float innerR = eventHorizonRadius * perspS * 0.95;
        float outerR = eventHorizonRadius * perspS * 1.05;
        // Clamp to prevent smoothstep undefined behavior
        if (innerR >= outerR) continue;
        float horizonMask = 1.0 - smoothstep(innerR, outerR, dist);
        color = mix(color, vec3(0.0, 0.0, 0.0), clamp(horizonMask, 0.0, 1.0));
    }

    fragColor = vec4(color, 1.0);
}
