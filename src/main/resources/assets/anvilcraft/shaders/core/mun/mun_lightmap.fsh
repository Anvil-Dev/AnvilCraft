#version 330

layout(std140) uniform LightmapInfo {
    float SkyFactor;
    float BlockFactor;
    float NightVisionFactor;
    float DarknessScale;
    float BossOverlayWorldDarkeningFactor;
    float BrightnessFactor;
    vec3 BlockLightTint;
    vec3 SkyLightColor;
    vec3 AmbientColor;
    vec3 NightVisionColor;
} lightmapInfo;

in vec2 texCoord;

out vec4 fragColor;

float get_brightness(float level) {
    return level / (4.0 - 3.0 * level);
}

vec3 notGamma(vec3 color) {
    vec3 inverted = 1.0 - color;
    return 1.0 - inverted * inverted * inverted * inverted;
}
void main() {
    float blockLevel = floor(texCoord.x * 16) / 15;
    float block = get_brightness(blockLevel) * lightmapInfo.BlockFactor;
    vec3 color = vec3(block, block * ((block * 0.6 + 0.4) * 0.6 + 0.4), block * (block * block * 0.6 + 0.4));
    color += vec3(0.26, 0.28, 0.30);
    if (floor(texCoord.y * 16) == 15.0) color += vec3(lightmapInfo.SkyFactor);
    float maximum = max(color.r, max(color.g, color.b));
    if (lightmapInfo.NightVisionFactor > 0.0 && maximum < 1.0) {
        color = mix(color, color / maximum, lightmapInfo.NightVisionFactor);
    }
    color = clamp(color - lightmapInfo.DarknessScale, 0.0, 1.0);
    color = mix(color, notGamma(color), lightmapInfo.BrightnessFactor);
    color = clamp(mix(color, vec3(0.75), 0.04), 0.0, 1.0);
    fragColor = vec4(floor(color * 255.0) / 255.0, 1.0);
}
