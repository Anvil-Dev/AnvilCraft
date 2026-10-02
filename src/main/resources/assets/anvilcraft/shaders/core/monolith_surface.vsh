#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in vec3 Position;
in vec2 SpriteOrigin;
in ivec2 SpriteSize;

out vec4 texProj0;
flat out vec4 spriteBounds;
out float sphericalVertexDistance;
out float cylindricalVertexDistance;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    texProj0 = projection_from_position(gl_Position);
    spriteBounds = vec4(SpriteOrigin, vec2(SpriteSize) / vec2(textureSize(Sampler0, 0)));
#ifdef GUI_PROJECTION
    texProj0 = TextureMat * texProj0;
    sphericalVertexDistance = 0.0;
    cylindricalVertexDistance = 0.0;
#else
    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);
#endif
}
