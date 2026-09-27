#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:sample_lightmap.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;

uniform sampler2D Sampler2;

out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;

void main() {
    vec3 pos = Position + ModelOffset;
    vec3 viewPos = (ModelViewMat * vec4(pos, 1.0)).xyz;
    gl_Position = ProjMat * vec4(viewPos, 1.0);

    // Cached beam vertices are chunk-local; fog needs camera-relative world coordinates.
    vec3 relativePos = transpose(mat3(ModelViewMat)) * viewPos;

    sphericalVertexDistance = fog_spherical_distance(relativePos);
    cylindricalVertexDistance = fog_cylindrical_distance(relativePos);
    vertexColor = Color * sample_lightmap(Sampler2, UV2);
    texCoord0 = UV0;
}
