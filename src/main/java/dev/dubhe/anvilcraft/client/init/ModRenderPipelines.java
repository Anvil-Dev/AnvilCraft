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

    /// 加法混合（发光叠加），用于超新星爆发光束/闪光。
    public static final BlendFunction ADDITIVE_BLEND = new BlendFunction(
        SourceFactor.SRC_ALPHA,
        DestFactor.ONE,
        SourceFactor.ONE,
        DestFactor.ONE
    );

    /// 乘法混合（DST_COLOR × SRC_COLOR），用于恒星颜色叠加，精确调色板着色。
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
        .withColorTargetState(new ColorTargetState(LASER_BLEND))
        .withShaderDefine("ALPHA_CUTOUT", 0.01F)
        .withDepthStencilState(DepthStencilState.DEFAULT)
        .withLocation(AnvilCraft.of("pipeline/translucent_laser"))
        .build();

    public static final RenderPipeline LIGHTNING = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
        .withColorTargetState(new ColorTargetState(LASER_BLEND))
        .withShaderDefine("ALPHA_CUTOUT", 0.01F)
        .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
        .withVertexShader(Identifier.withDefaultNamespace("core/position_tex_color"))
        .withFragmentShader(AnvilCraft.of("core/rendertype_lightning"))
        .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
        .withCull(false)
        .withLocation(AnvilCraft.of("pipeline/lightning"))
        .build();

    /// 超新星爆发光束/闪光：加法混合发光，双面可见，不写深度。复用 lightning 着色器（顶点色）。
    public static final RenderPipeline SUPERNOVA_BEAM = RenderPipeline.builder(RenderPipelines.BLOCK_SNIPPET)
        .withColorTargetState(new ColorTargetState(ADDITIVE_BLEND))
        .withShaderDefine("ALPHA_CUTOUT", 0.01F)
        .withFragmentShader(AnvilCraft.of("core/rendertype_lightning"))
        .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
        .withCull(false)
        .withLocation(AnvilCraft.of("pipeline/supernova_beam"))
        .build();

    /// 腐化信标暗色光束：普通半透明混合（非加法），双面可见，写深度以呈现"暗色遮挡"感。
    /// 复用 lightning 片段着色器（顶点色），与超新星光束区别在于用 TRANSLUCENT 而非 ADDITIVE 混合。
    public static final RenderPipeline CORRUPTED_BEACON_BEAM = RenderPipeline.builder(RenderPipelines.BLOCK_SNIPPET)
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withShaderDefine("ALPHA_CUTOUT", 0.01F)
        .withFragmentShader(AnvilCraft.of("core/rendertype_lightning"))
        .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
        .withCull(false)
        .withLocation(AnvilCraft.of("pipeline/corrupted_beacon_beam"))
        .build();

    /// 恒星颜色叠加：乘法混合（DST_COLOR × SRC_COLOR），使灰度恒星贴图精确按恒星 RGB 着色。
    public static final RenderPipeline STAR_COLOR_OVERLAY = RenderPipeline.builder(RenderPipelines.BLOCK_SNIPPET)
        .withColorTargetState(new ColorTargetState(MULTIPLY_BLEND))
        .withShaderDefine("ALPHA_CUTOUT", 0.01F)
        .withDepthStencilState(DepthStencilState.DEFAULT)
        .withLocation(AnvilCraft.of("pipeline/star_color_overlay"))
        .build();

    /// 天体大气层/光晕立方体：普通半透明混合，双面可见（NO_CULL）。
    public static final RenderPipeline CELESTIAL_ATMOSPHERE = RenderPipeline.builder(RenderPipelines.BLOCK_SNIPPET)
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withShaderDefine("ALPHA_CUTOUT", 0.01F)
        .withDepthStencilState(DepthStencilState.DEFAULT)
        .withCull(false)
        .withLocation(AnvilCraft.of("pipeline/celestial_atmosphere"))
        .build();

    /// 超新星闪光 billboard：加法混合发光贴图，双面可见，不写深度。
    public static final RenderPipeline SUPERNOVA_FLASH = RenderPipeline.builder(RenderPipelines.BLOCK_SNIPPET)
        .withColorTargetState(new ColorTargetState(ADDITIVE_BLEND))
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
        event.registerPipeline(LASER_TRANSLUCENT);
        event.registerPipeline(LIGHTNING);
        event.registerPipeline(SUPERNOVA_BEAM);
        event.registerPipeline(CORRUPTED_BEACON_BEAM);
        event.registerPipeline(STAR_COLOR_OVERLAY);
        event.registerPipeline(CELESTIAL_ATMOSPHERE);
        event.registerPipeline(SUPERNOVA_FLASH);
        event.registerPipeline(GRAVITATIONAL_LENS);
    }
}
