#version 150

#moj_import <projection.glsl>
#moj_import <fog.glsl>

in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

uniform int FogShape;

out vec4 texProj0;
out float vertexDistance;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    vertexDistance = ProjMat[3][3] == 0.0 ? fog_distance(Position, FogShape) : 0.0;
    texProj0 = projection_from_position(gl_Position);
}
