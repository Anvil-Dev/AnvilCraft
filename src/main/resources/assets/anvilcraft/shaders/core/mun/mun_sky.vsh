#version 150

in vec3 Position;
uniform mat4 InverseProjection;
uniform mat4 InverseView;
out vec3 localRay;

void main() {
    gl_Position = vec4(Position.xy, 1.0, 1.0);
    vec4 viewRay = InverseProjection * vec4(Position.xy, 1.0, 1.0);
    localRay = mat3(InverseView) * viewRay.xyz;
}
