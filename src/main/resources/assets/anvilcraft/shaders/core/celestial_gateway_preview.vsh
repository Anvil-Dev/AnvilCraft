#version 330

#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

in vec3 Position;

out vec4 texProj0;
out float sphericalVertexDistance;
out float cylindricalVertexDistance;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    texProj0 = TextureMat * projection_from_position(gl_Position);
    sphericalVertexDistance = 0.0;
    cylindricalVertexDistance = 0.0;
}
