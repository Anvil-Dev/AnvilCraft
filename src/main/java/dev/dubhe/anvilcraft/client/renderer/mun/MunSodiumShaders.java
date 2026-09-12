package dev.dubhe.anvilcraft.client.renderer.mun;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/** 为 Sodium 和 Embeddium 的原始地形着色器增加可按维度切换的月面光照。 */
public final class MunSodiumShaders {
    private MunSodiumShaders() {
    }

    public static String patch(String source, ResourceLocation name) {
        if (!MunRenderPipeline.requested()) return source;
        if (!name.getPath().startsWith("blocks/block_layer_opaque.")) return source;
        return patch(source, name.getNamespace(), name.getPath(), name.getPath().endsWith(".fsh") ? surfaceSource() : "");
    }

    public static String patch(String source, String namespace, String path, String surface) {
        if (!path.startsWith("blocks/block_layer_opaque.")) return source;
        boolean vertex = path.endsWith(".vsh");
        boolean embeddium = namespace.equals("embeddium");
        String declarations = vertex ? """
            uniform vec3 CameraPosition;
            out vec3 mun_Position;
            out vec3 mun_Tint;
            out vec3 mun_BlockLight;
            out float mun_SkyAccess;
            """ : """
            in vec3 mun_Position;
            in vec3 mun_Tint;
            in vec3 mun_BlockLight;
            in float mun_SkyAccess;
            vec4 mun_Texel;

            vec4 anvilcraft_sampleBlockTexture(sampler2D blockTexture, vec2 uv, float bias) {
                mun_Texel = texture(blockTexture, uv, bias);
                return mun_Texel;
            }
            """ + surface;
        int versionEnd = source.indexOf('\n') + 1;
        String body = source.substring(versionEnd).replace("void main()", "void anvilcraft_originalMain()");
        if (!vertex) {
            // 复用原始材质采样，兼容 Sodium 0.6 的插值 LOD 和 0.8 的材质位字段。
            body = body.replace("texture(u_BlockTex,", "anvilcraft_sampleBlockTexture(u_BlockTex,");
        }
        String extensions = vertex ? "" : """
            #ifdef GL_ARB_shader_image_load_store
            #extension GL_ARB_shader_image_load_store : enable
            #define MUN_SHADOW_HISTORY
            #endif
            """;
        String patched = source.substring(0, versionEnd) + extensions + "uniform int MunEnabled;\n" + declarations + body;
        if (vertex) {
            String lightCoord = embeddium ? "vec2(_vert_tex_light_coord) / 256.0" : "_vert_tex_light_coord";
            String ambientOcclusion = embeddium ? """
                #ifndef USE_VANILLA_COLOR_FORMAT
                    mun_Tint *= _vert_color.a;
                #endif
                """ : "";
            return patched + """
                void main() {
                    anvilcraft_originalMain();
                    if (MunEnabled == 0) return;
                    mun_Position = _vert_position + u_RegionOffset + _get_draw_translation(_draw_id) + CameraPosition;
                    mun_Tint = _vert_color.rgb;
                    %s
                    vec2 lightCoord = %s;
                    mun_BlockLight = texture(u_LightTex, vec2(clamp(lightCoord.x, 0.03125, 0.96875), 0.03125)).rgb;
                    mun_SkyAccess = lightCoord.y;
                }
                """.formatted(ambientOcclusion, lightCoord);
        }
        // 在原始 cutout 片元可能 discard 之前计算导数，避免透明边缘出现无效法线。
        return patched + """
            void main() {
                vec3 normal = vec3(0.0, 1.0, 0.0);
                if (MunEnabled != 0) {
                    normal = normalize(cross(dFdx(mun_Position), dFdy(mun_Position)));
                    if (!gl_FrontFacing) normal = -normal;
                }
                anvilcraft_originalMain();
                if (MunEnabled == 0) return;
                vec4 light = surfaceLight(mun_Position, normal, mun_SkyAccess);
                float alpha = mun_Texel.a;
                %s
                vec4 color = vec4(surfaceColor(mun_Texel.rgb * mun_Tint, mun_BlockLight, light.rgb, normal, mun_SkyAccess, light.a), alpha);
                fragColor = _linearFog(color, v_FragDistance, u_FogColor, u_FogStart, u_FogEnd);
            }
            """.formatted(embeddium ? """
                #ifdef USE_VANILLA_COLOR_FORMAT
                    alpha *= v_Color.a;
                #endif
                """ : "alpha *= v_Color.a;");
    }

    private static String surfaceSource() {
        try (var stream = Minecraft.getInstance().getResourceManager()
            .open(AnvilCraft.of("shaders/include/mun/mun_surface.glsl"))) {
            String surface = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            try (var solar = Minecraft.getInstance().getResourceManager().open(AnvilCraft.of("shaders/include/mun/mun_solar.glsl"));
                 var sun = Minecraft.getInstance().getResourceManager().open(AnvilCraft.of("shaders/include/mun/mun_sun.glsl"))) {
                return surface.replace("#moj_import <anvilcraft:mun/mun_solar.glsl>",
                    new String(solar.readAllBytes(), StandardCharsets.UTF_8).replace("#moj_import <anvilcraft:mun/mun_sun.glsl>",
                        new String(sun.readAllBytes(), StandardCharsets.UTF_8)));
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot load Mun surface shader", exception);
        }
    }
}
