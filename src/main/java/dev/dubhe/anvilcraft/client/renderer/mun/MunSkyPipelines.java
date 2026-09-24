package dev.dubhe.anvilcraft.client.renderer.mun;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.init.ModRenderPipelines;
import net.minecraft.client.renderer.RenderPipelines;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

import java.util.Optional;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class MunSkyPipelines {
    public static final RenderPipeline STANDARD = RenderPipeline.builder()
        .withLocation(AnvilCraft.of("pipeline/mun_sky"))
        .withVertexShader(AnvilCraft.of("core/mun/mun_sky"))
        .withFragmentShader(AnvilCraft.of("core/mun/mun_sky"))
        .withUniform("MunSky", UniformType.UNIFORM_BUFFER)
        .withSampler("Sampler0").withSampler("Sampler1")
        .withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS)
        .withDepthStencilState(Optional.empty()).withCull(false).build();
    public static final RenderPipeline BACKGROUND = color("background", VertexFormat.Mode.QUADS, ColorTargetState.DEFAULT);
    public static final RenderPipeline COLOR = color("color", VertexFormat.Mode.TRIANGLES, ColorTargetState.DEFAULT);
    public static final RenderPipeline ADDITIVE = color("additive", VertexFormat.Mode.TRIANGLES,
        new ColorTargetState(ModRenderPipelines.ADDITIVE_BLEND));
    public static final RenderPipeline TRANSLUCENT = color("translucent", VertexFormat.Mode.TRIANGLES,
        new ColorTargetState(BlendFunction.TRANSLUCENT));
    public static final RenderPipeline TEXTURED = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
        .withLocation(AnvilCraft.of("pipeline/mun_sky_textured"))
        .withVertexShader("core/position_tex_color").withFragmentShader("core/position_tex_color")
        .withSampler("Sampler0").withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.TRIANGLES)
        .withDepthStencilState(Optional.empty()).withCull(false).build();

    private MunSkyPipelines() {
    }

    private static RenderPipeline color(String name, VertexFormat.Mode mode, ColorTargetState target) {
        return RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withLocation(AnvilCraft.of("pipeline/mun_sky_" + name))
            .withVertexShader("core/position_color").withFragmentShader("core/position_color")
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, mode).withColorTargetState(target)
            .withDepthStencilState(Optional.empty()).withCull(false).build();
    }

    @SubscribeEvent
    public static void register(RegisterRenderPipelinesEvent event) {
        for (var pipeline : new RenderPipeline[]{STANDARD, BACKGROUND, COLOR, ADDITIVE, TRANSLUCENT, TEXTURED}) {
            event.registerPipeline(pipeline);
        }
    }
}
