#version 330
#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:sample_lightmap.glsl>
#moj_import <anvilcraft:mun/mun_surface_uniforms.glsl>
#ifdef MUN_TERRAIN
#moj_import <minecraft:globals.glsl>
#moj_import <minecraft:chunksection.glsl>
#else
#moj_import <minecraft:dynamictransforms.glsl>
#endif
in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;
#ifdef MUN_ENTITY
in ivec2 UV1;
in vec3 Normal;
#ifndef NO_OVERLAY
uniform sampler2D Sampler1;
#endif
#endif
uniform sampler2D Sampler2;
out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec4 vertexColor;
out vec4 overlayColor;
out vec3 blockLight;
out float skyAccess;
out vec3 worldPosition;
out vec2 texCoord0;
void main() {
#ifdef MUN_TERRAIN
    vec3 pos = Position + (ChunkPosition - CameraBlockPos) + CameraOffset;
#elif defined(MUN_ENTITY)
    vec3 pos = Position;
#else
    vec3 pos = Position + ModelOffset;
#endif
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
    sphericalVertexDistance = fog_spherical_distance(pos);
    cylindricalVertexDistance = fog_cylindrical_distance(pos);
    vertexColor = Color;
    blockLight = texture(Sampler2, clamp(vec2(UV2.x, 0) / 256.0, vec2(0.5 / 16.0), vec2(15.5 / 16.0))).rgb;
    skyAccess = float(UV2.y) / 240.0;
    worldPosition = pos + CameraPosition;
    texCoord0 = UV0;
#ifdef APPLY_TEXTURE_MATRIX
    texCoord0 = (TextureMat * vec4(UV0, 0.0, 1.0)).xy;
#endif
#if defined(MUN_ENTITY) && !defined(NO_OVERLAY)
    overlayColor = texelFetch(Sampler1, UV1, 0);
#else
    overlayColor = vec4(0.0, 0.0, 0.0, 1.0);
#endif
}
