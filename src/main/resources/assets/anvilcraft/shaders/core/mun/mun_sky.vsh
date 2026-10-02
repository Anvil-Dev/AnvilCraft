#version 150
#moj_import <anvilcraft:mun/mun_sky_uniforms.glsl>

in vec3 Position;
out vec3 localRay;

void main() {
    gl_Position = vec4(Position.xy, 1.0, 1.0);
    vec4 viewRay = InverseProjection * vec4(Position.xy, 1.0, 1.0);
    localRay = mat3(InverseView) * viewRay.xyz;
}
