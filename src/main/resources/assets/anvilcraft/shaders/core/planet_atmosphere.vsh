#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:fog.glsl>

in vec3 Position;
in vec4 Color;
out vec3 localPosition;
out float vertexDistance;
out float cylindricalVertexDistance;
out float atmosphereOpacity;
flat out vec3 CameraLocal;
flat out vec3 LightLocal;
out vec3 AtmosphereColor;

void main() {
    vec4 position = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * position;
    vertexDistance = length(position.xyz);
    cylindricalVertexDistance = fog_cylindrical_distance(position.xyz);
    atmosphereOpacity = Color.a;
    mat4 inversePose = inverse(TextureMat);
    localPosition = (inversePose * vec4(Position, 1.0)).xyz - vec3(0.5);
    CameraLocal = (inversePose * vec4(0.0, 0.0, 0.0, 1.0)).xyz - vec3(0.5);
    LightLocal = normalize(mat3(inversePose) * normalize(vec3(0.7, 0.5, 0.5)));
    AtmosphereColor = Color.rgb;
}
