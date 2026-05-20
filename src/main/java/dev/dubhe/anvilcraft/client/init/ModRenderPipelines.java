package dev.dubhe.anvilcraft.client.init;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

@EventBusSubscriber
public class ModRenderPipelines {

    public static final BlendFunction LASER_BLEND = new BlendFunction(
        SourceFactor.SRC_COLOR,
        DestFactor.ONE_MINUS_SRC_ALPHA,
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

    public static final RenderPipeline LIGHTNING = RenderPipeline.builder(RenderPipelines.BLOCK_SNIPPET)
        .withColorTargetState(new ColorTargetState(LASER_BLEND))
        .withShaderDefine("ALPHA_CUTOUT", 0.01F)
        .withFragmentShader(AnvilCraft.of("core/rendertype_lightning"))
        .withDepthStencilState(DepthStencilState.DEFAULT)
        .withLocation(AnvilCraft.of("pipeline/lightning"))
        .build();

    @SubscribeEvent
    public static void on(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(LASER_TRANSLUCENT);
    }
}
