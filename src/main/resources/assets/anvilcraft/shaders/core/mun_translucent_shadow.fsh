#version 150
uniform sampler2D Sampler0;
uniform int OpaquePass;
in vec2 texCoord;
in vec4 shadowColor;
out uvec2 fragColor;
void main() {
    ivec2 size = textureSize(Sampler0, 0);
    ivec2 pixel = clamp(ivec2(texCoord * vec2(size)), ivec2(0), size - 1);
    vec4 texel = texelFetch(Sampler0, pixel, 0) * shadowColor;
    if (texel.a <= 0.0) discard;
    if (OpaquePass != 0 ? texel.a < 1.0 : texel.a >= 1.0) discard;
    vec3 transmission = (1.0 - texel.a) * mix(vec3(1.0), texel.rgb, texel.a);
    uvec3 color = uvec3(round(clamp(transmission, 0.0, 1.0) * 255.0));
    // Pack RGB and 24-bit depth into one integer sampler without losing depth precision.
    fragColor = uvec2(color.r | (color.g << 8u) | (color.b << 16u), uint(gl_FragCoord.z * 16777215.0));
}
