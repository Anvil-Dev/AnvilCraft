#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;

out float vertexDistance;
out float cylindricalVertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;

void main() {
    vec4 position = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * position;
    vertexDistance = length(position.xyz);
    cylindricalVertexDistance = fog_cylindrical_distance(position.xyz);
    vertexColor = Color;
    texCoord0 = UV0;
}
