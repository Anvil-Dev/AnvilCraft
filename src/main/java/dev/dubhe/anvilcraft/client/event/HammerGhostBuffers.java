package dev.dubhe.anvilcraft.client.event;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import dev.dubhe.anvilcraft.integration.iris.IrisState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.ARGB;
import net.neoforged.neoforge.client.model.pipeline.VertexConsumerWrapper;
import org.jspecify.annotations.NullMarked;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@NullMarked
final class HammerGhostBuffers extends MultiBufferSource.BufferSource {
    private static final Map<RenderType, RenderType> SPECIAL_LAYERS = new HashMap<>();
    private static final Map<RenderPipeline, RenderPipeline> PIPELINES = new HashMap<>();
    private final Map<RenderType, RenderType> layers = new HashMap<>();
    private final RenderType blockLayer;

    HammerGhostBuffers(RenderType blockLayer) {
        super(new ByteBufferBuilder(32768), new LinkedHashMap<>());
        this.blockLayer = blockLayer;
    }

    static void clearLayers() {
        SPECIAL_LAYERS.clear();
    }

    @Override
    public VertexConsumer getBuffer(RenderType type) {
        RenderType ghost = this.layers.computeIfAbsent(type, this::ghostLayer);
        boolean shaderTint = ghost == ModRenderTypes.TRANSLUCENT_COLORED_OVERLAY;
        return new VertexConsumerWrapper(super.getBuffer(ghost)) {
            @Override
            public VertexConsumer setColor(int color) {
                return this.setColor(ARGB.red(color), ARGB.green(color), ARGB.blue(color), ARGB.alpha(color));
            }

            @Override
            public VertexConsumer setColor(int red, int green, int blue, int alpha) {
                this.parent.setColor(shaderTint ? red : red * 3 / 5, shaderTint ? green : green * 9 / 10, blue, alpha * 3 / 5);
                return this;
            }
        };
    }

    RenderType ghostLayer(RenderType type) {
        var texture = type.state.textures.get("Sampler0");
        if (texture != null) {
            if (IrisState.isBlockFormat(type.format()) && texture.location().equals(Sheets.BLOCKS_MAPPER.sheet())) {
                return this.blockLayer;
            }
            if (type.format() == RenderPipelines.ENTITY_TRANSLUCENT_CULL.getVertexFormat()) {
                return RenderTypes.entityTranslucentCullItemTarget(texture.location());
            }
        }
        if (type.hasBlending()) {
            return type;
        }
        return SPECIAL_LAYERS.computeIfAbsent(type, HammerGhostBuffers::blendLayer);
    }

    private static RenderType blendLayer(RenderType type) {
        var pipeline = PIPELINES.computeIfAbsent(type.pipeline(), source -> {
            var blended = source.toBuilder()
                .withLocation(AnvilCraft.of("pipeline/hammer_ghost/" + PIPELINES.size()))
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .build();
            IrisState.copyPipeline(source, blended);
            return blended;
        });
        var setup = RenderSetup.builder(pipeline).setLayeringTransform(type.state.layeringTransform)
            .setTextureTransform(type.state.textureTransform).setOutputTarget(type.outputTarget());
        type.state.textures.forEach((name, binding) -> setup.withTexture(name, binding.location(), binding.sampler()));
        if (pipeline.getSamplers().contains("Sampler1")) {
            setup.useOverlay();
        }
        if (pipeline.getSamplers().contains("Sampler2")) {
            setup.useLightmap();
        }
        return RenderType.create("anvilcraft:hammer_ghost", setup.createRenderSetup());
    }
}
