#version 150
#moj_import <anvilcraft:mun_solar.glsl>
in vec3 Position;
in vec2 UV0;
in vec4 Color;
uniform vec3 ShadowScale;
uniform vec3 ChunkOffset;
out vec2 texCoord;
out vec2 opacity;
void main() {
    // Resolve model-local world rays before introducing the movable map window.
    vec3 projected = solarProject(Position);
    vec2 subpixel = floor(projected.xz * ShadowScale.z * 256.0 + 0.5) / (ShadowScale.z * 256.0);
    gl_Position = vec4(subpixel.x * ShadowScale.x + ChunkOffset.x,
        -subpixel.y * ShadowScale.x + ChunkOffset.y, projected.y * ShadowScale.y + ChunkOffset.z, 1.0);
    texCoord = UV0;
    opacity = Color.ra;
}
