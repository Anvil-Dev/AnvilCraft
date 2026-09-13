#version 150

#moj_import <fog.glsl>
#moj_import <anvilcraft:scan_preview.glsl>

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
uniform float ScanTime;

in float vertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec2 size = 1.0 / max(fwidth(texCoord0), vec2(1.0 / 8192.0));
    vec4 color = anvilcraftScanPreview(Sampler0, texCoord0, size, ScanTime);
    if (color.a < 0.1) discard;
    color.rgb *= 0.85;
    color *= vertexColor * ColorModulator;
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
    color *= lightMapColor;
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
