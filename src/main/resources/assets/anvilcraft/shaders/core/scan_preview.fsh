#version 150

#moj_import <anvilcraft:scan_preview.glsl>

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform float GameTime;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    fragColor = anvilcraftScanPreview(DiffuseSampler, texCoord, InSize, GameTime);
}
