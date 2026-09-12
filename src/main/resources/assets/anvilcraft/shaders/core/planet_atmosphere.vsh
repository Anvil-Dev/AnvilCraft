#version 150

in vec3 Position;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat4 PlanetPose;
out vec3 localPosition;
out float vertexDistance;

void main() {
    vec4 position = ModelViewMat * PlanetPose * vec4(Position + vec3(0.5), 1.0);
    gl_Position = ProjMat * position;
    vertexDistance = length(position.xyz);
    localPosition = Position;
}
