float anvilcraftScanHash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

vec4 anvilcraftScanPreview(sampler2D source, vec2 uv, vec2 size, float time) {
    vec4 color = texture(source, uv);
    float lineY = floor((uv.y * size.y) / 4.0);
    float lineHash = anvilcraftScanHash(vec2(lineY, floor(time * 3.0)));
    float scanBrightness = 0.7 + 0.3 * lineHash;
    float jitter = (lineHash - 0.5) * 4.0 / size.x;
    float shouldJitter = step(0.7, anvilcraftScanHash(vec2(lineY, 42.0)));
    vec4 jitteredColor = texture(source, uv + vec2(jitter * shouldJitter, 0.0));
    vec3 blueTint = vec3(0.2, 0.45, 1.0);
    float luminance = dot(jitteredColor.rgb, vec3(0.299, 0.587, 0.114));
    vec3 finalColor = mix(jitteredColor.rgb, blueTint * luminance, 0.75) * scanBrightness;
    return vec4(finalColor, 0.85 * color.a);
}
