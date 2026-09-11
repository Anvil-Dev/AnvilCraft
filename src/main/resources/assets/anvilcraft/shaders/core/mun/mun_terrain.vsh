#version 150

#moj_import <light.glsl>
#moj_import <fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;
in vec3 Normal;
uniform sampler2D Sampler2;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec3 ChunkOffset;
uniform vec3 CameraPosition;
uniform int FogShape;
out float vertexDistance;
out vec4 vertexColor;
out vec3 blockLight;
out float skyAccess;
out vec3 worldPosition;
out vec3 surfaceNormal;
out vec2 texCoord0;

void main() {
    vec3 pos = Position + ChunkOffset;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
    vertexDistance = fog_distance(pos, FogShape);
    vertexColor = Color;
    blockLight = minecraft_sample_lightmap(Sampler2, ivec2(UV2.x, 0)).rgb;
    skyAccess = float(UV2.y) / 240.0;
    worldPosition = pos + CameraPosition;
    surfaceNormal = Normal;
    texCoord0 = UV0;
}
