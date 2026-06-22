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

// Polygon vertex data for cubic lensing (tightly packed for all holes, max 24)
uniform vec2 PolyVerts[24];
uniform float PolyStart[4];
uniform float PolyCount[4];

uniform float BlackHoleCount;
uniform float LensStrength;
uniform float EventHorizonRadius;
uniform float LensingShape; // 0 = CIRCULAR, 1 = CUBIC
uniform float PerspectiveScale; // reference distance for perspective (default 10.0)

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

// Signed distance to convex polygon: negative inside, positive outside.
float sdPolygon(vec2 p, int startIdx, int vertCount) {
    if (vertCount < 3) return 1.0;
    float d = 1e10;
    int posCount = 0;
    int negCount = 0;

    for (int j = 0; j < 6; j++) {
        if (j >= vertCount) break;
        int jNext = (j + 1) % vertCount;
        vec2 a = PolyVerts[startIdx + j];
        vec2 b = PolyVerts[startIdx + jNext];
        vec2 e = b - a;
        vec2 ap = p - a;

        // Cross product sign per edge
        float crossVal = e.x * ap.y - e.y * ap.x;
        if (crossVal > 0.0) posCount++;
        else if (crossVal < 0.0) negCount++;

        // Distance to this edge segment
        float t = clamp(dot(ap, e) / max(dot(e, e), 0.00001), 0.0, 1.0);
        vec2 closest = a + t * e;
        d = min(d, length(p - closest));
    }

    // Inside iff all cross products have same sign (convex polygon)
    float inside = (posCount == vertCount || negCount == vertCount) ? -1.0 : 1.0;
    return d * inside;
}

// Distance from origin along `dir` to polygon boundary.
// Returns a large number if no intersection is found.
float rayToPolygon(vec2 origin, vec2 dir, int startIdx, int vertCount) {
    float minT = 1e10;
    for (int j = 0; j < 6; j++) {
        if (j >= vertCount) break;
        int jNext = (j + 1) % vertCount;
        vec2 a = PolyVerts[startIdx + j];
        vec2 b = PolyVerts[startIdx + jNext];
        vec2 e = b - a;
        float det = dir.x * e.y - dir.y * e.x;
        if (abs(det) < 0.00001) continue;
        float t = ((a.x - origin.x) * e.y - (a.y - origin.y) * e.x) / det;
        float s = ((a.x - origin.x) * dir.y - (a.y - origin.y) * dir.x) / (-det);
        if (t > 0.0 && s >= 0.0 && s <= 1.0) {
            minT = min(minT, t);
        }
    }
    return minT;
}

void main() {
    vec2 uv = texCoord;
    float aspectRatio = InSize.x / InSize.y;

    vec2 offset = vec2(0.0);
    int count = int(BlackHoleCount);

    for (int i = 0; i < 4; i++) {
        if (i >= count) break;

        vec2 holeUv = getHolePos(i);
        bool isCubic = LensingShape > 0.5;
        int pStart = int(PolyStart[i]);
        int pCount = int(PolyCount[i]);

        float perspScale = PerspectiveScale / max(getHoleDist(i), 0.1);

        float effDist;
        vec2 dir;

        if (isCubic && pCount >= 3) {
            // --- Cubic mode: polygon-shaped gravitational field ---

            // Compute polygon center
            vec2 polyCenter = vec2(0.0);
            for (int j = 0; j < 6; j++) {
                if (j >= pCount) break;
                polyCenter += PolyVerts[pStart + j];
            }
            polyCenter /= float(pCount);

            // Compute average polygon radius in UV units
            float avgPolyRadius = 0.0;
            for (int j = 0; j < 6; j++) {
                if (j >= pCount) break;
                avgPolyRadius += length(PolyVerts[pStart + j] - polyCenter);
            }
            avgPolyRadius /= float(pCount);

            // Direction and distance from polygon center to current pixel
            vec2 fromCenter = uv - polyCenter;
            float distFromCenter = length(fromCenter);

            if (distFromCenter < 0.0001) continue;

            vec2 centerDir = fromCenter / distFromCenter;

            // Distance from center to polygon boundary in this direction
            float boundaryDist = rayToPolygon(polyCenter, centerDir, pStart, pCount);
            if (boundaryDist >= 1e9) {
                boundaryDist = avgPolyRadius;
            }

            // Normalized t: 0 at center, 1 at boundary, >1 outside
            float t = distFromCenter / max(boundaryDist, 0.0001);

            // Convert UV-based EventHorizonRadius to polygon-normalized threshold.
            // horizonScale = how many polygon-radius-units the UV horizon covers.
            float horizonScale = (EventHorizonRadius * perspScale) / max(avgPolyRadius, 0.00001);
            // Map t so that t=horizonScale corresponds to the event horizon boundary.
            effDist = t / max(horizonScale, 0.0001);
            // Direction toward polygon center (in raw UV)
            dir = centerDir;
        } else {
            // --- Circular mode ---
            vec2 toHole = holeUv - uv;
            toHole.x *= aspectRatio;
            float dist = length(toHole);

            if (dist < 0.0001) continue;

            effDist = dist;
            dir = toHole / dist;
        }

        // Inverse-square gravitational pull (scaled by perspective)
        float distSqr = effDist * effDist;
        float gravity = LensStrength * perspScale / distSqr;

        // Mask out within event horizon (scaled by perspective)
        float scaledHorizon = EventHorizonRadius * perspScale;
        float mask = step(scaledHorizon, effDist);

        vec2 lensOffset = dir * gravity * mask;

        if (!isCubic || pCount < 3) {
            // Circular mode: convert aspect-ratio back
            lensOffset.x /= aspectRatio;
        }
        // Cubic mode: offset is already in raw UV space, no conversion needed

        offset += lensOffset;
    }

    vec3 color = texture(DiffuseSampler, uv + offset).rgb;

    // Render event horizon for each black hole
    for (int i = 0; i < 4; i++) {
        if (i >= count) break;

        vec2 holeUv = getHolePos(i);
        bool isCubic = LensingShape > 0.5;
        int pStart = int(PolyStart[i]);
        int pCount = int(PolyCount[i]);

        float horizonMask = 0.0;

        if (isCubic && pCount >= 3) {
            // Polygon-shaped event horizon.
            vec2 polyCenter = vec2(0.0);
            for (int j = 0; j < 6; j++) {
                if (j >= pCount) break;
                polyCenter += PolyVerts[pStart + j];
            }
            polyCenter /= float(pCount);

            // Average polygon radius in UV units
            float avgPolyRadius = 0.0;
            for (int j = 0; j < 6; j++) {
                if (j >= pCount) break;
                avgPolyRadius += length(PolyVerts[pStart + j] - polyCenter);
            }
            avgPolyRadius /= float(pCount);

            // Convert UV-based horizon to polygon fraction
            float perspS = PerspectiveScale / max(getHoleDist(i), 0.1);
            float horizonUv = EventHorizonRadius * perspS;
            float r = clamp(horizonUv / max(avgPolyRadius, 0.00001), 0.0, 0.99);

            float scale = 1.0 / max(1.0 - r, 0.01);
            vec2 testPt = polyCenter + (uv - polyCenter) * scale;
            float sd = sdPolygon(testPt, pStart, pCount);
            horizonMask = step(sd, 0.0);
        } else {
            // Circular event horizon (aspect-ratio corrected, perspective scaled)
            float perspS = PerspectiveScale / max(getHoleDist(i), 0.1);
            vec2 toHole = uv - holeUv;
            toHole.x *= aspectRatio;
            float dist = length(toHole);
            horizonMask = 1.0 - step(EventHorizonRadius * perspS, dist);
        }

        color = mix(color, vec3(0.0, 0.0, 0.0), horizonMask);
    }

    fragColor = vec4(color, 1.0);
}
