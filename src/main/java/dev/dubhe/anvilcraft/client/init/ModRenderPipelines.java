package dev.dubhe.anvilcraft.client.init;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.anvilcraft.lib.v2.rendering.ALRPipelines;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = AnvilCraft.MOD_ID)
public class ModRenderPipelines {

    public static final BlendFunction LASER_BLEND = new BlendFunction(
        SourceFactor.SRC_COLOR,
        DestFactor.ONE_MINUS_SRC_ALPHA,
        SourceFactor.ZERO,
        DestFactor.ONE
    );

    public static final BlendFunction ADDITIVE_BLEND = new BlendFunction(
        SourceFactor.SRC_ALPHA,
        DestFactor.ONE,
        SourceFactor.ONE,
        DestFactor.ONE
    );

    public static final BlendFunction MULTIPLY_BLEND = new BlendFunction(
        SourceFactor.DST_COLOR,
        DestFactor.ZERO,
        SourceFactor.ZERO,
        DestFactor.ONE
    );

    public static final RenderPipeline COLORED_OVERLAY = RenderPipeline.builder(RenderPipelines.BLOCK_SNIPPET)
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withShaderDefine("ALPHA_CUTOUT", 0.01F)
        .withFragmentShader(AnvilCraft.of("core/rendertype_translucent_colored_overlay"))
        .withShaderDefine("OVERLAY_COLOR")
        .withShaderDefine("OVERLAY_COLOR_A", 0.866f)
        .withShaderDefine("OVERLAY_COLOR_R", 0.4f)
        .withShaderDefine("OVERLAY_COLOR_G", 0.8f)
        .withShaderDefine("OVERLAY_COLOR_B", 1f)
        .withDepthStencilState(DepthStencilState.DEFAULT)
        .withLocation(AnvilCraft.of("pipeline/colored_overlay_block"))
        .build();

    public static final RenderPipeline LASER_TRANSLUCENT = RenderPipeline.builder(RenderPipelines.BLOCK_SNIPPET)
        .withColorTargetState(new ColorTargetState(ModRenderPipelines.LASER_BLEND))
        .withShaderDefine("ALPHA_CUTOUT", 0.01F)
        .withDepthStencilState(DepthStencilState.DEFAULT)
        .withLocation(AnvilCraft.of("pipeline/translucent_laser"))
        .build();

    public static final RenderPipeline LIGHTNING = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
        .withColorTargetState(new ColorTargetState(ModRenderPipelines.LASER_BLEND))
        .withShaderDefine("ALPHA_CUTOUT", 0.01F)
        .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
        .withVertexShader(Identifier.withDefaultNamespace("core/position_tex_color"))
        .withFragmentShader(AnvilCraft.of("core/rendertype_lightning"))
        .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
        .withCull(false)
        .withLocation(AnvilCraft.of("pipeline/lightning"))
        .build();

    public static final RenderPipeline SUPERNOVA_BEAM = RenderPipeline.builder(RenderPipelines.BEACON_BEAM_SNIPPET)
        .withColorTargetState(new ColorTargetState(ModRenderPipelines.ADDITIVE_BLEND))
        .withShaderDefine("ALPHA_CUTOUT", 0.01F)
        .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
        .withCull(false)
        .withLocation(AnvilCraft.of("pipeline/supernova_beam"))
        .build();

    /**
     * 托举光束和超新星放射光束使用的纯顶点色叠加管线。
     * 不采样纹理且不写入深度，与 1.21 的 POSITION_COLOR 渲染类型保持一致。
     */
    public static final RenderPipeline STELLAR_BEAM = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
        .withColorTargetState(new ColorTargetState(ModRenderPipelines.ADDITIVE_BLEND))
        .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
        .withCull(false)
        .withLocation(AnvilCraft.of("pipeline/stellar_beam"))
        .build();
    
    /**
     * 腐化信标使用纯顶点色半透明管线，使深色顶点通过常规透明混合压暗背景。
     * 不采样激光图集，避免贴图透明度冲淡黑紫色核心和外围暗光。
     */
    public static final RenderPipeline CORRUPTED_BEACON_BEAM = RenderPipeline.builder(
        RenderPipelines.DEBUG_FILLED_SNIPPET
    )
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES)
        .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
        .withCull(false)
        .withLocation(AnvilCraft.of("pipeline/corrupted_beacon_beam"))
        .build();

    public static final RenderPipeline STAR_COLOR_OVERLAY = RenderPipeline.builder(RenderPipelines.BLOCK_SNIPPET)
        .withColorTargetState(new ColorTargetState(ModRenderPipelines.MULTIPLY_BLEND))
        .withShaderDefine("ALPHA_CUTOUT", 0.01F)
        .withDepthStencilState(DepthStencilState.DEFAULT)
        .withLocation(AnvilCraft.of("pipeline/star_color_overlay"))
        .build();

    public static final RenderPipeline CELESTIAL_ATMOSPHERE = RenderPipeline.builder(RenderPipelines.BLOCK_SNIPPET)
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withShaderDefine("ALPHA_CUTOUT", 0.01F)
        .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
        .withCull(false)
        .withLocation(AnvilCraft.of("pipeline/celestial_atmosphere"))
        .build();

    /**
     * 天体环使用方块半透明着色器，只写颜色并保留深度测试。
     * 这样行星主体能够遮挡环的背面部分，同时不会让环本身写入深度。
     */
    public static final RenderPipeline CELESTIAL_RING = RenderPipeline.builder(RenderPipelines.BLOCK_SNIPPET)
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withShaderDefine("ALPHA_CUTOUT", 0.01F)
        .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
        .withCull(false)
        .withLocation(AnvilCraft.of("pipeline/celestial_ring"))
        .build();

    public static final RenderPipeline SUPERNOVA_FLASH = RenderPipeline.builder(RenderPipelines.BLOCK_SNIPPET)
        .withColorTargetState(new ColorTargetState(ModRenderPipelines.ADDITIVE_BLEND))
        .withShaderDefine("ALPHA_CUTOUT", 0.01F)
        .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
        .withCull(false)
        .withLocation(AnvilCraft.of("pipeline/supernova_flash"))
        .build();

    public static final RenderPipeline GRAVITATIONAL_LENS = RenderPipeline.builder(ALRPipelines.POST_PASS)
        .withLocation(AnvilCraft.of("pipeline/gravitational_lens"))
        .withFragmentShader(AnvilCraft.of("core/gravitational_lens"))
        .withUniform("SamplerInfo", UniformType.UNIFORM_BUFFER)
        .withUniform("BlackHoles", UniformType.UNIFORM_BUFFER)
        .build();

    @SubscribeEvent
    public static void on(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(ModRenderPipelines.LASER_TRANSLUCENT);
        event.registerPipeline(ModRenderPipelines.LIGHTNING);
        event.registerPipeline(ModRenderPipelines.SUPERNOVA_BEAM);
        event.registerPipeline(ModRenderPipelines.STELLAR_BEAM);
        event.registerPipeline(ModRenderPipelines.CORRUPTED_BEACON_BEAM);
        event.registerPipeline(ModRenderPipelines.STAR_COLOR_OVERLAY);
        event.registerPipeline(ModRenderPipelines.CELESTIAL_ATMOSPHERE);
        event.registerPipeline(ModRenderPipelines.CELESTIAL_RING);
        event.registerPipeline(ModRenderPipelines.SUPERNOVA_FLASH);
        event.registerPipeline(ModRenderPipelines.GRAVITATIONAL_LENS);
    }
}
