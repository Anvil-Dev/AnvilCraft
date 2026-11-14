#version 150

#moj_import <light.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;

uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat4 SkyRotation;

out vec4 vertexColor;
out vec2 texCoord0;
out vec3 skyPosition;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    // Sky geometry belongs at the far plane, independently of the terrain view distance.
    gl_Position.z = gl_Position.w;
    vertexColor = Color * minecraft_sample_lightmap(Sampler2, UV2);
    texCoord0 = UV0;
    // 顶点已包含相机旋转，雾气仰角需要恢复为世界坐标。
    skyPosition = (SkyRotation * vec4(Position, 0.0)).xyz;
}
