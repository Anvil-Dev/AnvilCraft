package dev.dubhe.anvilcraft.client.building;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.init.ModRenderPipelines;
import dev.dubhe.anvilcraft.integration.iris.IrisState;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.util.LinkedHashMap;
import java.util.Map;

final class BuildingRodRenderTypes {
    static final RenderType BLOCK_GHOST = blockLayer(ModRenderPipelines.BUILDING_ROD_GHOST);
    static final RenderType PLACED = blockLayer(ModRenderPipelines.BUILDING_ROD_PLACED);
    private static final Map<RenderType, RenderType> TYPES = new LinkedHashMap<>();
    private static final Map<RenderPipeline, RenderPipeline> PIPELINES = new LinkedHashMap<>();

    static void clearLayers() {
        TYPES.clear();
    }

    private BuildingRodRenderTypes() {
    }

    private static RenderType blockLayer(RenderPipeline pipeline) {
        return RenderType.create(pipeline.getLocation().toString(), RenderSetup.builder(pipeline).useLightmap().sortOnUpload()
            .bufferSize(786432).withTexture("Sampler0", Sheets.BLOCKS_MAPPER.sheet()).createRenderSetup());
    }

    static RenderType ghost(RenderType original) {
        var texture = original.state.textures.get("Sampler0");
        if (IrisState.isBlockFormat(original.format()) && texture != null && texture.location().equals(Sheets.BLOCKS_MAPPER.sheet())) {
            return BLOCK_GHOST;
        }
        if (original.hasBlending()) {
            return original;
        }
        return TYPES.computeIfAbsent(original, type -> {
            var previous = type.state;
            boolean entity = type.format() == RenderPipelines.ENTITY_TRANSLUCENT_CULL.getVertexFormat()
                && previous.textures.containsKey("Sampler0");
            var source = entity ? RenderPipelines.ENTITY_TRANSLUCENT_CULL : type.pipeline();
            var pipeline = PIPELINES.computeIfAbsent(source, originalPipeline -> {
                var blended = originalPipeline.toBuilder().withLocation(AnvilCraft.of("pipeline/blueprint_entity_" + PIPELINES.size()))
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT)).build();
                IrisState.copyPipeline(originalPipeline, blended);
                return blended;
            });
            var builder = RenderSetup.builder(pipeline).sortOnUpload().bufferSize(type.bufferSize())
                .setLayeringTransform(previous.layeringTransform).setOutputTarget(previous.outputTarget)
                .setTextureTransform(previous.textureTransform);
            previous.textures.forEach((name, binding) -> {
                if (pipeline.getSamplers().contains(name)) {
                    builder.withTexture(name, binding.location(), binding.sampler());
                }
            });
            var textures = previous.getTextures();
            if (entity || textures.containsKey("Sampler1")) {
                builder.useOverlay();
            }
            if (entity || textures.containsKey("Sampler2")) {
                builder.useLightmap();
            }
            return RenderType.create("anvilcraft:blueprint_entity", builder.createRenderSetup());
        });
    }
}
